package me.mykindos.betterpvp.core.client.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

@BPvPListener
public class ResourcePackListener implements Listener {

    @Inject
    @Config(path = "resourcepack.url", defaultValue = "")
    private String resourcePackUrl;

    @Inject
    @Config(path = "resourcepack.sha", defaultValue = "")
    private String resourcePackSha;

    @Inject
    @Config(path = "resourcepack.force", defaultValue = "false")
    private boolean forceResourcePack;

    @EventHandler
    public void onClientLogin(ClientLoginEvent event) {

        event.getPlayer().setResourcePack(resourcePackUrl, resourcePackSha);

    }

    @EventHandler
    public void onTexturepackStatus(PlayerResourcePackStatusEvent e) {
        if (forceResourcePack) {
            if (e.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {

            } else if (e.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {

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
