package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

@BPvPListener
public class ResourcePackListener implements Listener {


    private final ResourcePackHandler resourcePackHandler;

    private final Core core;

    @Inject
    public ResourcePackListener(ResourcePackHandler resourcePackHandler, Core core) {
        this.resourcePackHandler = resourcePackHandler;
        this.core = core;
    }

    @EventHandler
    public void onClientLogin(ClientJoinEvent event) {

        if(resourcePackHandler.isResourcePackEnabled()) {
            UtilServer.runTaskLater(core, () -> {
                event.getPlayer().setResourcePack(resourcePackHandler.getResourcePackUrl(), resourcePackHandler.getResourcePackSha());
            }, 2L);
        }


    }

    @EventHandler
    public void onTexturepackStatus(PlayerResourcePackStatusEvent e) {
        if(!resourcePackHandler.isResourcePackEnabled()) return;

        if (resourcePackHandler.isForceResourcePack()) {
            if (e.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {
                e.getPlayer().kick(Component.text("You must allow the resource pack"));
            } else if (e.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
                e.getPlayer().kick(Component.text("Resource pack failed to load"));
            }
        }

        if (e.getStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            //System.out.println("Added Texture Pack immunity to " + e.getPlayer().getName());
            //EffectManager.addEffect(e.getPlayer(), EffectType.TEXTURELOADING, 15000);
        } else if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            //new BukkitRunnable() {
            //    @Override
            //    public void run() {
            //        System.out.println("Removed Texture Pack immunity from " + e.getPlayer().getName());
            //        EffectManager.removeEffect(e.getPlayer(), EffectType.TEXTURELOADING);
            //    }
            //}.runTaskLater(getInstance(), 10);

        }
    }
}
