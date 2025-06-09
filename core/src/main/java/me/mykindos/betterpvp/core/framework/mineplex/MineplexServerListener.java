package me.mykindos.betterpvp.core.framework.mineplex;

import com.google.inject.Singleton;
import com.mineplex.studio.sdk.util.NamespaceUtil;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.bukkit.event.Listener;

@Singleton
@PluginAdapter("StudioEngine")
public class MineplexServerListener implements Listener {

    public MineplexServerListener() {
        String serverName = NamespaceUtil.getCommonName();
        if (serverName == null) return;
        if (serverName.toLowerCase().startsWith("champions")) {
            serverName = "Champions";
        }

        Core.setCurrentServer(serverName);
    }
}
