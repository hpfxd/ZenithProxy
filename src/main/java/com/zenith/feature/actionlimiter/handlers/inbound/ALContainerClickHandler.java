package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALContainerClickHandler implements IncomingHandler<ServerboundContainerClickPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundContainerClickPacket packet, final ServerConnection session) {
        if (!CONFIG.client.extra.actionLimiter.allowInventory) return false;
        return true;
    }
}