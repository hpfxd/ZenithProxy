package com.zenith.network.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundTeleportToEntityPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class TeleportToEntitySpectatorHandler implements PacketHandler<ServerboundTeleportToEntityPacket, ServerConnection> {
    @Override
    public ServerboundTeleportToEntityPacket apply(final ServerboundTeleportToEntityPacket packet, final ServerConnection session) {
        final Entity targetEntity = CACHE.getEntityCache().get(packet.getTarget());
        if (targetEntity != null) {
            if (session.hasCameraTarget()) {
                session.setCameraTarget(null);
                session.send(new ClientboundSetCameraPacket(session.getSpectatorSelfEntityId()));
            }
            SpectatorUtils.syncSpectatorPositionToEntity(session, targetEntity);
        }
        return null;
    }
}
