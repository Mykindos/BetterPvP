package me.mykindos.betterpvp.champions.champions.builds.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.event.LoadBuildsEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

@BPvPListener
public class BuildListener implements Listener {

    @Inject
    private Champions champions;

    @Inject
    private BuildManager buildManager;

    @Inject
    private SkillManager skillManager;

    @Inject
    private ClassSelectionMenu classSelectionMenu;

    @EventHandler
    public void onClientJoin(ClientLoginEvent event) {
        UtilServer.runTaskAsync(champions, () -> {
            GamerBuilds builds = new GamerBuilds(event.getClient().getUuid());
            buildManager.getBuildRepository().loadBuilds(builds);
            buildManager.getBuildRepository().loadDefaultBuilds(builds);
            buildManager.addObject(event.getClient().getUuid(), builds);
            UtilServer.runTask(champions, () -> {
                UtilServer.callEvent(new LoadBuildsEvent(event.getPlayer(), builds));
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        buildManager.removeObject(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onOpenBuildManager(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;
            if (block.getType() == Material.ENCHANTING_TABLE) {
                Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(event.getPlayer().getUniqueId());
                gamerBuildsOptional.ifPresent(builds -> {
                    classSelectionMenu.show(event.getPlayer());
                    event.setCancelled(true);
                });
            }
        }
    }

    @EventHandler
    public void onDeleteBuild(DeleteBuildEvent event) {
        Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(event.getPlayer().getUniqueId());
        if (gamerBuildsOptional.isPresent()) {
            buildManager.getBuildRepository().update(event.getRoleBuild());
        }
    }

    @EventHandler
    public void onApplyBuild(ApplyBuildEvent event) {
        Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(event.getPlayer().getUniqueId());
        if (gamerBuildsOptional.isPresent()) {
            buildManager.getBuildRepository().update(event.getNewBuild());
            buildManager.getBuildRepository().update(event.getOldBuild());
        }
    }

}
