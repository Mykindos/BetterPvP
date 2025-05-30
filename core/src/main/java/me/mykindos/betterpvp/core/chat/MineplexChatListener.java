package me.mykindos.betterpvp.core.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.ignore.PlayerIgnoreModule;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientIgnoreStatusEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@PluginAdapter("StudioEngine")
@Singleton
@BPvPListener
public class MineplexChatListener implements Listener {

    private final Core core;
    private final ClientManager clientManager;
    private final PlayerIgnoreModule playerIgnoreModule;

    @Inject
    public MineplexChatListener(Core core, ClientManager clientManager) {
        this.core = core;
        this.clientManager = clientManager;
        playerIgnoreModule = MineplexModuleManager.getRegisteredModule(PlayerIgnoreModule.class);
    }

    @EventHandler
    public void onIgnoreCheck(ClientIgnoreStatusEvent event) {
        Client client = event.getClient();
        Client target = event.getTarget();
        if(target.hasRank(Rank.HELPER)) {
            return;
        }

        event.setResult(playerIgnoreModule.isIgnored(client.getUniqueId(), target.getUniqueId()) ? ClientIgnoreStatusEvent.Result.DENY : ClientIgnoreStatusEvent.Result.ALLOW);
    }

}
