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
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerFishingDoubleTreasureDropEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerFishingTreasureDropEvent;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
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
public class DoubleTreasureChanceListener implements Listener {

    private final FishingDoubleTreasureChanceAttribute doubleTreasureChanceAttribute;
    private final ItemFactory itemFactory;

    @Inject
    public DoubleTreasureChanceListener(FishingDoubleTreasureChanceAttribute doubleTreasureChanceAttribute,
                                        ItemFactory itemFactory) {
        this.doubleTreasureChanceAttribute = doubleTreasureChanceAttribute;
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreasureDrop(PlayerFishingTreasureDropEvent event) {
        final Player player = event.getPlayer();
        final Location location = event.getLocation();

        double chance = doubleTreasureChanceAttribute.getChance(player);
        if (chance <= 0 || UtilMath.randDouble(0, 1) >= chance) {
            return;
        }

        final LootBundle original = event.getBundle();
        final LootContext doubleContext = new LootContext(
                original.getContext().getSession(),
                location,
                LootSource.of("Fishing", "fishing:treasure_double")
        );
        final LootBundle doubled = original.duplicate(doubleContext);

        PlayerFishingDoubleTreasureDropEvent doubleEvent = UtilServer.callEvent(new PlayerFishingDoubleTreasureDropEvent(player, location, doubled));
        if (doubleEvent.isCancelled()) {
            return;
        }

        doubled.award();
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

    private void sendMessage(Player player, Location location, ItemStack itemStack) {
        final Component name;
        Optional<ItemInstance> itemInstanceOptional = itemFactory.fromItemStack(itemStack);
        if (itemInstanceOptional.isPresent()) {
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

        UtilMessage.message(player, "core.prefix.fishing", Translations.component("progression.fishing.treasure.found-doubled",
                Component.text(UtilFormat.formatNumber(itemStack.getAmount())),
                name));

        log.info("{} found {}x {} from fishing treasure (doubled)", player.getName(), itemStack.getAmount(),
                        itemStack.getType().name().toLowerCase())
                .addClientContext(player).addLocationContext(location).submit();
    }
}
