package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.treasurechance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootSource;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerWoodcuttingTreasureDropEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.WoodcuttingTreasureChanceDropTableEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@BPvPListener
@Singleton
@CustomLog
public class WoodcuttingTreasureChanceListener implements Listener, Reloadable {

    private final WoodcuttingTreasureChanceAttribute treasureChanceAttribute;
    private final LootTableRegistry lootTableRegistry;
    private final LootSessionController sessionController;
    private final ItemFactory itemFactory;

    private LootTable treasureLootTable;

    @Inject
    public WoodcuttingTreasureChanceListener(WoodcuttingTreasureChanceAttribute treasureChanceAttribute,
                                             LootTableRegistry lootTableRegistry,
                                             LootSessionController sessionController,
                                             ItemFactory itemFactory) {
        this.treasureChanceAttribute = treasureChanceAttribute;
        this.lootTableRegistry = lootTableRegistry;
        this.sessionController = sessionController;
        this.itemFactory = itemFactory;
    }

    @Override
    public void reload() {
        this.treasureLootTable = lootTableRegistry.loadLootTable("woodcutting_treasure_chance");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChopLog(PlayerChopLogEvent event) {
        final Player player = event.getPlayer();
        final Location location = event.getChoppedLogBlock().getLocation();

        double chance = treasureChanceAttribute.getChance(player);
        WoodcuttingTreasureChanceDropTableEvent treasureCalculationEvent = UtilServer.callEvent(new WoodcuttingTreasureChanceDropTableEvent(player, location, chance));
        chance = treasureCalculationEvent.getTreasureChance();

        if (chance <= 0 || UtilMath.randDouble(0, 1) >= chance) {
            return;
        }

        LootBundle bundle = rollTreasure(player, location, treasureCalculationEvent.getLootTableId());
        PlayerWoodcuttingTreasureDropEvent treasureEvent = UtilServer.callEvent(new PlayerWoodcuttingTreasureDropEvent(player, location, bundle));
        if (treasureEvent.isCancelled()) {
            return;
        }

        bundle.award();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreasureLootAwarded(LootAwardedEvent event) {
        if (!"woodcutting:treasure".equals(event.getContext().getSource().getId())) return;
        final Audience audience = event.getContext().getSession().getAudience();
        if (!(audience instanceof Player player)) return;
        final Object award = event.getRawResult();

        // Deliver ItemStack awards into the player's inventory.
        // Messaging is handled by WoodcuttingDoubleTreasureChanceListener so it can append
        // "(doubled!)" when appropriate without sending two separate messages.
        if (award instanceof ItemStack itemStack) {
            ItemStack finalItemStack = itemFactory.convertItemStack(itemStack).orElse(itemStack);
            UtilItem.insert(player, finalItemStack);
        } else if (award instanceof List<?> itemStacks) {
            for (Object awardedItem : itemStacks) {
                if (!(awardedItem instanceof ItemStack itemStack)) continue;
                UtilItem.insert(player, itemStack);
            }
        }
    }

    private LootBundle rollTreasure(Player player, Location location, String lootTableId) {
        LootTable lootTable = lootTableRegistry.getLoaded().get(lootTableId);
        if (lootTable == null) {
            lootTable = treasureLootTable;
        }

        final LootTable finalLootTable = lootTable;
        final LootSession session = sessionController.resolve(player, finalLootTable, () -> LootSession.newSession(finalLootTable, player));
        final LootContext context = new LootContext(session, location, LootSource.of("Woodcutting", "woodcutting:treasure"));
        return finalLootTable.generateLoot(context);
    }
}
