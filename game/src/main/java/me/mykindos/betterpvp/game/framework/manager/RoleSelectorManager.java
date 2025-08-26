package me.mykindos.betterpvp.game.framework.manager;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PerspectiveRegion;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.npc.KitSelector;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.gui.hotbar.ButtonBuildMenuHotbar;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps track of selected kits
 */
@CustomLog
@Singleton
public class RoleSelectorManager {

    @Getter
    private final Map<UUID, Role> selectedRoles = new HashMap<>();
    @Getter
    private final Multimap<MappedWorld, KitSelector> kitSelectors = ArrayListMultimap.create();
    private final ChampionsSkillManager skillManager;
    private final BuildManager buildManager;

    @Inject
    public RoleSelectorManager() {
        this.buildManager = JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(BuildManager.class);
        this.skillManager = JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(ChampionsSkillManager.class);
    }

    public void selectRole(Player player, Role role) {
        if (role == null) {
            selectedRoles.remove(player.getUniqueId());
        } else {
            selectedRoles.put(player.getUniqueId(), role);
        }
    }

    public Role getRole(Player player) {
        return selectedRoles.getOrDefault(player.getUniqueId(), Role.KNIGHT);
    }

    /**
     * Creates and spawns role selectors in the map
     * @param map the map
     * @return a list of spawned role selectors
     */
    public List<KitSelector> createKitSelectors(MappedWorld map, InventoryProvider inventoryProvider, HotBarLayoutManager hotBarLayoutManager, ItemHandler itemHandler) {
        // Clear previous selectors
        clearSelectors(map);
        List<KitSelector> selectors = new ArrayList<>();

        // Spawn selector for each role
        for (Role role : Role.values()) {
            // Try different naming patterns for finding the region
            List<PerspectiveRegion> regions = findKitSelectorRegion(map, role);

            if (regions.isEmpty()) {
                log.warn("No region found for kit selector {}", role.name()).submit();
                continue;
            }

            for (PerspectiveRegion region : regions) {
                Location location = region.getLocation();
                // Create the selector, make sure they can edit their hotbar
                KitSelector selector = new KitSelector(role, true, true);
                selector.setBuildMenuFunction(player -> {
                    final GamerBuilds builds = buildManager.getObject(player.getUniqueId()).orElseThrow();
                    return new BuildMenu(builds, role, buildManager, skillManager, null, (buildId, menu) -> {
                        return new ButtonBuildMenuHotbar(inventoryProvider, hotBarLayoutManager, itemHandler, role, builds, buildId);
                    }, null);
                });

                // Spawn it and log it
                selector.spawn(location);
                selectors.add(selector);
                log.info("Spawned selector for kit {} at {}", role.name(), location).submit();
            }
        }

        kitSelectors.putAll(map, selectors);

        return selectors;
    }

    private List<PerspectiveRegion> findKitSelectorRegion(MappedWorld world, Role role) {
        String regionName = "kit_selector_" + role.getName().toLowerCase();
        return world.findRegion(regionName, PerspectiveRegion.class).toList();
    }

    /**
     * Clears all spawned selectors
     */
    public void clearSelectors(MappedWorld world) {
        for (KitSelector selector : kitSelectors.removeAll(world)) {
            selector.remove();
        }
    }

}
