package me.mykindos.betterpvp.game.framework.model.setting.hotbar;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.jooq.tables.records.ChampionsHotbarLayoutsRecord;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.game.framework.manager.RoleSelectorManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CHAMPIONS_HOTBAR_LAYOUTS;

/**
 * Manages the layout of the hotbar for players
 */
@Singleton
@CustomLog
public class HotBarLayoutManager {

    @Getter
    private final Map<UUID, Multimap<Role, HotBarLayout>> hotBarLayouts = new HashMap<>();
    private final Database database;
    private final ClientManager clientManager;
    private final BuildManager buildManager;
    private final RoleSelectorManager roleSelectorManager;
    private final ItemHandler itemHandler;

    @Config(path = "hotbar-layout-tokens", defaultValue = "10")
    @Inject
    private int hotBarLayoutTokens;

    @Inject
    public HotBarLayoutManager(Database database, ClientManager clientManager, RoleSelectorManager roleSelectorManager, ItemHandler itemHandler) {
        this.database = database;
        this.clientManager = clientManager;
        this.roleSelectorManager = roleSelectorManager;
        this.itemHandler = itemHandler;
        this.buildManager = JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(BuildManager.class);
    }

    /**
     * Generates a default {@link HotBarLayout} for the given {@link RoleBuild}
     *
     * @param build     the {@link RoleBuild} to generate the {@link HotBarLayout} for
     * @param maxTokens the maximum amount of tokens this {@link HotBarLayout} can have
     * @return the default {@link HotBarLayout}
     */
    public static HotBarLayout getDefaultHotbarLayout(RoleBuild build, int maxTokens) {
        HotBarLayout layout = new HotBarLayout(build, maxTokens);
        int slots = 0;
        layout.setSlot(slots++, HotBarItem.STANDARD_SWORD); // 2
        layout.setSlot(slots++, HotBarItem.STANDARD_AXE); // 2
        if (build.getRole() == Role.ASSASSIN || build.getRole() == Role.RANGER) {
            layout.setSlot(slots++, HotBarItem.BOW); // 1
            layout.setSlot(slots++, HotBarItem.ARROWS); // 1
        } else {
            layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 1
            layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 1
        }
        layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 1
        layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 1
        layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 1
        layout.setSlot(slots++, HotBarItem.MUSHROOM_STEW); // 1
        return layout;
    }

    public HotBarLayout getLayout(Player player, RoleBuild build) {
        return hotBarLayouts.computeIfAbsent(player.getUniqueId(), p -> generateMap())
                .get(build.getRole())
                .stream()
                .filter(layout -> layout.getBuild().getId() == build.getId())
                .findAny()
                .orElseGet(() -> {
                    HotBarLayout layout = getDefaultHotbarLayout(build, hotBarLayoutTokens);
                    hotBarLayouts.get(player.getUniqueId()).put(build.getRole(), layout);
                    return layout;
                });
    }

    /**
     * Resets the specified {@link RoleBuild}'s {@link HotBarLayout} to the {@link HotBarLayoutManager#getDefaultHotbarLayout(RoleBuild, int) default}
     *
     * @param player the {@link Player}
     * @param build  the {@link RoleBuild} to reset the {@link HotBarLayout}
     */
    public HotBarLayout resetLayout(Player player, RoleBuild build) {
        HotBarLayout previousLayout = getLayout(player, build);
        HotBarLayout defaultLayout = getDefaultHotbarLayout(build, hotBarLayoutTokens);
        if (!previousLayout.equals(defaultLayout)) {
            previousLayout.copy(defaultLayout);
            saveLayout(player, defaultLayout);
        }
        return defaultLayout;
    }

    private Multimap<Role, HotBarLayout> generateMap() {
        return MultimapBuilder.SetMultimapBuilder.hashKeys().hashSetValues().build();
    }

