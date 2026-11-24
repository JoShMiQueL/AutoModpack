package pl.skidam.automodpack_loader_core;

import pl.skidam.automodpack_loader_core.client.Changelogs;
import pl.skidam.automodpack_core.loader.LoaderManagerService;
import pl.skidam.automodpack_loader_core.compat.crashassistant.ProcessSignalIO;
import pl.skidam.automodpack_loader_core.screen.ScreenManager;
import pl.skidam.automodpack_loader_core.utils.UpdateType;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    /**
     * Suiteland fork: a needed restart relaunches the game with the same command line instead of
     * asking. When the launcher's own command line cannot be reproduced safely (Modrinth/CurseForge
     * wrappers, TLauncher, etc.), it falls back to the upstream behavior: restart screen in-game,
     * Swing prompt at preload.
     */
    private void handleClientRestart(Runnable[] callbacks, boolean isHeadless) {
        Optional<List<String>> relaunch = relaunchCommand();
        if (relaunch.isPresent()) {
            LOGGER.info("Update applied; relaunching the game with the same command line (private server policy)");
            ProcessSignalIO.post("normal_stop"); // let crash assistant know
            try {
                spawnDetached(relaunch.get(), ProcessHandle.current().pid());
                runCallbacks(callbacks);
                System.exit(0);
            } catch (IOException e) {
                LOGGER.error("Auto-relaunch failed; falling back to the restart prompt", e);
            }
        } else {
            LOGGER.info("Cannot reconstruct the launch command line; falling back to the restart prompt");
        }

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

            waitFor(semaphore); // wait for gui to launch
            runCallbacks(callbacks);
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            waitFor(semaphore); // wait for user interaction to close the gui
            System.exit(0);
        }

        runCallbacks(callbacks);
    }

    /**
     * The exact command line this JVM was started with, when it can be reproduced safely: a single
     * java binary plus its arguments. Wrapper processes (Modrinth, CurseForge, TLauncher) report a
     * non-java executable or nothing - those return empty so the caller falls back to the prompt.
     */
    private static Optional<List<String>> relaunchCommand() {
        try {
            ProcessHandle self = ProcessHandle.current();
            ProcessHandle.Info info = self.info();
            Optional<String> command = info.command();
            Optional<String[]> arguments = info.arguments();
            if (command.isEmpty() || arguments.isEmpty()) return Optional.empty();

            String executable = command.get();
            String binaryName = new File(executable).getName().toLowerCase();
            if (!binaryName.startsWith("java")) return Optional.empty();

            List<String> commandLine = new ArrayList<>(1 + arguments.get().length);
            commandLine.add(executable);
            commandLine.addAll(List.of(arguments.get()));
            return Optional.of(commandLine);
        } catch (Throwable t) {
            LOGGER.debug("ProcessHandle command line unavailable", t);
            return Optional.empty();
        }
    }

    /**
     * Spawns the new JVM fully detached so it outlives this process: own working directory, inherited
     * console on Linux/macOS, and CREATE_NEW_PROCESS_GROUP + DETACHED_PROCESS on Windows so closing
     * this JVM does not take the child down.
     */
    /**
     * Spawns a wrapper that first waits for this JVM to die (the new game cannot bind the
     * session.lock while the old one lives) and then starts the game again, fully detached so it
     * outlives both this process and the wrapper itself.
     */
    private static void spawnDetached(List<String> commandLine, long parentPid) throws IOException {
        File dir = new File(System.getProperty("user.dir"));
        ProcessBuilder builder;
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            // goto only works inside a batch file, so the waiter is written to disk. tasklist
            // filtered by PID prints a process row (always containing ".exe") while the parent
            // lives and "INFO: No tasks..." once it is gone; ping -n 2 is a redirect-safe sleep
            // (timeout fails without a console). The game runs through cmd /c so its output lands
            // in a log file instead of dying with the headless parent.
            File script = new File(dir, "automodpack-relaunch.bat");
            String joined = String.join(" ", commandLine.stream().map(ReLauncher::quoteForCmd).toList());
            String batch =
                    "@echo off\r\n"
                            + ":wait\r\n"
                            + "tasklist /fi \"PID eq " + parentPid + "\" /nh | find \".exe\" >nul\r\n"
                            + "if %errorlevel%==0 (ping -n 2 127.0.0.1 >nul & goto wait)\r\n"
                            + "cd /d \"" + dir.getAbsolutePath() + "\"\r\n"
                            + "start \"\" /b cmd /c \"" + joined + " >> automodpack-relaunch.log 2>&1\"\r\n"
                            + "del \"%~f0\"\r\n";
            Files.writeString(script.toPath(), batch);
            builder = new ProcessBuilder("cmd", "/c", "start", "/min", "", script.getAbsolutePath());
        } else {
            String joined = String.join(" ", commandLine.stream().map(ReLauncher::quoteForSh).toList());
            builder = new ProcessBuilder("/bin/sh", "-c",
                    "while kill -0 " + parentPid + " 2>/dev/null; do sleep 1; done; cd "
                            + quoteForSh(dir.getAbsolutePath())
                            + " && exec " + joined + " >> automodpack-relaunch.log 2>&1");
        }
        builder.directory(dir);
        builder.start();
    }

    private static String quoteForCmd(String arg) {
        return "\"" + arg.replace("\"", "\"\"") + "\"";
    }

    private static String quoteForSh(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private void handleServerRestart(Runnable[] callbacks) {
        LOGGER.info("Please restart the server to apply updates!");
        runCallbacks(callbacks);
        System.exit(0);
    }

    private void waitFor(Semaphore semaphore) {
        if (semaphore != null) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Failed to acquire semaphore", e);
            }
        }
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
