package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.treasurechance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerWoodcuttingTreasureDropEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        if (chance <= 0 || UtilMath.randDouble(0, 1) >= chance) {
            return;
        }

        LootBundle bundle = rollTreasure(player, location);
        PlayerWoodcuttingTreasureDropEvent treasureEvent = UtilServer.callEvent(new PlayerWoodcuttingTreasureDropEvent(player, location, bundle));
        if (treasureEvent.isCancelled()) {
            return;
        }

        for (Loot<?, ?> loot : bundle) {
            awardTreasure(player, location, loot, bundle.getContext());
        }
    }

    private LootBundle rollTreasure(Player player, Location location) {
        final LootSession session = sessionController.resolve(player, treasureLootTable, () -> LootSession.newSession(treasureLootTable, player));
        final LootContext context = new LootContext(session, location, "Woodcutting");
        return treasureLootTable.generateLoot(context);
    }

    private void awardTreasure(Player player, Location location, Loot<?, ?> loot, LootContext context) {
        final Object award = loot.award(context);
        if (award instanceof Item item) {
            sendMessage(player, location, item.getItemStack());
        } else if (award instanceof ItemStack itemStack) {
            ItemStack finalItemStack = itemFactory.convertItemStack(itemStack).orElse(itemStack);
            UtilItem.insert(player, finalItemStack);
            sendMessage(player, location, finalItemStack);
        } else if (award instanceof List<?> itemStacks) {
            for (Object awardedItem : itemStacks) {
                if (!(awardedItem instanceof ItemStack itemStack)) continue;
                sendMessage(player, location, itemStack);
            }
        }
    }

    private void sendMessage(Player player, Location location, ItemStack itemStack) {
        final Component name;
        Optional<ItemInstance> itemInstanceOptional = itemFactory.fromItemStack(itemStack);
        if(itemInstanceOptional.isPresent()) {
            ItemInstance itemInstance = itemInstanceOptional.get();
            name = itemInstance.getView().getName();
        } else {
            if (itemStack.getItemMeta() != null && itemStack.getItemMeta().hasDisplayName()) {
                name = Objects.requireNonNull(itemStack.getItemMeta().displayName());
            } else {
                name = Objects.requireNonNullElse(itemStack.getData(DataComponentTypes.ITEM_NAME),
                        Component.translatable(itemStack.getType().translationKey()));
            }
        }

        TextComponent message = Component.text("You found ")
                .append(Component.text(UtilFormat.formatNumber(itemStack.getAmount())))
                .append(Component.text(" "))
                .append(name);

        UtilMessage.message(player, "Woodcutting", message);

        log.info("{} found {}x {} from woodcutting treasure", player.getName(), itemStack.getAmount(),
                        itemStack.getType().name().toLowerCase())
                .addClientContext(player).addLocationContext(location).submit();
    }
}
