package me.mykindos.betterpvp.core.client.punishments;

import com.google.inject.Inject;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.moderation.ModerationModule;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

@PluginAdapter("StudioEngine")
@BPvPListener
public class MineplexPunishmentListener implements Listener {

    @Inject
    public MineplexPunishmentListener() {
        ModerationModule moderationModule = MineplexModuleManager.getRegisteredModule(ModerationModule.class);
        moderationModule.disableDefaultPunishmentCommand();
    }
}
