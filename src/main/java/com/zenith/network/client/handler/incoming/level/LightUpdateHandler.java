package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLightUpdatePacket;
import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.SCHEDULED_EXECUTOR_SERVICE;

public class LightUpdateHandler implements AsyncIncomingHandler<ClientboundLightUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundLightUpdatePacket packet, final ClientSession session) {
        // todo: we should queue light updates instead of spawning a bunch of threads for each one
        long twentySecsBeforeNow = System.currentTimeMillis() - 30000;
        long connectTime = Proxy.getInstance().getConnectTime().toEpochMilli();
        if (connectTime > twentySecsBeforeNow) {
            // delay light updates until we populate the chunk cache
            SCHEDULED_EXECUTOR_SERVICE.schedule(() -> CACHE.getChunkCache().handleLightUpdate(packet),
                                                connectTime - twentySecsBeforeNow,
                                                TimeUnit.MILLISECONDS);
            return true;
        }
        return CACHE.getChunkCache().handleLightUpdate(packet);
    }

    @Override
    public Class<ClientboundLightUpdatePacket> getPacketClass() {
        return ClientboundLightUpdatePacket.class;
    }
}