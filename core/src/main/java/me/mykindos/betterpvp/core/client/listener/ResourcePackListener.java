package me.mykindos.betterpvp.core.client.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
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

    private final Core core;

    @Inject
    public ResourcePackListener(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onClientLogin(ClientLoginEvent event) {

        UtilServer.runTaskLater(core, () -> {
            event.getPlayer().setResourcePack(resourcePackUrl, resourcePackSha);
        }, 2L);


    }

    @EventHandler
    public void onTexturepackStatus(PlayerResourcePackStatusEvent e) {
        if (forceResourcePack) {
            if (e.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {
                e.getPlayer().kick(Component.text("You must allow the resource pack"));
            } else if (e.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
                e.getPlayer().kick(Component.text("Resource pack failed to load?"));
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
