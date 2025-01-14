package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.PlayerLoginEvent;
import com.zenith.event.proxy.ServerConnectionRemovedEvent;
import com.zenith.feature.actionlimiter.handlers.inbound.*;
import com.zenith.feature.actionlimiter.handlers.outbound.ALCMoveVehicleHandler;
import com.zenith.feature.actionlimiter.handlers.outbound.ALLoginHandler;
import com.zenith.feature.actionlimiter.handlers.outbound.ALPlayerPositionHandler;
import com.zenith.module.Module;
import com.zenith.network.registry.HandlerRegistry;
import com.zenith.network.server.ServerConnection;
import lombok.Getter;

import java.util.HashSet;
import java.util.function.Supplier;

import static com.zenith.Shared.*;
import static com.zenith.event.SimpleEventBus.pair;

public class ActionLimiter extends Module {
    @Getter
    private HandlerRegistry<ServerConnection> handlerRegistry;
    private final HashSet<ServerConnection> limitedConnections = new HashSet<>();

    public ActionLimiter() {
        initializeHandlers();
    }

    private void initializeHandlers() {
        handlerRegistry = new HandlerRegistry.Builder<ServerConnection>()
            .setLogger(MODULE_LOG)
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
            .build();
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            pair(PlayerLoginEvent.class, this::onPlayerLoginEvent),
            pair(ServerConnectionRemovedEvent.class, this::onServerConnectionRemoved)
        );
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.actionLimiter.enabled;
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

    public boolean bypassesLimits(final ServerConnection serverConnection) {
        return !limitedConnections.contains(serverConnection);
    }
}
