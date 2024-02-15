package com.zenith.network.registry;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.*;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundTeleportToEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import com.github.steveice10.mc.protocol.packet.login.clientbound.*;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.client.handler.incoming.*;
import com.zenith.network.client.handler.incoming.entity.*;
import com.zenith.network.client.handler.incoming.inventory.*;
import com.zenith.network.client.handler.incoming.level.*;
import com.zenith.network.client.handler.incoming.scoreboard.SetDisplayObjectiveHandler;
import com.zenith.network.client.handler.incoming.scoreboard.SetObjectiveHandler;
import com.zenith.network.client.handler.incoming.scoreboard.SetScoreHandler;
import com.zenith.network.client.handler.incoming.scoreboard.TeamHandler;
import com.zenith.network.client.handler.incoming.spawn.AddEntityHandler;
import com.zenith.network.client.handler.incoming.spawn.AddExperienceOrbHandler;
import com.zenith.network.client.handler.incoming.spawn.AddPlayerHandler;
import com.zenith.network.client.handler.incoming.spawn.SpawnPositionHandler;
import com.zenith.network.client.handler.outgoing.OutgoingChatHandler;
import com.zenith.network.client.handler.outgoing.OutgoingContainerClickHandler;
import com.zenith.network.client.handler.postoutgoing.*;
import com.zenith.network.server.ServerConnection;
import com.zenith.network.server.handler.player.incoming.*;
import com.zenith.network.server.handler.player.outgoing.ClientCommandsOutgoingHandler;
import com.zenith.network.server.handler.player.outgoing.SystemChatOutgoingHandler;
import com.zenith.network.server.handler.player.postoutgoing.LoginPostHandler;
import com.zenith.network.server.handler.shared.incoming.*;
import com.zenith.network.server.handler.shared.outgoing.PingOutgoingHandler;
import com.zenith.network.server.handler.shared.outgoing.SGameProfileOutgoingHandler;
import com.zenith.network.server.handler.shared.outgoing.ServerTablistDataOutgoingHandler;
import com.zenith.network.server.handler.shared.postoutgoing.LoginCompressionPostOutgoingHandler;
import com.zenith.network.server.handler.shared.postoutgoing.SGameProfilePostHandler;
import com.zenith.network.server.handler.spectator.incoming.*;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.outgoing.*;
import com.zenith.network.server.handler.spectator.postoutgoing.LoginSpectatorPostHandler;

import static com.zenith.Shared.*;

public final class ZenithHandlerCodec {
    private ZenithHandlerCodec() {}
    public static final CodecRegistry CLIENT_REGISTRY = new CodecRegistry("Client Handlers");
    public static final CodecRegistry SERVER_REGISTRY = new CodecRegistry("Server Handlers");

