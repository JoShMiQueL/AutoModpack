package pl.skidam.automodpack_core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.skidam.automodpack_core.config.Jsons;
import pl.skidam.automodpack_core.loader.*;
import pl.skidam.automodpack_core.modpack.ModpackExecutor;
import pl.skidam.automodpack_core.protocol.netty.NettyServer;

import java.nio.file.Path;

public class GlobalVariables {
    public static final Logger LOGGER = LogManager.getLogger("Suiteland-Updater");
    public static final String MOD_ID = "suiteland_updater"; // resource locations, lang keys, channels
    public static Boolean DEBUG = false;
    public static Boolean preload;
    public static String MC_VERSION;
    public static String AM_VERSION;
    public static String LOADER_VERSION;
    public static String LOADER;
    public static LoaderManagerService LOADER_MANAGER = new NullLoaderManager();
    public static ModpackLoaderService MODPACK_LOADER = new NullModpackLoader();
    public static GameCallService GAME_CALL = new NullGameCall();
    public static Path THIS_MOD_JAR;
    public static Path MODS_DIR;
    public static ModpackExecutor modpackExecutor;
    public static NettyServer hostServer;
    public static Jsons.ServerConfigFieldsV2 serverConfig;
    public static Jsons.ClientConfigFieldsV2 clientConfig;
    public static Jsons.KnownHostsFields knownHosts;
    public static final Path automodpackDir = Path.of("suiteland_updater");
    public static final Path hostModpackDir = automodpackDir.resolve("host-modpack");
    // TODO More server modpacks
    // Main - required
    // Addons - optional addon packs
    // Switches - optional or required packs, chosen by the player, only one can be installed at a time
    public static final Path hostContentModpackDir = hostModpackDir.resolve("main");
    public static Path hostModpackContentFile = hostModpackDir.resolve("suiteland_updater-content.json");
    public static Path serverConfigFile = automodpackDir.resolve("suiteland_updater-server.json");
    public static Path clientLocalMetadataFile = automodpackDir.resolve("suiteland_updater-client-metadata.json");
    public static Path clientDummyFilesFile = automodpackDir.resolve("suiteland_updater-dummy-files.json");
    public static Path clientDeletionTimeStamps = automodpackDir.resolve("suiteland_updater-deletion-timestamps-files.json");
    public static Path serverCoreConfigFile = automodpackDir.resolve("suiteland_updater-core.json");
    public static final Path privateDir = automodpackDir.resolve(".private");
    public static final Path serverSecretsFile = privateDir.resolve("suiteland_updater-secrets.json");
    public static final Path knownHostsFile = privateDir.resolve("suiteland_updater-known-hosts.json");
    public static final Path serverCertFile = privateDir.resolve("cert.crt");
    public static final Path serverPrivateKeyFile = privateDir.resolve("key.pem");


    // Client
    public static final Path modpackContentTempFile = automodpackDir.resolve("suiteland_updater-content.json.temp");
    public static final Path clientConfigFile = automodpackDir.resolve("suiteland_updater-client.json");
    public static final Path clientSecretsFile = privateDir.resolve("suiteland_updater-client-secrets.json");
    public static final Path modpacksDir = automodpackDir.resolve("modpacks");

    public static Path selectedModpackDir;
}
