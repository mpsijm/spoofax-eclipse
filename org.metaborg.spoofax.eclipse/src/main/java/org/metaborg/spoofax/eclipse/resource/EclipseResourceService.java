package org.metaborg.spoofax.eclipse.resource;

import java.io.File;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.provider.local.LocalFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.core.resource.ResourceChangeKind;
import org.metaborg.core.resource.ResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class EclipseResourceService extends ResourceService implements IEclipseResourceService {
    private static final ILogger logger = LoggerUtils.logger(EclipseResourceService.class);


    @Inject public EclipseResourceService(FileSystemManager fileSystemManager,
        @Named("ResourceClassLoader") ClassLoader classLoader) {
        super(fileSystemManager, classLoader);
    }


    @Override public FileObject resolve(IResource resource) {
        return resolve("eclipse://" + resource.getFullPath().toString());
    }

    @Override public @Nullable FileObject resolve(IEditorInput input) {
        if(input instanceof IFileEditorInput) {
            final IFileEditorInput fileInput = (IFileEditorInput) input;
            return resolve(fileInput.getFile());
        }
        logger.error("Could not resolve editor input {}", input);
        return null;
    }

    @Override public @Nullable ResourceChange resolve(IResourceDelta delta) {
        final FileObject resource = resolve(delta.getResource());
        final int eclipseKind = delta.getKind();
        final ResourceChangeKind kind;
        // GTODO: handle move/copies better
        switch(eclipseKind) {
            case IResourceDelta.NO_CHANGE:
                return null;
            case IResourceDelta.ADDED:
                kind = ResourceChangeKind.Create;
                break;
            case IResourceDelta.REMOVED:
                kind = ResourceChangeKind.Delete;
                break;
            case IResourceDelta.CHANGED:
                kind = ResourceChangeKind.Modify;
                break;
            default:
                final String message = String.format("Unhandled resource delta type: %s", eclipseKind);
                logger.error(message);
                throw new MetaborgRuntimeException(message);
        }

        return new ResourceChange(resource, kind);
    }

    @Override public @Nullable IResource unresolve(FileObject resource) {
        if(resource instanceof EclipseResourceFileObject) {
            final EclipseResourceFileObject eclipseResource = (EclipseResourceFileObject) resource;
            try {
                return eclipseResource.resource();
            } catch(Exception e) {
                logger.error("Could not unresolve resource {} to an Eclipse resource", e, resource);
                return null;
            }
        }

        if(resource instanceof LocalFile) {
            // LEGACY: analysis returns messages with relative local file resources, try to convert as relative first.
            final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            final String path = resource.getName().getPath();
            IResource eclipseResource = root.findMember(path);
            if(eclipseResource == null) {
                // Path might be absolute, try to get absolute file.
                final IPath location = Path.fromOSString(path);
                eclipseResource = root.getFileForLocation(location);
            }
            return eclipseResource;
        }

        return null;
    }

    @Override public @Nullable File localPath(FileObject resource) {
        if(!(resource instanceof EclipseResourceFileObject)) {
            return super.localPath(resource);
        }

        try {
            final IResource eclipseResource = unresolve(resource);
            IPath path = eclipseResource.getRawLocation();
            if(path == null) {
                path = eclipseResource.getLocation();
            }
            if(path == null) {
                return null;
            }
            return path.makeAbsolute().toFile();
        } catch(Exception e) {
            return null;
        }
    }
}
