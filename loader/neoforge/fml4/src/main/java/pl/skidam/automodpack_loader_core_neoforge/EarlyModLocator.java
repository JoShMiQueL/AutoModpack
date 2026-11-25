package pl.skidam.automodpack_loader_core_neoforge;

import cpw.mods.jarhandling.JarContents;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.*;
import pl.skidam.automodpack_loader_core.Preload;
import pl.skidam.automodpack_loader_core_neoforge.mods.ModpackLoader;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("unused")
public class EarlyModLocator implements IModFileCandidateLocator {

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {

        ProgressMeter progress = StartupNotificationManager.prependProgressBar("[SuitelandUpdater] Preload", 0);
        new Preload();
        progress.complete();

        // Suiteland fork: NeoForge marks the file hosting an IModFileCandidateLocator SPI as
        // "already located" before the mods folder locator runs, so a real mod jar carrying this
        // bootstrap would never be scanned as a mod. The CodeSource is a union:/path/to.jar%NNN!/
        // URL (SecureJar FS); strip it to a file path and addPath() it as our own candidate.
        try {
            String loc = EarlyModLocator.class.getProtectionDomain()
                    .getCodeSource().getLocation().toExternalForm();
            Path selfJar = resolveUnionPath(loc);
            if (selfJar != null && java.nio.file.Files.isRegularFile(selfJar)) {
                pipeline.addPath(selfJar,
                        ModFileDiscoveryAttributes.DEFAULT.withLocator(this),
                        IncompatibleFileReporting.WARN_ALWAYS);
            }
        } catch (Throwable t) {
            pl.skidam.automodpack_core.GlobalVariables.LOGGER.warn(
                    "[SuitelandUpdater] self-container injection failed", t);
        }

        for (Path path : ModpackLoader.modsToLoad) {
            pipeline.addPath(path, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS);
            pipeline.readModFile(JarContents.of(path), ModFileDiscoveryAttributes.DEFAULT);
        }
    }

    @Override
    public int getPriority() {
        return IModFileCandidateLocator.HIGHEST_SYSTEM_PRIORITY;
    }

    /** union:/C:/dir/file.jar%23118!/  ->  C:\dir\file.jar ; file:/... handled too. */
    private static Path resolveUnionPath(String external) {
        try {
            String s = external;
            if (s.startsWith("union:")) {
                s = s.substring("union:".length());
                int bang = s.indexOf('!');
                if (bang >= 0) s = s.substring(0, bang);
                int pct = s.indexOf('%');
                if (pct >= 0) s = s.substring(0, pct);
                // s == /C:/mc-test/mods/x.jar or /home/...
                return Path.of(java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8)
                        .replaceFirst("^/(?=[A-Za-z]:/)", ""));
            }
            if (s.startsWith("file:")) {
                return Path.of(java.net.URI.create(s));
            }
            return Path.of(s);
        } catch (Exception e) {
            return null;
        }
    }
}
