package me.mykindos.betterpvp.game.framework.model.setting.hotbar;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.game.framework.manager.RoleSelectorManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Manages the layout of the hotbar for players
 */
@Singleton
@CustomLog
public class HotBarLayoutManager {

    private final Map<Player, Multimap<Role, HotBarLayout>> hotBarLayouts = new WeakHashMap<>();
    private final Database database;
    private final BuildManager buildManager;
    private final RoleSelectorManager roleSelectorManager;
    private final ItemHandler itemHandler;

    @Config(path = "hotbar-layout-tokens", defaultValue = "12")
    @Inject
    private int hotBarLayoutTokens;

    @Inject
    public HotBarLayoutManager(Database database, RoleSelectorManager roleSelectorManager, ItemHandler itemHandler) {
        this.database = database;
        this.roleSelectorManager = roleSelectorManager;
        this.itemHandler = itemHandler;
        this.buildManager = JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(BuildManager.class);
    }

    /**
     * Generates a default {@link HotBarLayout} for the given {@link RoleBuild}
     * @param build the {@link RoleBuild} to generate the {@link HotBarLayout} for
     * @param maxTokens the maximum amount of tokens this {@link HotBarLayout} can have
     * @return the default {@link HotBarLayout}
     */
    private static HotBarLayout getDefaultHotbarLayout(RoleBuild build, int maxTokens) {
        HotBarLayout layout = new HotBarLayout(build, maxTokens);
        int slots = 0;
        layout.setSlot(slots++, HotBarItem.STANDARD_SWORD); // 3
        layout.setSlot(slots++, HotBarItem.STANDARD_AXE); // 3
        if (build.getRole() == Role.ASSASSIN || build.getRole() == Role.RANGER) {
            layout.setSlot(slots++, HotBarItem.BOW); // 1
            layout.setSlot(slots++, HotBarItem.ARROWS); // 1
        } else {
            layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 2
        }
        layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 2
        layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 2
        return layout;
    }

    public HotBarLayout getLayout(Player player, RoleBuild build) {
        return hotBarLayouts.computeIfAbsent(player, p -> generateMap())
                .get(build.getRole())
                .stream()
                .filter(layout -> layout.getBuild().getId() == build.getId())
                .findAny()
                .orElseGet(() -> {
                    HotBarLayout layout = getDefaultHotbarLayout(build, hotBarLayoutTokens);
                    hotBarLayouts.get(player).put(build.getRole(), layout);
                    return layout;
                });
    }

    /**
     * Resets the specified {@link RoleBuild}'s {@link HotBarLayout} to the {@link HotBarLayoutManager#getDefaultHotbarLayout(RoleBuild, int) default}
     * @param player the {@link Player}
     * @param build the {@link RoleBuild} to reset the {@link HotBarLayout}
     */
    public void resetLayout(Player player, RoleBuild build) {
        HotBarLayout previousLayout = getLayout(player, build);
        HotBarLayout defaultLayout = getDefaultHotbarLayout(build, hotBarLayoutTokens);
        if (!previousLayout.equals(defaultLayout)) {
            previousLayout.copy(defaultLayout);
            saveLayout(player, defaultLayout);
        }
    }

    private Multimap<Role, HotBarLayout> generateMap() {
        return MultimapBuilder.SetMultimapBuilder.hashKeys().hashSetValues().build();
    }

    public void load(Player player) {
        final Statement statement = new Statement("SELECT * FROM champions_hotbar_layouts WHERE Gamer = ?",
                new UuidStatementValue(player.getUniqueId()));

        try (final CachedRowSet result = database.executeQuery(statement, TargetDatabase.GLOBAL).join()) {
            // Group layouts by role and build ID
            Map<Role, Map<Integer, HotBarLayout>> layoutMap = new HashMap<>();

            while (result.next()) {
                Role role = Role.valueOf(result.getString("Role"));
                int id = result.getInt("ID");
                int slot = result.getInt("Slot");
                String itemName = result.getString("Item");

                // Check if the build exists before creating a layout for it
                Optional<RoleBuild> buildOptional = buildManager.getObject(player.getUniqueId())
                        .flatMap(playerBuilds -> playerBuilds.getBuild(role, id));
                
                if (buildOptional.isEmpty()) {
                    continue; // Skip this build if it doesn't exist, AKA the player has deleted the build
                }
                
                final RoleBuild build = buildOptional.get();

                // Get or create the layout for this role and ID
                HotBarLayout layout = layoutMap
                        .computeIfAbsent(role, k -> new HashMap<>())
                        .computeIfAbsent(id, buildId -> new HotBarLayout(build, hotBarLayoutTokens));

                // Set the item in the slot
                if (itemName != null) {
                    HotBarItem item = HotBarItem.valueOf(itemName);
                    layout.setSlot(slot, item);
                }
            }

            // Convert to the required multimap format
            Multimap<Role, HotBarLayout> layouts = generateMap();
            layoutMap.forEach((role, idMap) -> {
                idMap.values().forEach(layout -> layouts.put(role, layout));
            });

            hotBarLayouts.put(player, layouts);
        } catch (SQLException e) {
            log.error("Failed to load hotbar layout for " + player.getName(), e).submit();
        }
    }

    public void saveLayout(Player player, HotBarLayout layout) {
        // Delete old data
        List<Statement> transactionStatements = new ArrayList<>();
        final Statement deleteStatement = Statement.builder()
                        .delete("champions_hotbar_layouts")
                        .where("Gamer", "=", new UuidStatementValue(player.getUniqueId()))
                        .where("Role", "=", new StringStatementValue(layout.getBuild().getRole().name()))
                        .where("ID", "=", new IntegerStatementValue(layout.getBuild().getId()))
                        .build();
        transactionStatements.add(deleteStatement);

        // Insert new data
        for (Map.Entry<Integer, HotBarItem> entry : layout.getLayout().entrySet()) {
            final Statement insertStatement = Statement.builder()
                    .insertInto("champions_hotbar_layouts")
                    .values(new StringStatementValue(player.getUniqueId().toString()),
                            new StringStatementValue(layout.getBuild().getRole().name()),
                            new IntegerStatementValue(layout.getBuild().getId()),
                            new IntegerStatementValue(entry.getKey()),
                            new StringStatementValue(entry.getValue().name()))
                    .build();
            transactionStatements.add(insertStatement);
        }
        database.executeTransaction(transactionStatements, TargetDatabase.GLOBAL);
    }

    /**
     * Applies the player's saved hotbar layout. This method deletes
     * the player's current inventory and replaces it with the saved layout.
     *
     * @param player The player to apply the layout to
     */
    public void applyPlayerLayout(Player player) {
        // Get the player's current role and build
        final RoleBuild build = Objects.requireNonNull(buildManager.getObject(player.getUniqueId())
                .orElseThrow()
                .getActiveBuilds()
                .get(roleSelectorManager.getRole(player).getName()), "Player does not have an active build");

        // Get the player's layout for this build
        final HotBarLayout layout = getLayout(player, build);

        // Apply the layout to the player's inventory
        for (int i = 0; i < 9; i++) {
            HotBarItem item = layout.getLayout().get(i);
            if (item == null) {
                player.getInventory().clear(i);
                continue;
            }

            final BPvPItem bPvPItem = itemHandler.getItem(item.getNamespacedKey());
            final ItemStack itemStack = itemHandler.updateNames(bPvPItem.getItemStack(item.getAmount()));
            player.getInventory().setItem(i, itemStack);
        }

        player.updateInventory();
    }
}
