package org.metaborg.spoofax.eclipse.meta.build;

import java.io.IOException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.project.IProjectService;
import org.metaborg.meta.core.project.ILanguageSpec;
import org.metaborg.meta.core.project.ILanguageSpecService;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigService;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecPathsService;
import org.metaborg.util.file.FileAccess;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class GenerateSourcesBuilder extends Builder {
    public static final String id = SpoofaxMetaPlugin.id + ".builder.generatesources";

    private static final ILogger logger = LoggerUtils.logger(GenerateSourcesBuilder.class);

    private final SpoofaxMetaBuilder builder;


    public GenerateSourcesBuilder() {
        super(SpoofaxMetaPlugin.injector().getInstance(IEclipseResourceService.class),
            SpoofaxMetaPlugin.injector().getInstance(IProjectService.class),
            SpoofaxMetaPlugin.injector().getInstance(ILanguageSpecService.class),
            SpoofaxMetaPlugin.injector().getInstance(ISpoofaxLanguageSpecConfigService.class),
            SpoofaxMetaPlugin.injector().getInstance(ISpoofaxLanguageSpecPathsService.class));
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.builder = injector.getInstance(SpoofaxMetaBuilder.class);
    }


    @Override protected void build(final ILanguageSpec languageSpec, final IProgressMonitor monitor)
        throws CoreException, IOException {
        final LanguageSpecBuildInput input = createBuildInput(languageSpec);

        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    logger.info("Generating sources for language project {}", languageSpec);
                    builder.initialize(input);
                    builder.generateSources(input, new FileAccess());
                } catch(Exception e) {
                    workspaceMonitor.setCanceled(true);
                    monitor.setCanceled(true);
                    logger.error("Cannot generate sources for language project {}; build failed unexpectedly", e,
                        languageSpec);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);
    }

    @Override protected void clean(final ILanguageSpec languageSpec, final IProgressMonitor monitor)
        throws CoreException, IOException {
        final LanguageSpecBuildInput input = createBuildInput(languageSpec);

        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    logger.info("Cleaning and generating sources for language project {}", languageSpec);
                    builder.clean(input);
                    builder.initialize(input);
                    builder.generateSources(input, new FileAccess());
                } catch(Exception e) {
                    workspaceMonitor.setCanceled(true);
                    monitor.setCanceled(true);
                    logger.error("Cannot clean language project {}; build failed unexpectedly", e, languageSpec);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);
    }

    @Override protected String description() {
        return "generate sources for";
    }
}
