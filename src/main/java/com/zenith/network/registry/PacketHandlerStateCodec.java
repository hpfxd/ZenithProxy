package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.*;
import lombok.experimental.Accessors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PacketHandlerStateCodec<S extends Session> {
    @NonNull
    protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers;
    @NonNull
    protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> outboundHandlers;
    @NonNull
    protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> postOutboundHandlers;
    protected final boolean allowUnhandled;

    public static <S extends Session> Builder<S> builder() {
        return new Builder<>();
    }

    public <P extends Packet> P handleInbound(@NonNull P packet, @NonNull S session) {
        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.inboundHandlers.get(packet.getClass());
        if (handler == null) {
            if (allowUnhandled) return packet;
            else return null;
        } else {
            return handler.apply(packet, session);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends Packet> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.outboundHandlers.get(packet.getClass());
        if (handler == null) {
            // allowUnhandled has no effect here
            return packet;
        } else {
            return handler.apply(packet, session);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends Packet> void handlePostOutgoing(@NonNull P packet, @NonNull S session) {

        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.postOutboundHandlers.get(packet.getClass());
        if (handler != null) {
            handler.apply(packet, session);
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder<S extends Session> {

        protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers = new Reference2ObjectOpenHashMap<>();

        protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> outboundHandlers = new Reference2ObjectOpenHashMap<>();

        protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> postOutboundHandlers = new Reference2ObjectOpenHashMap<>();
        protected boolean allowUnhandled = true;

        public PacketHandlerStateCodec.Builder<S> registerInbound(@NonNull Class<? extends Packet> packetClass, @NonNull PacketHandler<? extends Packet, S> handler) {
            this.inboundHandlers.put(packetClass, handler);
            return this;
        }

        public PacketHandlerStateCodec.Builder<S> registerOutbound(@NonNull Class<? extends Packet> packetClass, @NonNull PacketHandler<? extends Packet, S> handler) {
            this.outboundHandlers.put(packetClass, handler);
            return this;
        }

        public PacketHandlerStateCodec.Builder<S> registerPostOutbound(@NonNull Class<? extends Packet> packetClass, @NonNull PostOutgoingPacketHandler<? extends Packet, S> handler) {
            this.postOutboundHandlers.put(packetClass, handler);
            return this;
        }

        public PacketHandlerStateCodec.Builder<S> registerPostOutbound(@NonNull Class<? extends Packet> packetClass, @NonNull AsyncPacketHandler<? extends Packet, S> handler) {
            this.postOutboundHandlers.put(packetClass, handler);
            return this;
        }

        public PacketHandlerStateCodec.Builder<S> allowUnhandled(final boolean allowUnhandled) {
            this.allowUnhandled = allowUnhandled;
            return this;
        }

        public PacketHandlerStateCodec<S> build() {
            return new PacketHandlerStateCodec<>(this.inboundHandlers, this.outboundHandlers, this.postOutboundHandlers, this.allowUnhandled);
        }
    }
}
