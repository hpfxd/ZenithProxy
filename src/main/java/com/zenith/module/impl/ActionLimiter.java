package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import com.zenith.event.proxy.PlayerLoginEvent;
import com.zenith.event.proxy.ServerConnectionRemovedEvent;
import com.zenith.feature.actionlimiter.handlers.inbound.*;
import com.zenith.feature.actionlimiter.handlers.outbound.ALCMoveVehicleHandler;
import com.zenith.feature.actionlimiter.handlers.outbound.ALLoginHandler;
import com.zenith.feature.actionlimiter.handlers.outbound.ALPlayerPositionHandler;
import com.zenith.module.Module;
import com.zenith.network.registry.PacketHandlerCodec;
import com.zenith.network.registry.PacketHandlerStateCodec;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.network.server.ServerConnection;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import lombok.Getter;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class ActionLimiter extends Module {
    @Getter
    private PacketHandlerCodec codec;
    private final ReferenceSet<ServerConnection> limitedConnections = new ReferenceOpenHashSet<>();

    public ActionLimiter() {
        initializeHandlers();
    }

    private void initializeHandlers() {
        codec = PacketHandlerCodec.builder()
            .setId("action-limiter")
            .setPriority(1000)
            .setLogger(MODULE_LOG)
            .setActivePredicate((session) -> shouldLimit((ServerConnection) session))
            .state(ProtocolState.GAME, PacketHandlerStateCodec.<ServerConnection>builder()
                .allowUnhandled(true)
                .registerInbound(ServerboundChatCommandPacket.class, new ALChatCommandHandler())
                .registerInbound(ServerboundChatPacket.class, new ALChatHandler())
                .registerInbound(ServerboundClientCommandPacket.class, new ALClientCommandHandler())
                .registerInbound(ServerboundContainerClickPacket.class, new ALContainerClickHandler())
                .registerInbound(ServerboundInteractPacket.class, new ALInteractHandler())
                .registerInbound(ServerboundMovePlayerPosPacket.class, new ALMovePlayerPosHandler())
                .registerInbound(ServerboundMovePlayerPosRotPacket.class, new ALMovePlayerPosRotHandler())
                .registerInbound(ServerboundMoveVehiclePacket.class, new ALMoveVehicleHandler())
                .registerInbound(ServerboundPlayerActionPacket.class, new ALPlayerActionHandler())
                .registerInbound(ServerboundUseItemOnPacket.class, new ALUseItemOnHandler())
                .registerInbound(ServerboundUseItemPacket.class, new ALUseItemHandler())
                .registerOutbound(ClientboundMoveVehiclePacket.class, new ALCMoveVehicleHandler())
                .registerOutbound(ClientboundLoginPacket.class, new ALLoginHandler())
                .registerOutbound(ClientboundPlayerPositionPacket.class, new ALPlayerPositionHandler())
                .build())
            .build();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            of(PlayerLoginEvent.class, this::onPlayerLoginEvent),
                            of(ServerConnectionRemovedEvent.class, this::onServerConnectionRemoved)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.actionLimiter.enabled;
    }

    @Override
    public void onEnable() {
        ZenithHandlerCodec.SERVER_REGISTRY.register(codec);
    }

    @Override
    public void onDisable() {
        ZenithHandlerCodec.SERVER_REGISTRY.unregister(codec);
    }

    public void onPlayerLoginEvent(final PlayerLoginEvent event) {
        ServerConnection serverConnection = event.serverConnection();
        var profile = serverConnection.getProfileCache().getProfile();
        var proxyProfile = CACHE.getProfileCache().getProfile();
        if (profile != null && proxyProfile != null && profile.getId().equals(proxyProfile.getId()))
            return;
        limitedConnections.add(serverConnection);
    }

    public void onServerConnectionRemoved(final ServerConnectionRemovedEvent event) {
        limitedConnections.remove(event.serverConnection());
    }

    public boolean shouldLimit(final ServerConnection serverConnection) {
        return limitedConnections.contains(serverConnection);
    }
}
