package pl.skidam.automodpack_loader_core;

import pl.skidam.automodpack_loader_core.client.Changelogs;
import pl.skidam.automodpack_core.loader.LoaderManagerService;
import pl.skidam.automodpack_loader_core.compat.crashassistant.ProcessSignalIO;
import pl.skidam.automodpack_loader_core.utils.UpdateType;

import java.awt.*;
import java.nio.file.Path;

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
        this.updateMessage = "Successfully updated AutoModpack!";
    }

    public ReLauncher(Path modpackDir, UpdateType updateType, Changelogs changelogs) {
        this.modpackDir = modpackDir;
        this.updateType = updateType;
        this.changelogs = changelogs;
        this.updateMessage = "Successfully updated the modpack!";
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
        // Suiteland fork: a needed restart just exits - no restart screen in-game, no Swing prompt at preload.
        LOGGER.info("Update applied; exiting for relaunch without asking (private server policy)");
        ProcessSignalIO.post("normal_stop"); // let crash assistant know
        runCallbacks(callbacks);
        System.exit(0);
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