    public void load(Client client) {

        try {
            database.getAsyncDslContext().executeAsyncVoid(ctx -> {
                Result<ChampionsHotbarLayoutsRecord> results = ctx.selectFrom(CHAMPIONS_HOTBAR_LAYOUTS)
                        .where(CHAMPIONS_HOTBAR_LAYOUTS.CLIENT.eq(client.getId()))
                        .fetch();

                Map<Role, Map<Integer, HotBarLayout>> layoutMap = new HashMap<>();
                results.forEach(hotbarRecord -> {
                    Role role = Role.valueOf(hotbarRecord.getRole());
                    int id = hotbarRecord.getId();
                    int slot = hotbarRecord.getSlot();
                    String itemName = hotbarRecord.getItem();

                    // Check if the build exists before creating a layout for it
                    Optional<RoleBuild> buildOptional = buildManager.getObject(client.getUniqueId())
                            .flatMap(playerBuilds -> playerBuilds.getBuild(role, id));

                    if (buildOptional.isEmpty()) {
                        return; // Skip this build if it doesn't exist, AKA the client has deleted the build
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
                });

                // Convert to the required multimap format
                Multimap<Role, HotBarLayout> layouts = generateMap();
                layoutMap.forEach((role, idMap) -> {
                    idMap.values().forEach(layout -> layouts.put(role, layout));
                });

                hotBarLayouts.put(client.getUniqueId(), layouts);
            });


        } catch (DataAccessException e) {
            log.error("Failed to load hotbar layout for " + client.getName(), e).submit();
        }
    }

    public void saveLayout(Player player, HotBarLayout layout) {
        // Delete old data
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            Client client = clientManager.search().online(player);
            ctx.transaction(configuration -> {
                DSLContext ctxl = DSL.using(configuration);

                try {
                    ctxl.deleteFrom(CHAMPIONS_HOTBAR_LAYOUTS)
                            .where(CHAMPIONS_HOTBAR_LAYOUTS.CLIENT.eq(client.getId()))
                            .and(CHAMPIONS_HOTBAR_LAYOUTS.ROLE.eq(layout.getBuild().getRole().getName()))
                            .and(CHAMPIONS_HOTBAR_LAYOUTS.ID.eq(layout.getBuild().getId()))
                            .execute();


                    // Insert new data
                    List<Query> batchInserts = new ArrayList<>();
                    for (Map.Entry<Integer, HotBarItem> entry : layout.getLayout().entrySet()) {
                        batchInserts.add(
                                ctxl.insertInto(CHAMPIONS_HOTBAR_LAYOUTS)
                                        .set(CHAMPIONS_HOTBAR_LAYOUTS.CLIENT, client.getId())
                                        .set(CHAMPIONS_HOTBAR_LAYOUTS.ROLE, layout.getBuild().getRole().name())
                                        .set(CHAMPIONS_HOTBAR_LAYOUTS.ID, layout.getBuild().getId())
                                        .set(CHAMPIONS_HOTBAR_LAYOUTS.SLOT, entry.getKey())
                                        .set(CHAMPIONS_HOTBAR_LAYOUTS.ITEM, entry.getValue().name())
                        );
                    }

                    ctxl.batch(batchInserts).execute();
                } catch (DataAccessException ex) {
                    log.error("Failed to save layout for {}", client.getId(), ex).submit();
                }
            });
        });

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
        for (int i = 0; i < 36; i++) {
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

    /**
     * Return the item held in the slot
     *
     * @param player
     * @param slot
     * @return
     */
    @Nullable
    public ItemStack getPlayerHotBarLayoutSlot(Player player, int slot) {
        final RoleBuild build = Objects.requireNonNull(buildManager.getObject(player.getUniqueId())
                .orElseThrow()
                .getActiveBuilds()
                .get(roleSelectorManager.getRole(player).getName()), "Player does not have an active build");
        final HotBarLayout layout = getLayout(player, build);
        final HotBarItem hotBarItem = layout.getLayout().get(slot);
        if (hotBarItem == null) return null;
        final BPvPItem bPvPItem = itemHandler.getItem(hotBarItem.getNamespacedKey());
        return itemHandler.updateNames(bPvPItem.getItemStack(hotBarItem.getAmount()));
    }
}
