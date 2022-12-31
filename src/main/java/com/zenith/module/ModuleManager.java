package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.zenith.util.Constants.EVENT_BUS;
import static com.zenith.util.Constants.MODULE_EXECUTOR_SERVICE;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ModuleManager {

    protected ScheduledExecutorService clientTickExecutorService;
    protected List<Module> modules;

    public ModuleManager() {
        EVENT_BUS.subscribe(this);
        this.modules = asList(
                new AntiAFK(),
                new AutoDisconnect(),
                new AutoReply(),
                new Spook(),
                new AutoRespawn()
        );
    }

    public <T> Optional<T> getModule(final Class<T> clazz) {
        return this.modules.stream()
                .filter(module -> module.getClass().equals(clazz))
                .map(module -> (T) module)
                .findFirst();
    }

    @Subscribe
    public void handlePlayerOnlineEvent(final PlayerOnlineEvent event) {
        startClientTicks();
    }

    @Subscribe
    public void handleProxyClientConnectedEvent(final ProxyClientConnectedEvent event) {
        stopClientTicks();
    }

    @Subscribe
    public void handleProxyClientDisconnectedEvent(final ProxyClientDisconnectedEvent event) {
        if (nonNull(Proxy.getInstance().getClient()) && Proxy.getInstance().getClient().isOnline()) {
            startClientTicks();
        }
    }

    @Subscribe
    public void handleDisconnectEvent(final DisconnectEvent event) {
        stopClientTicks();
    }

    public synchronized void startClientTicks() {
        if (isNull(clientTickExecutorService) || clientTickExecutorService.isShutdown()) {
            this.modules.forEach(Module::clientTickStarting);
            this.clientTickExecutorService = new ScheduledThreadPoolExecutor(1);
            this.clientTickExecutorService.scheduleAtFixedRate(() -> {
                MODULE_EXECUTOR_SERVICE.execute(() -> EVENT_BUS.dispatch(new ClientTickEvent()));
            }, 0, 50L, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stopClientTicks() {
        if (nonNull(this.clientTickExecutorService)) {
            this.clientTickExecutorService.shutdown();
            this.clientTickExecutorService = null;
            this.modules.forEach(Module::clientTickStopping);
        }
    }
}