package pl.skidam.automodpack_loader_core;

import pl.skidam.automodpack_loader_core.client.Changelogs;
import pl.skidam.automodpack_core.loader.LoaderManagerService;
import pl.skidam.automodpack_loader_core.compat.crashassistant.ProcessSignalIO;
import pl.skidam.automodpack_loader_core.screen.ScreenManager;
import pl.skidam.automodpack_loader_core.utils.UpdateType;

import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

import static pl.skidam.automodpack_core.GlobalVariables.*;

public class ReLauncher {

    private final String updateMessage;
    private final Path modpackDir;
    private final UpdateType updateType;
    private final Changelogs changelogs;

    public ReLauncher(UpdateType updateType) {
        this.modpackDir = null;
        this.updateType = updateType;
        this.changelogs = null;
        this.updateMessage = Lang.get("relaunch.self");
    }

    public ReLauncher(Path modpackDir, UpdateType updateType, Changelogs changelogs) {
        this.modpackDir = modpackDir;
        this.updateType = updateType;
        this.changelogs = changelogs;
        this.updateMessage = Lang.get("relaunch.modpack");
    }

    /**
     * Suiteland fork: minimal Spanish/English strings for the preload Swing window and its forked
     * helper, which run before Minecraft (and its language files) load. In-game screens keep using
     * the vanilla lang system, which already ships full es_* translations and picks by client locale.
     */
    static final class Lang {
        // getLanguage() returns the bare ISO-639 code with no region or variant, so es_ES, es_MX,
        // es_AR, es_419, es-ES-u-va-... all collapse to "es" - every Spanish dialect matches.
        private static final boolean SPANISH = "es".equalsIgnoreCase(java.util.Locale.getDefault().getLanguage());

        static String get(String key) {
            if (SPANISH) {
                switch (key) {
                    case "relaunch.self":    return "¡SuitelandUpdater actualizado correctamente!";
                    case "relaunch.modpack": return "¡El modpack se actualizó correctamente!";
                    case "gui.restart":      return "¡Reinicia el juego!";
                    case "gui.windowTitle":  return "Ventana de SuitelandUpdater";
                    default:                 return key;
                }
            }
            switch (key) {
                case "relaunch.self":    return "Successfully updated SuitelandUpdater!";
                case "relaunch.modpack": return "Successfully updated the modpack!";
                case "gui.restart":      return "Restart your game!";
                case "gui.windowTitle":  return "SuitelandUpdater Window";
                default:                 return key;
            }
        }
    }

    public final void restart(boolean shutdownInPreload, Runnable... callbacks) {
        if (preload && !shutdownInPreload) {
            runCallbacks(callbacks);
            return;
        }

        boolean isClient = LOADER_MANAGER.getEnvironmentType() == LoaderManagerService.EnvironmentType.CLIENT;
        boolean isHeadless = GraphicsEnvironment.isHeadless();

        if (isClient) {
            handleClientRestart(callbacks, isHeadless);
        } else {
            handleServerRestart(callbacks);
        }
    }

    private void handleClientRestart(Runnable[] callbacks, boolean isHeadless) {
        if (updateType != null && new ScreenManager().getScreenString().isPresent()) {
            new ScreenManager().restart(modpackDir, updateType, changelogs);
        } else if (preload) {
            ProcessSignalIO.post("normal_stop"); // let crash assistant know

            Semaphore semaphore = null;
            final Thread shutdownHook = new Thread(() -> runCallbacks(callbacks));
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            if (isHeadless) {
                LOGGER.info("Please restart the game to apply updates!");
            } else {
                semaphore = new Gui().open(updateMessage);
            }

            wait(semaphore); // wait for gui to launch
            runCallbacks(callbacks);
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            wait(semaphore); // wait for user interaction to close the gui
            System.exit(0);
        }

        runCallbacks(callbacks);
    }

    private void wait(Semaphore semaphore) {
        if (semaphore != null) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Failed to acquire semaphore", e);
            }
        }
    }

    private void handleServerRestart(Runnable[] callbacks) {
        LOGGER.info("Please restart the server to apply updates!");
        runCallbacks(callbacks);
        System.exit(0);
    }

    private void runCallbacks(Runnable[] callbacks) {
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}