    static {
        final PacketHandlerCodec CLIENT_CODEC = PacketHandlerCodec.builder()
            .setId("client")
            .setPriority(0)
            .setLogger(CLIENT_LOG)
            .state(ProtocolState.HANDSHAKE, PacketHandlerStateCodec.<ClientSession>builder()
                .allowUnhandled(false)
                .registerPostOutbound(ClientIntentionPacket.class, new PostOutgoingClientIntentionHandler())
                .build())
            .state(ProtocolState.STATUS, PacketHandlerStateCodec.<ClientSession>builder()
                .allowUnhandled(false)
                .registerInbound(ClientboundStatusResponsePacket.class, new CStatusResponseHandler())
                .registerInbound(ClientboundPongResponsePacket.class, new PongResponseHandler())
                .build())
            .state(ProtocolState.LOGIN, PacketHandlerStateCodec.<ClientSession>builder()
                .allowUnhandled(false)
                .registerInbound(ClientboundHelloPacket.class, new CHelloHandler())
                .registerInbound(ClientboundLoginCompressionPacket.class, new CLoginCompressionHandler())
                .registerInbound(ClientboundGameProfilePacket.class, new CGameProfileHandler())
                .registerInbound(ClientboundLoginDisconnectPacket.class, new LoginDisconnectHandler())
                .registerInbound(ClientboundCustomQueryPacket.class, new CCustomQueryHandler())
                .build())
            .state(ProtocolState.GAME, PacketHandlerStateCodec.<ClientSession>builder()
                .allowUnhandled(true)
                .registerInbound(ClientboundDisconnectPacket.class, new CDisconnectHandler())
                .registerInbound(ClientboundUpdateAdvancementsPacket.class, new UpdateAdvancementsHandler())
                .registerInbound(ClientboundBlockUpdatePacket.class, new BlockUpdateHandler())
                .registerInbound(ClientboundChangeDifficultyPacket.class, new ChangeDifficultyHandler())
                .registerInbound(ClientboundBossEventPacket.class, new BossEventHandler())
                .registerInbound(ClientboundChunksBiomesPacket.class, new ChunksBiomesHandler())
                .registerInbound(ClientboundSystemChatPacket.class, new SystemChatHandler())
                .registerInbound(ClientboundPlayerChatPacket.class, new PlayerChatHandler())
                .registerInbound(ClientboundLevelChunkWithLightPacket.class, new LevelChunkWithLightHandler())
                .registerInbound(ClientboundLightUpdatePacket.class, new LightUpdateHandler())
                .registerInbound(ClientboundKeepAlivePacket.class, new CKeepAliveHandler())
                .registerInbound(ClientboundCommandsPacket.class, new CommandsHandler())
                .registerInbound(ClientboundGameEventPacket.class, new GameEventHandler())
                .registerInbound(ClientboundLoginPacket.class, new LoginHandler())
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
                .registerInbound(ClientboundContainerClosePacket.class, new ContainerCloseHandler())
                .registerInbound(ClientboundOpenScreenPacket.class, new ContainerOpenScreenHandler())
                .registerInbound(ClientboundContainerSetSlotPacket.class, new ContainerSetSlotHandler())
                .registerInbound(ClientboundContainerSetContentPacket.class, new ContainerSetContentHandler())
                .registerInbound(ClientboundAwardStatsPacket.class, new AwardStatsHandler())
                .registerInbound(ClientboundTabListPacket.class, new TabListDataHandler())
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
                .registerInbound(ClientboundSetObjectivePacket.class, new SetObjectiveHandler())
                .registerInbound(ClientboundSetDisplayObjectivePacket.class, new SetDisplayObjectiveHandler())
                .registerInbound(ClientboundSetScorePacket.class, new SetScoreHandler())
                .registerInbound(ClientboundUpdateEnabledFeaturesPacket.class, new UpdateEnabledFeaturesHandler())
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
                .registerOutbound(ServerboundChatPacket.class, new OutgoingChatHandler())
                .registerOutbound(ServerboundContainerClickPacket.class, new OutgoingContainerClickHandler())
                .registerPostOutbound(ServerboundMoveVehiclePacket.class, new PostOutgoingMoveVehicleHandler())
                .registerPostOutbound(ServerboundPlayerCommandPacket.class, new PostOutgoingPlayerCommandHandler())
                .registerPostOutbound(ServerboundSetCarriedItemPacket.class, new PostOutgoingSetCarriedItemHandler())
                .registerPostOutbound(ServerboundMovePlayerPosPacket.class, new PostOutgoingPlayerPositionHandler())
                .registerPostOutbound(ServerboundMovePlayerPosRotPacket.class, new PostOutgoingPlayerPositionRotationHandler())
                .registerPostOutbound(ServerboundMovePlayerRotPacket.class, new PostOutgoingPlayerRotationHandler())
                .registerPostOutbound(ServerboundMovePlayerStatusOnlyPacket.class, new PostOutgoingPlayerStatusOnlyHandler())
                .registerPostOutbound(ServerboundSwingPacket.class, new PostOutgoingSwingHandler())
                .registerPostOutbound(ServerboundContainerClosePacket.class, new PostOutgoingContainerCloseHandler())
                .registerPostOutbound(ServerboundContainerClickPacket.class, new PostOutgoingContainerClickHandler())
                .registerPostOutbound(ServerboundPlayerActionPacket.class, new PostOutgoingPlayerActionHandler())
                .build())
            .build();

        final PacketHandlerCodec SERVER_PLAYER_CODEC = PacketHandlerCodec.builder()
            .setId("server-player")
            .setPriority(2)
            .setActivePredicate((connection) -> !((ServerConnection) connection).isSpectator())
            .setLogger(SERVER_LOG)
            .state(ProtocolState.GAME, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerInbound(ServerboundChatCommandPacket.class, new ChatCommandHandler())
                .registerInbound(ServerboundChatPacket.class, new ChatHandler())
                .registerInbound(ServerboundClientInformationPacket.class, new ClientInformationHandler())
                .registerInbound(ServerboundCommandSuggestionPacket.class, new CommandSuggestionHandler())
                .registerInbound(ServerboundPongPacket.class, new PongHandler())
                .registerInbound(ServerboundClientCommandPacket.class, new ClientCommandHandler())
                .registerOutbound(ClientboundCommandsPacket.class, new ClientCommandsOutgoingHandler())
                .registerOutbound(ClientboundSystemChatPacket.class, new SystemChatOutgoingHandler())
                .registerPostOutbound(ClientboundLoginPacket.class, new LoginPostHandler())
                .build())
            .build();

        final PacketHandlerCodec SERVER_SPECTATOR_CODEC = PacketHandlerCodec.builder()
            .setId("server-spectator")
            .setPriority(1)
            .setActivePredicate((connection) -> ((ServerConnection) connection).isSpectator())
            .setLogger(SERVER_LOG)
            .state(ProtocolState.GAME, PacketHandlerStateCodec.<ServerConnection>builder()
                .allowUnhandled(false)
                .registerInbound(ServerboundPongPacket.class, new SpectatorPongHandler())
                .registerInbound(ServerboundMovePlayerPosRotPacket.class, new PlayerPositionRotationSpectatorHandler())
                .registerInbound(ServerboundMovePlayerPosPacket.class, new PlayerPositionSpectatorHandler())
                .registerInbound(ServerboundMovePlayerRotPacket.class, new PlayerRotationSpectatorHandler())
                .registerInbound(ServerboundChatPacket.class, new ServerChatSpectatorHandler())
                .registerInbound(ServerboundPlayerCommandPacket.class, new PlayerCommandSpectatorHandler())
                .registerInbound(ServerboundTeleportToEntityPacket.class, new TeleportToEntitySpectatorHandler())
                .registerInbound(ServerboundInteractPacket.class, new InteractEntitySpectatorHandler())
                .registerOutbound(ClientboundGameProfilePacket.class, new SGameProfileOutgoingHandler())
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
                .registerOutbound(ClientboundPlayerAbilitiesPacket.class, new PlayerAbilitiesSpectatorOutgoingHandler())
                .registerOutbound(ClientboundRespawnPacket.class, new RespawnSpectatorOutgoingPacket())
                .registerPostOutbound(ClientboundLoginPacket.class, new LoginSpectatorPostHandler())
                .build())
            .build();

        final PacketHandlerCodec SERVER_SHARED_CODEC = PacketHandlerCodec.builder()
            .setId("server-shared")
            .setPriority(0)
            .setLogger(SERVER_LOG)
            .state(ProtocolState.HANDSHAKE, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerInbound(ClientIntentionPacket.class, new IntentionHandler())
                .build())
            .state(ProtocolState.LOGIN, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerInbound(ServerboundKeyPacket.class, new KeyHandler())
                .registerInbound(ServerboundHelloPacket.class, new SHelloHandler())
                // state is switched to game and connection is set to spectator in SGameProfileOutgoingHandler
                .registerOutbound(ClientboundGameProfilePacket.class, new SGameProfileOutgoingHandler())
                .registerPostOutbound(ClientboundLoginCompressionPacket.class, new LoginCompressionPostOutgoingHandler())
                .registerPostOutbound(ClientboundGameProfilePacket.class, new SGameProfilePostHandler())
                .build())
            .state(ProtocolState.STATUS, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerInbound(ServerboundPingRequestPacket.class, new PingRequestHandler())
                .registerInbound(ServerboundStatusRequestPacket.class, new StatusRequestHandler())
                .build())
            .state(ProtocolState.GAME, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerInbound(ServerboundKeepAlivePacket.class, new KeepAliveHandler())
                .registerInbound(ServerboundPingRequestPacket.class, new PingRequestHandler())
                .registerOutbound(ClientboundPingPacket.class, new PingOutgoingHandler())
                .registerOutbound(ClientboundTabListPacket.class, new ServerTablistDataOutgoingHandler())
                .build())
            .build();

        final PacketHandlerCodec CLIENT_PACKETLOG = new PacketLogPacketHandlerCodec(
            "client-packet-log",
            CLIENT_LOG,
            () -> CONFIG.debug.packetLog.clientPacketLog
        );

        final PacketHandlerCodec SERVER_PACKETLOG = new PacketLogPacketHandlerCodec(
            "server-packet-log",
            SERVER_LOG,
            () -> CONFIG.debug.packetLog.serverPacketLog
        );

        CLIENT_REGISTRY.register(CLIENT_CODEC);
        CLIENT_REGISTRY.register(CLIENT_PACKETLOG);
        SERVER_REGISTRY.register(SERVER_PLAYER_CODEC);
        SERVER_REGISTRY.register(SERVER_SPECTATOR_CODEC);
        SERVER_REGISTRY.register(SERVER_SHARED_CODEC);
        SERVER_REGISTRY.register(SERVER_PACKETLOG);
    }
}
