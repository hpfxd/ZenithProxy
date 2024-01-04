package com.zenith;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.*;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundTeleportToEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.cache.DataCache;
import com.zenith.command.CommandManager;
import com.zenith.database.DatabaseManager;
import com.zenith.discord.DiscordBot;
import com.zenith.event.SimpleEventBus;
import com.zenith.feature.api.VcApi;
import com.zenith.feature.food.FoodManager;
import com.zenith.feature.items.ItemsManager;
import com.zenith.feature.language.LanguageManager;
import com.zenith.feature.pathing.Pathing;
import com.zenith.feature.pathing.blockdata.BlockDataManager;
import com.zenith.feature.prioban.PriorityBanChecker;
import com.zenith.feature.tps.TPSCalculator;
import com.zenith.feature.whitelist.PlayerListsManager;
import com.zenith.module.ModuleManager;
import com.zenith.network.client.ClientSession;
import com.zenith.network.client.handler.incoming.*;
import com.zenith.network.client.handler.incoming.entity.*;
import com.zenith.network.client.handler.incoming.inventory.*;
import com.zenith.network.client.handler.incoming.level.*;
import com.zenith.network.client.handler.incoming.spawn.AddEntityHandler;
import com.zenith.network.client.handler.incoming.spawn.AddExperienceOrbHandler;
import com.zenith.network.client.handler.incoming.spawn.AddPlayerHandler;
import com.zenith.network.client.handler.incoming.spawn.SpawnPositionHandler;
import com.zenith.network.client.handler.outgoing.OutgoingChatHandler;
import com.zenith.network.client.handler.outgoing.OutgoingContainerClickHandler;
import com.zenith.network.client.handler.postoutgoing.*;
import com.zenith.network.registry.HandlerRegistry;
import com.zenith.network.server.ServerConnection;
import com.zenith.network.server.handler.player.InGameCommandManager;
import com.zenith.network.server.handler.player.incoming.*;
import com.zenith.network.server.handler.player.outgoing.SystemChatOutgoingHandler;
import com.zenith.network.server.handler.player.postoutgoing.LoginPostHandler;
import com.zenith.network.server.handler.shared.incoming.HelloHandler;
import com.zenith.network.server.handler.shared.incoming.KeepAliveHandler;
import com.zenith.network.server.handler.shared.outgoing.GameProfileOutgoingHandler;
import com.zenith.network.server.handler.shared.outgoing.PingOutgoingHandler;
import com.zenith.network.server.handler.shared.outgoing.ServerTablistDataOutgoingHandler;
import com.zenith.network.server.handler.spectator.incoming.*;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.outgoing.*;
import com.zenith.network.server.handler.spectator.postoutgoing.LoginSpectatorPostHandler;
import com.zenith.terminal.TerminalManager;
import com.zenith.util.Config;
import com.zenith.util.LaunchConfig;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@UtilityClass
public class Shared {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }
    public static final Logger DEFAULT_LOG = LoggerFactory.getLogger("Proxy");
    public static final Logger AUTH_LOG = LoggerFactory.getLogger("Auth");
    public static final Logger CACHE_LOG = LoggerFactory.getLogger("Cache");
    public static final Logger CLIENT_LOG = LoggerFactory.getLogger("Client");
    public static final Logger CHAT_LOG = LoggerFactory.getLogger("Chat");
    public static final Logger MODULE_LOG = LoggerFactory.getLogger("Module");
    public static final Logger SERVER_LOG = LoggerFactory.getLogger("Server");
    public static final Logger DISCORD_LOG = LoggerFactory.getLogger("Discord");
    public static final Logger DATABASE_LOG = LoggerFactory.getLogger("Database");
    public static final Logger TERMINAL_LOG = LoggerFactory.getLogger("Terminal");
    public static final File CONFIG_FILE = new File("config.json");
    public static final File LAUNCH_CONFIG_FILE = new File("launch_config.json");
    public static final String SERVER_RESTARTING = "Server restarting";
    public static final String SYSTEM_DISCONNECT = "System disconnect";
    public static final String MANUAL_DISCONNECT = "Manual Disconnect";
    public static final String AUTO_DISCONNECT = "AutoDisconnect";
    public static final String LOGIN_FAILED = "Login Failed";
    public static boolean isReconnectableDisconnect(final String reason) {
        if (reason.equals(SYSTEM_DISCONNECT)
            || reason.equals(MANUAL_DISCONNECT)
            || reason.equals(MinecraftConstants.SERVER_CLOSING_MESSAGE)
            || reason.equals(LOGIN_FAILED)
        ) {
            return false;
        } else if (reason.equals(AUTO_DISCONNECT)) {
            return (!CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect && !Proxy.getInstance()
                .getIsPrio()
                .orElse(false));
        } else {
            return true;
        }
    }
    public static Config CONFIG;
    public static LaunchConfig LAUNCH_CONFIG;
    public static final DataCache CACHE;
    public static final DiscordBot DISCORD_BOT;
    public static final SimpleEventBus EVENT_BUS;
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;
    public static final PlayerListsManager PLAYER_LISTS;
    public static final PriorityBanChecker PRIORITY_BAN_CHECKER;
    public static final BlockDataManager BLOCK_DATA_MANAGER;
    public static final DatabaseManager DATABASE_MANAGER;
    public static final TPSCalculator TPS_CALCULATOR;
    public static final ModuleManager MODULE_MANAGER;
    public static final Pathing PATHING;
    public static final TerminalManager TERMINAL_MANAGER;
    public static final InGameCommandManager IN_GAME_COMMAND_MANAGER;
    public static final CommandManager COMMAND_MANAGER;
    public static final LanguageManager LANGUAGE_MANAGER;
    public static final FoodManager FOOD_MANAGER;
    public static final ItemsManager ITEMS_MANAGER;
    public static final VcApi VC_API;
    public static volatile boolean SHOULD_RECONNECT;

    public static final HandlerRegistry<ServerConnection> SERVER_PLAYER_HANDLERS = new HandlerRegistry.Builder<ServerConnection>()
        .setLogger(SERVER_LOG)
        .allowUnhandled(true)
        //
        // Inbound packets
        //
        .registerInbound(ServerboundHelloPacket.class, new HelloHandler())
        .registerInbound(ServerboundKeepAlivePacket.class, new KeepAliveHandler())
        .registerInbound(ServerboundChatCommandPacket.class, new ChatCommandHandler())
        .registerInbound(ServerboundChatPacket.class, new ChatHandler())
        .registerInbound(ServerboundClientInformationPacket.class, new ClientInformationHandler())
        .registerInbound(ServerboundPongPacket.class, new PongHandler())
        .registerInbound(ServerboundClientCommandPacket.class, new ClientCommandHandler())
        //
        // Outbound packets
        //
        .registerOutbound(ClientboundGameProfilePacket.class, new GameProfileOutgoingHandler())
        .registerOutbound(ClientboundPingPacket.class, new PingOutgoingHandler())
        .registerOutbound(ClientboundTabListPacket.class, new ServerTablistDataOutgoingHandler())
        .registerOutbound(ClientboundSystemChatPacket.class, new SystemChatOutgoingHandler())
        //
        // Post-outbound packets
        //
        .registerPostOutbound(ClientboundLoginPacket.class, new LoginPostHandler())
        .build();
    public static final HandlerRegistry<ServerConnection> SERVER_SPECTATOR_HANDLERS = new HandlerRegistry.Builder<ServerConnection>()
        .setLogger(SERVER_LOG)
        .allowUnhandled(false)

        .registerInbound(ServerboundHelloPacket.class, new HelloHandler())
        .registerInbound(ServerboundKeepAlivePacket.class, new KeepAliveHandler())
        .registerInbound(ServerboundPongPacket.class, new SpectatorPongHandler())
        .registerInbound(ServerboundMovePlayerPosRotPacket.class, new PlayerPositionRotationSpectatorHandler())
        .registerInbound(ServerboundMovePlayerPosPacket.class, new PlayerPositionSpectatorHandler())
        .registerInbound(ServerboundMovePlayerRotPacket.class, new PlayerRotationSpectatorHandler())
        .registerInbound(ServerboundChatPacket.class, new ServerChatSpectatorHandler())
        .registerInbound(ServerboundPlayerCommandPacket.class, new PlayerCommandSpectatorHandler())
        .registerInbound(ServerboundTeleportToEntityPacket.class, new TeleportToEntitySpectatorHandler())
        .registerInbound(ServerboundInteractPacket.class, new InteractEntitySpectatorHandler())

        .registerOutbound(ClientboundGameProfilePacket.class, new GameProfileOutgoingHandler())
        .registerOutbound(ClientboundPingPacket.class, new PingOutgoingHandler())

        .registerOutbound(ClientboundContainerClosePacket.class, new ContainerCloseSpectatorOutgoingHandler())
        .registerOutbound(ClientboundContainerSetContentPacket.class, new ContainerSetContentSpectatorOutgoingHandler())
        .registerOutbound(ClientboundPlaceGhostRecipePacket.class, new PlaceGhostRecipeSpectatorOutgoingHandler())
        .registerOutbound(ClientboundOpenScreenPacket.class, new OpenScreenSpectatorOutgoingHandler())
        .registerOutbound(ClientboundSetCarriedItemPacket.class, new SetCarriedItemSpectatorOutgoingHandler())
        .registerOutbound(ClientboundSetHealthPacket.class, new SetHealthSpectatorOutgoingHandler())
        .registerOutbound(ClientboundPlayerPositionPacket.class, new PlayerPositionSpectatorOutgoingHandler())
        .registerOutbound(ClientboundSetExperiencePacket.class, new SetExperienceSpectatorOutgoingHandler())
        .registerOutbound(ClientboundOpenBookPacket.class, new OpenBookSpectatorOutgoingHandler())
        .registerOutbound(ClientboundContainerSetSlotPacket.class, new ContainerSetSlotSpectatorOutgoingHandler())
        .registerOutbound(ClientboundGameEventPacket.class, new GameEventSpectatorOutgoingHandler())
        .registerOutbound(ClientboundMoveVehiclePacket.class, new MoveVehicleSpectatorOutgoingHandler())
        .registerOutbound(ClientboundHorseScreenOpenPacket.class, new HorseScreenOpenSpectatorOutgoingHandler())
        .registerOutbound(ClientboundContainerSetDataPacket.class, new ContainerSetDataSpectatorOutgoingHandler())
        .registerOutbound(ClientboundTabListPacket.class, new ServerTablistDataOutgoingHandler())
        .registerOutbound(ClientboundPlayerAbilitiesPacket.class, new PlayerAbilitiesSpectatorOutgoingHandler())
        .registerOutbound(ClientboundRespawnPacket.class, new RespawnSpectatorOutgoingPacket())

        .registerPostOutbound(ClientboundLoginPacket.class, new LoginSpectatorPostHandler())
        .build();

    public static final HandlerRegistry<ClientSession> CLIENT_HANDLERS = new HandlerRegistry.Builder<ClientSession>()
        .setLogger(CLIENT_LOG)
        .allowUnhandled(true)
        //
        // Inbound packets
        //
        .registerInbound(ClientboundUpdateAdvancementsPacket.class, new UpdateAdvancementsHandler())
        .registerInbound(ClientboundBlockUpdatePacket.class, new BlockUpdateHandler())
        .registerInbound(ClientboundChangeDifficultyPacket.class, new ChangeDifficultyHandler())
        .registerInbound(ClientboundBossEventPacket.class, new BossEventHandler())
        .registerInbound(ClientboundChunksBiomesPacket.class, new ChunksBiomesHandler())
        .registerInbound(ClientboundSystemChatPacket.class, new SystemChatHandler())
        .registerInbound(ClientboundPlayerChatPacket.class, new PlayerChatHandler())
        .registerInbound(ClientboundLevelChunkWithLightPacket.class, new LevelChunkWithLightHandler())
        .registerInbound(ClientboundLightUpdatePacket.class, new LightUpdateHandler())
        .registerInbound(ClientboundKeepAlivePacket.class, new ClientKeepaliveHandler())
        .registerInbound(ClientboundCommandsPacket.class, new CommandsHandler())
        .registerInbound(ClientboundGameEventPacket.class, new GameEventHandler())
        .registerInbound(ClientboundLoginPacket.class, new LoginHandler())
        .registerInbound(ClientboundGameProfilePacket.class, new GameProfileHandler())
        .registerInbound(ClientboundSectionBlocksUpdatePacket.class, new SectionBlocksUpdateHandler())
        .registerInbound(ClientboundSetCarriedItemPacket.class, new SetCarriedItemHandler())
        .registerInbound(ClientboundSetChunkCacheCenterPacket.class, new SetChunkCacheCenterHandler())
        .registerInbound(ClientboundSetChunkCacheRadiusPacket.class, new SetChunkCacheRadiusHandler())
        .registerInbound(ClientboundSetSimulationDistancePacket.class, new SetSimulationDistanceHandler())
        .registerInbound(ClientboundSetHealthPacket.class, new SetHealthHandler())
        .registerInbound(ClientboundSetSubtitleTextPacket.class, new SetSubtitleTextHandler())
        .registerInbound(ClientboundPlayerPositionPacket.class, new PlayerPositionHandler())
        .registerInbound(ClientboundSoundPacket.class, new SoundHandler())
        .registerInbound(ClientboundSetExperiencePacket.class, new SetExperienceHandler())
        .registerInbound(ClientboundRespawnPacket.class, new RespawnHandler())
        .registerInbound(ClientboundContainerSetSlotPacket.class, new ContainerSetSlotHandler())
        .registerInbound(ClientboundContainerSetContentPacket.class, new ContainerSetContentHandler())
        .registerInbound(ClientboundAwardStatsPacket.class, new AwardStatsHandler())
        .registerInbound(ClientboundTabListPacket.class, new TabListDataHandler())
        .registerInbound(ClientboundUpdateEnabledFeaturesPacket.class, new UpdateEnabledFeaturesHandler())
        .registerInbound(ClientboundPlayerInfoUpdatePacket.class, new PlayerInfoUpdateHandler())
        .registerInbound(ClientboundExplodePacket.class, new ExplodeHandler())
        .registerInbound(ClientboundPlayerInfoRemovePacket.class, new PlayerInfoRemoveHandler())
        .registerInbound(ClientboundSetActionBarTextPacket.class, new SetActionBarTextHandler())
        .registerInbound(ClientboundSetEntityMotionPacket.class, new SetEntityMotionHandler())
        .registerInbound(ClientboundForgetLevelChunkPacket.class, new ForgetLevelChunkHandler())
        .registerInbound(ClientboundUpdateRecipesPacket.class, new SyncRecipesHandler())
        .registerInbound(ClientboundUpdateTagsPacket.class, new UpdateTagsHandler())
        .registerInbound(ClientboundInitializeBorderPacket.class, new WorldBorderInitializeHandler())
        .registerInbound(ClientboundBlockEntityDataPacket.class, new BlockEntityDataHandler())
        .registerInbound(ClientboundSetTimePacket.class, new SetTimeHandler())
        .registerInbound(ClientboundPlayerCombatKillPacket.class, new PlayerCombatKillHandler())
        .registerInbound(ClientboundMapItemDataPacket.class, new MapDataHandler())
        .registerInbound(ClientboundPingPacket.class, new PingHandler())
        .registerInbound(ClientboundPlayerAbilitiesPacket.class, new PlayerAbilitiesHandler())
        .registerInbound(ClientboundCustomPayloadPacket.class, new CustomPayloadHandler())
        .registerInbound(ClientboundRecipePacket.class, new UnlockRecipeHandler())
        .registerInbound(ClientboundSetPlayerTeamPacket.class, new TeamHandler())
        //ENTITY
        .registerInbound(ClientboundEntityEventPacket.class, new EntityEventHandler())
        .registerInbound(ClientboundSetEntityLinkPacket.class, new SetEntityLinkHandler())
        .registerInbound(ClientboundTakeItemEntityPacket.class, new TakeItemEntityHandler())
        .registerInbound(ClientboundRemoveEntitiesPacket.class, new RemoveEntitiesHandler())
        .registerInbound(ClientboundUpdateMobEffectPacket.class, new UpdateMobEffectHandler())
        .registerInbound(ClientboundRemoveMobEffectPacket.class, new RemoveMobEffectHandler())
        .registerInbound(ClientboundSetEquipmentPacket.class, new SetEquipmentHandler())
        .registerInbound(ClientboundRotateHeadPacket.class, new RotateHeadHandler())
        .registerInbound(ClientboundSetEntityDataPacket.class, new SetEntityDataHandler())
        .registerInbound(ClientboundMoveEntityPosPacket.class, new MoveEntityPosHandler())
        .registerInbound(ClientboundMoveEntityPosRotPacket.class, new MoveEntityPosRotHandler())
        .registerInbound(ClientboundUpdateAttributesPacket.class, new UpdateAttributesHandler())
        .registerInbound(ClientboundMoveEntityRotPacket.class, new MoveEntityRotHandler())
        .registerInbound(ClientboundMoveVehiclePacket.class, new MoveVehicleHandler())
        .registerInbound(ClientboundSetPassengersPacket.class, new EntitySetPassengersHandler())
        .registerInbound(ClientboundTeleportEntityPacket.class, new TeleportEntityHandler())
        //SPAWN
        .registerInbound(ClientboundAddExperienceOrbPacket.class, new AddExperienceOrbHandler())
        .registerInbound(ClientboundAddEntityPacket.class, new AddEntityHandler())
        .registerInbound(ClientboundAddPlayerPacket.class, new AddPlayerHandler())
        .registerInbound(ClientboundSetDefaultSpawnPositionPacket.class, new SpawnPositionHandler())
        // Outbound
        .registerOutbound(ServerboundChatPacket.class, new OutgoingChatHandler())
        .registerOutbound(ServerboundContainerClickPacket.class, new OutgoingContainerClickHandler())
        //Postoutgoing
        .registerPostOutbound(ServerboundMoveVehiclePacket.class, new PostOutgoingMoveVehicleHandler())
        .registerPostOutbound(ServerboundPlayerCommandPacket.class, new PostOutgoingPlayerCommandHandler())
        .registerPostOutbound(ServerboundSetCarriedItemPacket.class, new PostOutgoingSetCarriedItemHandler())
        .registerPostOutbound(ServerboundMovePlayerPosPacket.class, new PostOutgoingPlayerPositionHandler())
        .registerPostOutbound(ServerboundMovePlayerPosRotPacket.class, new PostOutgoingPlayerPositionRotationHandler())
        .registerPostOutbound(ServerboundMovePlayerRotPacket.class, new PostOutgoingPlayerRotationHandler())
        .registerPostOutbound(ServerboundMovePlayerStatusOnlyPacket.class, new PostOutgoingPlayerStatusOnlyHandler())
        .registerPostOutbound(ServerboundSwingPacket.class, new PostOutgoingSwingHandler())
        .build();

    public static synchronized void loadConfig() {
        try {
            DEFAULT_LOG.info("Loading config...");

            Config config;
            if (CONFIG_FILE.exists()) {
                try (Reader reader = new FileReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, Config.class);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load config!", e);
                }
            } else {
                config = new Config();
            }

            CONFIG = config.doPostLoad();
            SHOULD_RECONNECT = CONFIG.client.extra.autoReconnect.enabled;
            PLAYER_LISTS.init();
            DEFAULT_LOG.info("Config loaded.");
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to load config!", e);
            System.exit(1);
        }
    }

    public static synchronized void loadLaunchConfig() {
        try {
            DEFAULT_LOG.info("Loading launch config...");

            LaunchConfig config = null;
            if (LAUNCH_CONFIG_FILE.exists()) {
                try (Reader reader = new FileReader(LAUNCH_CONFIG_FILE)) {
                    config = GSON.fromJson(reader, LaunchConfig.class);
                } catch (IOException e) {
                    DEFAULT_LOG.error("Unable to load launch config. Writing default config", e);
                    saveLaunchConfig();
                }
            } else {
                saveLaunchConfig();
            }
            if (config == null) {
                if (LAUNCH_CONFIG == null) LAUNCH_CONFIG = new LaunchConfig();
            } else LAUNCH_CONFIG = config;
            CONFIG.autoUpdater.autoUpdate = LAUNCH_CONFIG.auto_update;
            DEFAULT_LOG.info("Launch config loaded.");
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to load launch config!", e);
            System.exit(1);
        }
    }

    public static void saveConfigAsync() {
        Thread.ofVirtual().name("Async Config Save").start(Shared::saveConfig);
    }

    public static synchronized void saveConfig() {
        DEFAULT_LOG.debug("Saving config...");

        if (CONFIG == null) {
            DEFAULT_LOG.warn("Config is not set, saving default config!");
            CONFIG = new Config().doPostLoad();
        }

        try {
            final File tempFile = new File(CONFIG_FILE.getAbsolutePath() + ".tmp");
            if (tempFile.exists()) tempFile.delete();
            try (Writer out = new FileWriter(tempFile)) {
                GSON.toJson(CONFIG, out);
            }
            Files.move(tempFile, CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save config!", e);
        }

        DEFAULT_LOG.debug("Config saved.");
    }
    public static synchronized void saveLaunchConfig() {
        DEFAULT_LOG.debug("Saving launch config...");

        if (LAUNCH_CONFIG == null) {
            DEFAULT_LOG.warn("Launch config is not set, saving default config!");
            LAUNCH_CONFIG = new LaunchConfig();
        }

        try {
            final File tempFile = new File(LAUNCH_CONFIG_FILE.getAbsolutePath() + ".tmp");
            if (tempFile.exists()) tempFile.delete();
            try (Writer out = new FileWriter(tempFile)) {
                GSON.toJson(LAUNCH_CONFIG, out);
            }
            Files.move(tempFile, LAUNCH_CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save launch config!", e);
        }

        DEFAULT_LOG.debug("Launch config saved.");
    }

    static {
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
                DEFAULT_LOG.error("Uncaught exception in thread {}", thread, e);
            });
            SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(4, new ThreadFactoryBuilder()
                .setNameFormat("ZenithProxy Scheduled Executor - #%d")
                .setDaemon(true)
                .build());
            DISCORD_BOT = new DiscordBot();
            EVENT_BUS = new SimpleEventBus(Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("ZenithProxy Async EventBus - #%d")
                .setDaemon(true)
                .build()));
            CACHE = new DataCache();
            PLAYER_LISTS = new PlayerListsManager();
            PRIORITY_BAN_CHECKER = new PriorityBanChecker();
            BLOCK_DATA_MANAGER = new BlockDataManager();
            DATABASE_MANAGER = new DatabaseManager();
            TPS_CALCULATOR = new TPSCalculator();
            MODULE_MANAGER = new ModuleManager();
            PATHING = new Pathing();
            TERMINAL_MANAGER = new TerminalManager();
            IN_GAME_COMMAND_MANAGER = new InGameCommandManager();
            COMMAND_MANAGER = new CommandManager();
            LANGUAGE_MANAGER = new LanguageManager();
            FOOD_MANAGER = new FoodManager();
            ITEMS_MANAGER = new ItemsManager();
            VC_API = new VcApi();
            TranslationRegistry translationRegistry = TranslationRegistry.create(Key.key("minecraft"));
            translationRegistry.registerAll(Locale.ENGLISH, LANGUAGE_MANAGER.getLanguageDataMap());
            GlobalTranslator.translator().addSource(translationRegistry);
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to initialize!", e);
            throw e;
        }
    }

}
