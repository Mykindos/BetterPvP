package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.fishingdoubletreasurechance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootSource;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerFishingDoubleTreasureDropEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerFishingTreasureDropEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;

@BPvPListener
@Singleton
@CustomLog
public class DoubleTreasureChanceListener implements Listener, Reloadable {

    private final FishingDoubleTreasureChanceAttribute doubleTreasureChanceAttribute;
    private final LootTableRegistry lootTableRegistry;
    private final LootSessionController sessionController;
    private final ItemFactory itemFactory;

    private LootTable treasureLootTable;

    @Inject
    public DoubleTreasureChanceListener(FishingDoubleTreasureChanceAttribute doubleTreasureChanceAttribute,
                                        LootTableRegistry lootTableRegistry,
                                        LootSessionController sessionController, ItemFactory itemFactory) {
        this.doubleTreasureChanceAttribute = doubleTreasureChanceAttribute;
        this.lootTableRegistry = lootTableRegistry;
        this.sessionController = sessionController;
        this.itemFactory = itemFactory;
    }

    @Override
    public void reload() {
        this.treasureLootTable = lootTableRegistry.loadLootTable("fishing_treasure_chance");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreasureDrop(PlayerFishingTreasureDropEvent event) {
        final Player player = event.getPlayer();
        final Location location = event.getLocation();

        double chance = doubleTreasureChanceAttribute.getChance(player);
        if (chance <= 0 || UtilMath.randDouble(0, 1) >= chance) {
            return;
        }

        LootBundle bundle = rollTreasure(player, location);
        PlayerFishingDoubleTreasureDropEvent doubleEvent = UtilServer.callEvent(new PlayerFishingDoubleTreasureDropEvent(player, location, bundle));
        if (doubleEvent.isCancelled()) {
            return;
        }

        bundle.award();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDoubleTreasureLootAwarded(LootAwardedEvent event) {
        if (!"fishing:treasure_double".equals(event.getContext().getSource().getId())) return;
        final Audience audience = event.getContext().getSession().getAudience();
        if (!(audience instanceof Player player)) return;
        final LootContext context = event.getContext();
        final Location location = context.getLocation();
        final Object award = event.getRawResult();

        if (award instanceof Item item) {
            final Vector reelVelocity = player.getLocation().toVector()
                    .subtract(location.toVector())
                    .multiply(0.1)
                    .add(new Vector(0, 0.2, 0));
            if (item.isValid()) {
                item.setVelocity(reelVelocity);
            }
            sendMessage(player, location, item.getItemStack());
        } else if (award instanceof ItemStack itemStack) {
            sendMessage(player, location, itemStack);
        }
    }

    private LootBundle rollTreasure(Player player, Location location) {
        final LootSession session = sessionController.resolve(player, treasureLootTable, () -> LootSession.newSession(treasureLootTable, player));
        final LootContext context = new LootContext(session, location, LootSource.of("Fishing", "fishing:treasure_double"));
        return treasureLootTable.generateLoot(context);
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
                .append(name)
                .append(Component.text(" (doubled!)"));

        UtilMessage.message(player, "Fishing", message);

        log.info("{} found {}x {} from fishing treasure (doubled)", player.getName(), itemStack.getAmount(),
                        itemStack.getType().name().toLowerCase())
                .addClientContext(player).addLocationContext(location).submit();
    }
}
