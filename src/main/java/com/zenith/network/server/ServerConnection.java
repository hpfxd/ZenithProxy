package com.zenith.network.server;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.game.scoreboard.CollisionRule;
import com.github.steveice10.mc.protocol.data.game.scoreboard.NameTagVisibility;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamAction;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.codec.PacketCodecHelper;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.ServerProfileCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityCache;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.event.proxy.ProxySpectatorDisconnectedEvent;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.entity.SpectatorEntity;
import com.zenith.module.impl.ActionLimiter;
import com.zenith.util.ComponentSerializer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;


@Getter
@Setter
public class ServerConnection implements Session, SessionListener {
    protected final Session session;

    private static final int DEFAULT_COMPRESSION_THRESHOLD = 256;

    // Always empty post-1.7
    private static final String SERVER_ID = "";
    private static final KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            KEY_PAIR = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate server key pair.", e);
        }
    }

    private final byte[] challenge = new byte[4];
    private String username = "";

    public ServerConnection(final Session session) {
        ThreadLocalRandom.current().nextBytes(this.challenge);
        this.session = session;
        initSpectatorEntity();
    }

    protected long lastPacket = System.currentTimeMillis();
    protected long lastPingId = 0L;
    protected long lastPingTime = 0L;
    protected long ping = 0L;

    protected boolean whitelistChecked = false;
    protected boolean isPlayer = false;
    protected boolean isLoggedIn = false;
    protected boolean isSpectator = false;
    protected boolean onlySpectator = false;
    protected boolean allowSpectatorServerPlayerPosRotate = true;
    // allow spectator to set their camera to client
    // need to persist state to allow them in and out of this
    protected Entity cameraTarget = null;
    protected boolean showSelfEntity = true;
    protected int spectatorEntityId = 2147483647 - ThreadLocalRandom.current().nextInt(1000000);
    protected int spectatorSelfEntityId = spectatorEntityId - 1;
    protected UUID spectatorEntityUUID = UUID.randomUUID();
    protected ServerProfileCache profileCache = new ServerProfileCache();
    protected ServerProfileCache spectatorFakeProfileCache = new ServerProfileCache();
    protected PlayerCache spectatorPlayerCache = new PlayerCache(new EntityCache());
    protected SpectatorEntity spectatorEntity;

    /**
     * Team data
     */
    protected List<String> currentTeamMembers = new ArrayList<>();
    private static final String teamName = "ZenithProxy";
    private static final Component displayName = Component.text("ZenithProxy");
    private static final Component prefix = Component.text("");
    private static final Component suffix = Component.text("");
    private static final boolean friendlyFire = false;
    private static final boolean seeFriendlyInvisibles = false;

    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            if (!defaultPacketReceived(session, packet)) return;
            if (this.isLoggedIn)  {
                Packet p = packet;
                if (CONFIG.client.extra.actionLimiter.enabled && !MODULE_MANAGER.get(ActionLimiter.class).bypassesLimits(this)) {
                    p = MODULE_MANAGER.get(ActionLimiter.class).getHandlerRegistry().handleInbound(p, this);
                    if (p == null) return;
                }
                if (isSpectator()) {
                    p = SERVER_SPECTATOR_HANDLERS.handleInbound(p, this);
                    if (p != null) {
                        // there's no use case for this so I'm just disabling sending it to the client
                        // we still want spectator handlers to process the packet though
//                    Proxy.getInstance().getClient().sendAsync(packet);
                    }
                } else {
                    this.lastPacket = System.currentTimeMillis();
                    p = SERVER_PLAYER_HANDLERS.handleInbound(p, this);
                    if (p != null) {
                        Proxy.getInstance().getClient().sendAsync(p);
                    }
                }
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling Received packet: " + packet.getClass().getSimpleName(), e);
        }
    }

    private boolean defaultPacketReceived(Session session, Packet packet) {
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (protocol.getState() == ProtocolState.HANDSHAKE) {
            if (packet instanceof ClientIntentionPacket intentionPacket) {
                switch (intentionPacket.getIntent()) {
                    case STATUS:
                        protocol.setState(ProtocolState.STATUS);
                        break;
                    case LOGIN:
                        protocol.setState(ProtocolState.LOGIN);
                        if (intentionPacket.getProtocolVersion() > protocol.getCodec().getProtocolVersion()) {
                            session.disconnect("Outdated server! I'm still on " + protocol.getCodec().getMinecraftVersion() + ".");
                        } else if (intentionPacket.getProtocolVersion() < protocol.getCodec().getProtocolVersion()) {
                            session.disconnect("Outdated client! Please use " + protocol.getCodec().getMinecraftVersion() + ".");
                        }

                        break;
                    default:
                        throw new UnsupportedOperationException("Invalid client intent: " + intentionPacket.getIntent());
                }
            }
            return false;
        } else if (protocol.getState() == ProtocolState.LOGIN) {
            if (packet instanceof ServerboundHelloPacket p) {
                this.username = p.getUsername();

                if (session.getFlag(MinecraftConstants.VERIFY_USERS_KEY, true)) {
                    session.send(new ClientboundHelloPacket(SERVER_ID, KEY_PAIR.getPublic(), this.challenge));
                } else {
                    new Thread(new UserAuthTask(session, null)).start();
                }
            } else if (packet instanceof ServerboundKeyPacket keyPacket) {
                PrivateKey privateKey = KEY_PAIR.getPrivate();

                if (!Arrays.equals(this.challenge, keyPacket.getEncryptedChallenge(privateKey))) {
                    session.disconnect("Invalid challenge!");
                    return false;
                }

                SecretKey key = keyPacket.getSecretKey(privateKey);
                session.enableEncryption(key);
                new Thread(new UserAuthTask(session, key)).start();
            } else if (packet instanceof ServerboundLoginAcknowledgedPacket) {
                ((MinecraftProtocol) session.getPacketProtocol()).setState(ProtocolState.CONFIGURATION);
                // todo: handle this more gracefully, connect and wait until we have configuration set (assuming session is auth'd)
                if (!Proxy.getInstance().isConnected()) {
                    session.disconnect("Proxy is not connected to a server.");
                    return false;
                }
                CACHE.getConfigurationCache().getPackets(session::sendAsync);
                session.sendAsync(new ClientboundCustomPayloadPacket("minecraft:brand", CACHE.getChunkCache().getServerBrand()));
                session.sendAsync(new ClientboundFinishConfigurationPacket());
            }
            return false;
        } else if (protocol.getState() == ProtocolState.STATUS) {
            if (packet instanceof ServerboundStatusRequestPacket) {
                ServerInfoBuilder builder = session.getFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY);
                if (builder == null) {
                    builder = $ -> new ServerStatusInfo(
                        new VersionInfo(protocol.getCodec().getMinecraftVersion(), protocol.getCodec().getProtocolVersion()),
                        new PlayerInfo(0, 20, new ArrayList<>()),
                        Component.text("A Minecraft Server"),
                        null,
                        false
                    );
                }

                ServerStatusInfo info = builder.buildInfo(session);
                if (info == null) session.disconnect("bye");
                else session.send(new ClientboundStatusResponsePacket(info));
            } else if (packet instanceof ServerboundPingRequestPacket p) {
                session.send(new ClientboundPongResponsePacket(p.getPingTime()));
            }
            return false;
        } else if (protocol.getState() == ProtocolState.GAME) {
            if (packet instanceof ServerboundKeepAlivePacket p) {
                if (p.getPingId() == this.lastPingId) {
                    long time = System.currentTimeMillis() - this.lastPingTime;
                    session.setFlag(MinecraftConstants.PING_KEY, time);
                }
            } else if (packet instanceof ServerboundConfigurationAcknowledgedPacket) {
                protocol.setState(ProtocolState.CONFIGURATION);
            } else if (packet instanceof ServerboundPingRequestPacket) {
                session.send(new ClientboundPongResponsePacket(((ServerboundPingRequestPacket) packet).getPingTime()));
            }
        } else if (protocol.getState() == ProtocolState.CONFIGURATION) {
            if (packet instanceof ServerboundFinishConfigurationPacket) {
                protocol.setState(ProtocolState.GAME);
                ServerLoginHandler handler = session.getFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY);
                if (handler != null) {
                    handler.loggedIn(session);
                }

                if (session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
                    new Thread(new KeepAliveTask(session)).start();
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public Packet packetSending(final Session session, final Packet packet) {
        try {
            Packet p = isSpectator()
                ? SERVER_SPECTATOR_HANDLERS.handleOutgoing(packet, this)
                : SERVER_PLAYER_HANDLERS.handleOutgoing(packet, this);
            if (p != null && CONFIG.client.extra.actionLimiter.enabled) {
                p = MODULE_MANAGER.get(ActionLimiter.class).getHandlerRegistry().handleOutgoing(p, this);
            }
            return p;
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling packet sending: " + packet.getClass().getSimpleName(), e);
        }
        return packet;
    }


    @Override
    public void packetSent(Session session, Packet packet) {
        if (packet instanceof ClientboundLoginCompressionPacket) {
            session.setCompressionThreshold(((ClientboundLoginCompressionPacket) packet).getThreshold(), true);
            session.send(new ClientboundGameProfilePacket(session.getFlag(MinecraftConstants.PROFILE_KEY)));
        }
        try {
            if (isSpectator())
                SERVER_SPECTATOR_HANDLERS.handlePostOutgoing(packet, this);
            else
                SERVER_PLAYER_HANDLERS.handlePostOutgoing(packet, this);
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling PostOutgoing packet: " + packet.getClass().getSimpleName(), e);
        }
    }

    @Override
    public boolean packetError(final Session session, final Throwable throwable) {
        if (isLoggedIn) {
            SERVER_LOG.debug("", throwable);
            return true;
        } else {
            SERVER_LOG.error("", throwable);
        }
        return false;
    }

    @Override
    public void connected(final Session session) {
        session.setFlag(MinecraftConstants.PING_KEY, 0);
    }

    @Override
    public void disconnecting(final Session session, final Component reason, final Throwable cause) {
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (protocol.getState() == ProtocolState.LOGIN) {
            session.send(new ClientboundLoginDisconnectPacket(reason));
        } else if (protocol.getState() == ProtocolState.GAME) {
            session.send(new ClientboundDisconnectPacket(reason));
        }
    }

    @Override
    public void disconnected(final Session session, final Component reason, final Throwable cause) {
        Proxy.getInstance().getActiveConnections().remove(this);
        if (!this.isPlayer && cause != null && !(cause instanceof IOException)) {
            // any scanners or TCP connections established result in a lot of these coming in even when they are not actually speaking mc protocol
            SERVER_LOG.warn(String.format("Connection disconnected: %s", session.getRemoteAddress()), cause);
            return;
        }
        if (this.isPlayer) {
            final String reasonStr = ComponentSerializer.toRawString(reason);

            if (!isSpectator()) {
                SERVER_LOG.info("Player disconnected: UUID: {}, Username: {}, Address: {}, Reason {}",
                                Optional.ofNullable(this.profileCache.getProfile()).map(GameProfile::getId).orElse(null),
                                Optional.ofNullable(this.profileCache.getProfile()).map(GameProfile::getName).orElse(null),
                                session.getRemoteAddress(),
                                reasonStr,
                                cause);
                try {
                    EVENT_BUS.post(new ProxyClientDisconnectedEvent(reasonStr, profileCache.getProfile()));
                } catch (final Throwable e) {
                    SERVER_LOG.info("Could not get game profile of disconnecting player");
                    EVENT_BUS.post(new ProxyClientDisconnectedEvent(reasonStr));
                }
            } else {
                Proxy.getInstance().getActiveConnections().forEach(connection -> {
                    connection.send(new ClientboundRemoveEntitiesPacket(new int[]{this.spectatorEntityId}));
                    connection.send(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&9" + profileCache.getProfile().getName() + " disconnected&r"), false));
                });
                EVENT_BUS.postAsync(new ProxySpectatorDisconnectedEvent(profileCache.getProfile()));
            }
        }
        ServerConnection serverConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (serverConnection != null) {
            serverConnection.syncTeamMembers();
        }
    }

    public void setLoggedIn() {
        this.isLoggedIn = true;
        Proxy.getInstance().getActiveConnections().add(this);
    }

    public void send(@NonNull Packet packet) {
        this.session.send(packet);
    }

    public void sendDirect(Packet packet) {
        this.session.sendDirect(packet);
    }

    @Override
    public void sendDelayedDirect(@NonNull final Packet packet) {
        this.session.sendDelayedDirect(packet);
    }

    @Override
    public void flush() {
        this.session.flush();
    }

    @Override
    public void sendBundleDirect(@NotNull final @NonNull Packet... packets) {
        this.session.sendBundleDirect(packets);
    }

    @Override
    public void sendBundleDirect(@NonNull final List<Packet> packets) {
        this.session.sendBundleDirect(packets);
    }

    @Override
    public void sendBundle(@NonNull final List<Packet> packets) {
        this.session.sendBundle(packets);
    }

    @Override
    public void sendBundle(@NotNull final @NonNull Packet... packets) {
        this.session.sendBundle(packets);
    }

    @Override
    public void sendAsync(@NonNull final Packet packet) {
        this.session.sendAsync(packet);
    }

    @Override
    public void sendScheduledAsync(@NonNull final Packet packet, final long delay, final TimeUnit unit) {
        this.session.sendScheduledAsync(packet, delay, unit);
    }

    public boolean isActivePlayer() {
        // note: this could be false for the player connection during some points of disconnect
        return Objects.equals(Proxy.getInstance().getCurrentPlayer().get(), this);
    }

    // Spectator helper methods

    public Packet getEntitySpawnPacket() {
        return spectatorEntity.getSpawnPacket(spectatorEntityId, spectatorEntityUUID, spectatorPlayerCache, spectatorFakeProfileCache.getProfile());
    }

    public ClientboundSetEntityDataPacket getSelfEntityMetadataPacket() {
        return new ClientboundSetEntityDataPacket(spectatorEntityId, spectatorEntity.getSelfEntityMetadata(spectatorFakeProfileCache.getProfile(), spectatorEntityId));
    }

    public ClientboundSetEntityDataPacket getEntityMetadataPacket() {
        return new ClientboundSetEntityDataPacket(spectatorEntityId, spectatorEntity.getEntityMetadata(spectatorFakeProfileCache.getProfile(), spectatorEntityId));
    }

    public Optional<Packet> getSoundPacket() {
        return spectatorEntity.getSoundPacket(spectatorPlayerCache);
    }

    public boolean hasCameraTarget() {
        return cameraTarget != null;
    }

    public void initSpectatorEntity() {
        this.spectatorEntity = SpectatorEntityRegistry.getSpectatorEntityWithDefault(CONFIG.server.spectator.spectatorEntity);
    }

    // todo: might rework this to handle respawns in some central place
    public boolean setSpectatorEntity(final String identifier) {
        Optional<SpectatorEntity> entity = SpectatorEntityRegistry.getSpectatorEntity(identifier);
        if (entity.isPresent()) {
            this.spectatorEntity = entity.get();
            return true;
        } else {
            return false;
        }
    }

    public void initializeTeam() {
        session.send(new ClientboundSetPlayerTeamPacket(
            teamName,
            displayName,
            prefix,
            suffix,
            friendlyFire,
            seeFriendlyInvisibles,
            NameTagVisibility.HIDE_FOR_OTHER_TEAMS,
            CollisionRule.PUSH_OTHER_TEAMS,
            TeamColor.AQUA,
            currentTeamMembers.toArray(new String[0])
        ));
    }

    public synchronized void syncTeamMembers() {
        final List<String> teamMembers = Proxy.getInstance().getSpectatorConnections()
            .map(ServerConnection::getSpectatorEntityUUID)
            .map(UUID::toString)
            .collect(Collectors.toCollection(ArrayList::new));
        if (!teamMembers.isEmpty()) teamMembers.add(profileCache.getProfile().getName());
        final List<String> toRemove = currentTeamMembers.stream()
            .filter(member -> !teamMembers.contains(member))
            .toList();
        final List<String> toAdd = teamMembers.stream()
            .filter(member -> !currentTeamMembers.contains(member))
            .toList();
        if (!toRemove.isEmpty()) {
            sendAsync(new ClientboundSetPlayerTeamPacket(
                teamName,
                TeamAction.REMOVE_PLAYER,
                toRemove.toArray(new String[0])
            ));
        }
        if (!toAdd.isEmpty()) {
            sendAsync(new ClientboundSetPlayerTeamPacket(
                teamName,
                TeamAction.ADD_PLAYER,
                toAdd.toArray(new String[0])
            ));
        }
        this.currentTeamMembers = teamMembers;
        SERVER_LOG.debug("Synced Team members: {} for {}", currentTeamMembers, this.profileCache.getProfile().getName());
    }

    //
    //
    //
    // SESSION METHOD IMPLEMENTATIONS
    //
    //
    //

    @Override
    public void connect() {
        this.session.connect();
    }

    @Override
    public void connect(boolean wait) {
        this.session.connect(wait);
    }

    @Override
    public String getHost() {
        return this.session.getHost();
    }

    @Override
    public int getPort() {
        return this.session.getPort();
    }

    @Override
    public SocketAddress getLocalAddress() {
        return this.session.getLocalAddress();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return this.session.getRemoteAddress();
    }

    @Override
    public PacketProtocol getPacketProtocol() {
        return this.session.getPacketProtocol();
    }

    @Override
    public PacketCodecHelper getCodecHelper() {
        return session.getCodecHelper();
    }

    @Override
    public Map<String, Object> getFlags() {
        return this.session.getFlags();
    }

    @Override
    public boolean hasFlag(String key) {
        return this.session.hasFlag(key);
    }

    @Override
    public <T> T getFlag(String key) {
        return this.session.getFlag(key);
    }

    @Override
    public <T> T getFlag(String key, T def) {
        return this.session.getFlag(key, def);
    }

    @Override
    public void setFlag(String key, Object value) {
        this.session.setFlag(key, value);
    }

    @Override
    public List<SessionListener> getListeners() {
        return this.session.getListeners();
    }

    @Override
    public void addListener(SessionListener listener) {
        this.session.addListener(listener);
    }

    @Override
    public void removeListener(SessionListener listener) {
        this.session.removeListener(listener);
    }

    @Override
    public void callPacketReceived(Packet packet) {
        this.session.callPacketReceived(packet);
    }

    @Override
    public Packet callPacketSending(final Packet packet) {
        return this.session.callPacketSending(packet);
    }

    @Override
    public void callConnected() {
        this.session.callConnected();
    }

    @Override
    public void callDisconnecting(final Component reason, final Throwable cause) {
        this.session.callDisconnecting(reason, cause);
    }

    @Override
    public void callDisconnected(final Component reason, final Throwable cause) {
        this.session.callDisconnected(reason, cause);
    }

    @Override
    public void callPacketSent(Packet packet) {
        this.session.callPacketSent(packet);
    }

    @Override
    public boolean callPacketError(final Throwable throwable) {
        return this.session.callPacketError(throwable);
    }

    @Override
    public int getCompressionThreshold() {
        return this.session.getCompressionThreshold();
    }

    @Override
    public void setCompressionThreshold(int threshold, boolean validateCompression) {
        this.session.setCompressionThreshold(threshold, validateCompression);
    }

    @Override
    public void enableEncryption(SecretKey key) {
        this.session.enableEncryption(key);
    }

    @Override
    public int getConnectTimeout() {
        return this.session.getConnectTimeout();
    }

    @Override
    public void setConnectTimeout(int timeout) {
        this.session.setConnectTimeout(timeout);
    }

    @Override
    public int getReadTimeout() {
        return this.session.getReadTimeout();
    }

    @Override
    public void setReadTimeout(int timeout) {
        this.session.setReadTimeout(timeout);
    }

    @Override
    public int getWriteTimeout() {
        return this.session.getWriteTimeout();
    }

    @Override
    public void setWriteTimeout(int timeout) {
        this.session.setWriteTimeout(timeout);
    }

    @Override
    public boolean isConnected() {
        return this.session.isConnected();
    }

    @Override
    public void disconnect(String reason) {
        this.session.disconnect(reason);
    }

    @Override
    public void disconnect(String reason, Throwable cause) {
        this.session.disconnect(reason, cause);
    }

    @Override
    public void disconnect(@Nullable final Component reason) {
        this.session.disconnect(reason);
    }

    @Override
    public void disconnect(@Nullable final Component reason, final Throwable cause) {
        this.session.disconnect(reason, cause);
    }

    private class UserAuthTask implements Runnable {
        private Session session;
        private SecretKey key;

        public UserAuthTask(Session session, SecretKey key) {
            this.key = key;
            this.session = session;
        }

        @Override
        public void run() {
            GameProfile profile;
            if (this.key != null) {
                SessionService sessionService = this.session.getFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
                try {
                    profile = sessionService.getProfileByServer(username, sessionService.getServerId(SERVER_ID, KEY_PAIR.getPublic(), this.key));
                } catch (RequestException e) {
                    this.session.disconnect("Failed to make session service request.", e);
                    return;
                }

                if (profile == null) {
                    this.session.disconnect("Failed to verify username.");
                }
            } else {
                profile = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()), username);
            }

            this.session.setFlag(MinecraftConstants.PROFILE_KEY, profile);

            int threshold = session.getFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, DEFAULT_COMPRESSION_THRESHOLD);
            this.session.send(new ClientboundLoginCompressionPacket(threshold));
        }
    }

    private class KeepAliveTask implements Runnable {
        private Session session;

        public KeepAliveTask(Session session) {
            this.session = session;
        }

        @Override
        public void run() {
            while (this.session.isConnected()) {
                lastPingTime = System.currentTimeMillis();
                lastPingId = (int) lastPingTime;
                this.session.send(new ClientboundKeepAlivePacket(lastPingId));

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
