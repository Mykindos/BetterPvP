package me.mykindos.betterpvp.champions.champions.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.WeakHashMap;

@BPvPListener
@Singleton
public class KitSelectorListener implements Listener {

    protected final WeakHashMap<Entity, KitSelector> selectors = new WeakHashMap<>();
    private final ItemHandler itemHandler;
    private final BuildManager buildManager;
    private final ChampionsSkillManager skillManager;

    @Inject
    private KitSelectorListener(ItemHandler itemHandler, BuildManager buildManager, ChampionsSkillManager skillManager) {
        this.itemHandler = itemHandler;
        this.buildManager = buildManager;
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!selectors.containsKey(event.getRightClicked())) {
            return;
        }

        event.setCancelled(true);

        // Equip
        final KitSelector selector = selectors.get(event.getRightClicked());
        final Role role = selector.getRole();
        final Player player = event.getPlayer();
        player.getInventory().clear();
        role.equip(itemHandler, player);
        player.getWorld().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        if (selector.isEditor()) {
            // Open editor
            final GamerBuilds builds = buildManager.getObject(player.getUniqueId()).orElseThrow();
            new BuildMenu(builds, role, this.buildManager, this.skillManager, null, null).show(player);
        }
    }

}