package me.mykindos.betterpvp.clans.champions.builds.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.clans.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.clans.champions.skills.SkillManager;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.menu.events.MenuCloseEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

@BPvPListener
public class BuildListener implements Listener {

    private final GamerManager gamerManager;
    private final SkillManager skillManager;

    @Inject
    public BuildListener(GamerManager gamerManager, SkillManager skillManager) {
        this.gamerManager = gamerManager;
        this.skillManager = skillManager;
    }


    @EventHandler
    public void onOpenBuildManager(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            if (block == null) return;
            if (block.getType() == Material.ENCHANTING_TABLE) {
                Optional<Gamer> gamerOptional = gamerManager.getObject(e.getPlayer().getUniqueId());
                gamerOptional.ifPresent(gamer -> {
                    MenuManager.openMenu(e.getPlayer(), new ClassSelectionMenu(e.getPlayer(), gamer, skillManager));
                    e.setCancelled(true);
                });
            }
        }
    }

    @EventHandler
    public void onSkillUpdate(MenuCloseEvent event) {
        if (event.getMenu() instanceof SkillMenu skillMenu) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId());
            if (gamerOptional.isPresent()) {
                gamerManager.getBuildRepository().update(skillMenu.getRoleBuild());
            }
        }
    }

    @EventHandler
    public void onDeleteBuild(DeleteBuildEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId());
        if (gamerOptional.isPresent()) {
            gamerManager.getBuildRepository().update(event.getRoleBuild());
        }
    }

    @EventHandler
    public void onApplyBuild(ApplyBuildEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId());
        if (gamerOptional.isPresent()) {
            gamerManager.getBuildRepository().update(event.getNewBuild());
            gamerManager.getBuildRepository().update(event.getOldBuild());
        }
    }

}
