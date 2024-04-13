package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

@Singleton
@BPvPListener
public class ResourcePackListener implements Listener {

    private final ResourcePackHandler resourcePackHandler;

    private static final Title.Times TIME = Title.Times.times(Ticks.duration(0), Ticks.duration(10), Ticks.duration(0));
    private static final Title TITLE = Title.title(Component.text("Applying resource pack", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text("Please wait...", NamedTextColor.GRAY), TIME);


    @Inject
    public ResourcePackListener(ResourcePackHandler resourcePackHandler) {
        this.resourcePackHandler = resourcePackHandler;
    }

    @EventHandler
    public void onClientLogin(ClientJoinEvent event) {

        Player player = event.getPlayer();
        ResourcePack mainPack = resourcePackHandler.getResourcePack("main");
        if (mainPack == null) return;

        Component message = Component.text("You must accept the resource pack to play on this server", NamedTextColor.RED);
        player.setResourcePack(mainPack.getUuid(), mainPack.getUrl(), mainPack.getHashBytes(), message, true);

    }

    @EventHandler
    public void onTexturepackStatus(PlayerResourcePackStatusEvent event) {

        ResourcePack mainPack = resourcePackHandler.getResourcePack("main");
        if (mainPack == null) return;

        if (event.getID().equals(mainPack.getUuid())) {
            if (event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {
                event.getPlayer().kick(Component.text("You must allow the resource pack"));
            } else if (event.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
                event.getPlayer().kick(Component.text("Resource pack failed to load"));
            }
        }

    }

    @EventHandler
    public void onMoveWhileLoading(PlayerMoveEvent event) {
        if (event.getPlayer().getResourcePackStatus() != PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            event.setCancelled(true);
        }
    }



    @UpdateEvent(delay = 300)
    public void sendResourcePackTitle() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getResourcePackStatus() != PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {

                player.showTitle(TITLE);
            }
        }
    }
}
