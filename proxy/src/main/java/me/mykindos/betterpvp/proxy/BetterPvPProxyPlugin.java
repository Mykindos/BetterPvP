package me.mykindos.betterpvp.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.mykindos.betterpvp.orchestration.transport.QueuePluginChannels;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = "betterpvp-proxy",
        name = "BetterPvP Proxy",
        version = "1.0.0",
        description = "Velocity admission integration for BetterPvP"
)
public class BetterPvPProxyPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public BetterPvPProxyPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            final ProxyConfig config = ProxyConfig.load(dataDirectory);
            logger.info("Loaded BetterPvP proxy config from {}", dataDirectory.resolve("config.toml"));
            proxyServer.getChannelRegistrar().register(MinecraftChannelIdentifier.from(QueuePluginChannels.QUEUE_REQUEST));
            final QueueAdmissionListener queueAdmissionListener = new QueueAdmissionListener(proxyServer, logger, config);
            proxyServer.getEventManager().register(this, queueAdmissionListener);
            queueAdmissionListener.startPolling(this, proxyServer);
            logger.info("BetterPvP proxy admission listener initialized");
        } catch (IOException ex) {
            logger.error("Failed to load BetterPvP proxy config", ex);
        }
    }
}
