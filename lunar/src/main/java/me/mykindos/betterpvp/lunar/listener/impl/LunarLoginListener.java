package me.mykindos.betterpvp.lunar.listener.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.event.ApolloListener;
import com.lunarclient.apollo.event.Listen;
import com.lunarclient.apollo.event.player.ApolloRegisterPlayerEvent;
import com.lunarclient.apollo.event.player.ApolloUnregisterPlayerEvent;
import com.lunarclient.apollo.module.notification.Notification;
import com.lunarclient.apollo.module.notification.NotificationModule;
import com.lunarclient.apollo.module.staffmod.StaffModModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.events.lunar.LunarClientEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.lunar.Lunar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Objects;

@Singleton
public class LunarLoginListener implements ApolloListener {

    @Inject
    private ClientManager clientManager;

    @Listen
    public void onApolloRegister(ApolloRegisterPlayerEvent event) {
        final ApolloPlayer apolloPlayer = event.getPlayer();
        final Player player = Objects.requireNonNull(Bukkit.getPlayer(apolloPlayer.getUniqueId()));
        UtilServer.callEvent(new LunarClientEvent(player, true));

        // Notify them that they are using Lunar Client
        final Notification notification = Notification.builder()
                .titleComponent(Component.text("Lunar Client Support", NamedTextColor.GREEN))
                .descriptionComponent(Component.text("You are using Lunar Client! Enhanced features are now available."))
                .displayTime(Duration.ofSeconds(10L))
                .build();

        UtilServer.runTaskLater(JavaPlugin.getPlugin(Lunar.class), () -> {
            final NotificationModule module = Apollo.getModuleManager().getModule(NotificationModule.class);
            module.displayNotification(apolloPlayer, notification);

            // Enable staff module if admin
            final Client client = clientManager.search().online(player);
            if (client.hasRank(Rank.ADMIN)) {
                Apollo.getModuleManager().getModule(StaffModModule.class).enableAllStaffMods(apolloPlayer);
            }
        }, 1);
    }

    @Listen
    public void onApolloUnregister(ApolloUnregisterPlayerEvent event) {
        final Player player = Bukkit.getPlayer(event.getPlayer().getUniqueId());
        UtilServer.callEvent(new LunarClientEvent(player, false));
    }
}
