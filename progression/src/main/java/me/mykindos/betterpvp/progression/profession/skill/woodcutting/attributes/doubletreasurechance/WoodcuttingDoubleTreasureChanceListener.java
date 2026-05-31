package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.doubletreasurechance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@BPvPListener
@Singleton
@CustomLog
public class WoodcuttingDoubleTreasureChanceListener implements Listener {

    private final WoodcuttingDoubleTreasureChanceAttribute doubleTreasureChanceAttribute;
    private final ItemFactory itemFactory;

    @Inject
    public WoodcuttingDoubleTreasureChanceListener(WoodcuttingDoubleTreasureChanceAttribute doubleTreasureChanceAttribute,
                                                   ItemFactory itemFactory) {
        this.doubleTreasureChanceAttribute = doubleTreasureChanceAttribute;
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreasureLootAwarded(LootAwardedEvent event) {
        if (!"woodcutting:treasure".equals(event.getContext().getSource().getId())) return;
        final Audience audience = event.getContext().getSession().getAudience();
        if (!(audience instanceof Player player)) return;
        final Location location = event.getContext().getLocation();
        final Object award = event.getRawResult();

        double chance = doubleTreasureChanceAttribute.getChance(player);
        boolean doubled = chance > 0 && UtilMath.randDouble(0, 1) < chance;

        if (award instanceof Item item) {
            if (doubled) {
                UtilItem.insert(player, item.getItemStack().clone());
            }
            sendMessage(player, location, item.getItemStack(), doubled);
        } else if (award instanceof ItemStack itemStack) {
            ItemStack converted = itemFactory.convertItemStack(itemStack).orElse(itemStack);
            if (doubled) {
                UtilItem.insert(player, converted.clone());
            }
            sendMessage(player, location, converted, doubled);
        } else if (award instanceof List<?> itemStacks) {
            for (Object awardedItem : itemStacks) {
                if (!(awardedItem instanceof ItemStack itemStack)) continue;
                if (doubled) {
                    UtilItem.insert(player, itemStack.clone());
                }
                sendMessage(player, location, itemStack, doubled);
            }
        }
    }

    private void sendMessage(Player player, Location location, ItemStack itemStack, boolean doubled) {
        final Component name;
        Optional<ItemInstance> itemInstanceOptional = itemFactory.fromItemStack(itemStack);
        if (itemInstanceOptional.isPresent()) {
            name = itemInstanceOptional.get().getView().getName();
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

        if (doubled) {
            message = message.append(Component.text(" (doubled!)"));
        }

        UtilMessage.message(player, "Woodcutting", message);

        log.info("{} found {}x {} from woodcutting treasure{}", player.getName(), itemStack.getAmount(),
                        itemStack.getType().name().toLowerCase(), doubled ? " (doubled)" : "")
                .addClientContext(player).addLocationContext(location).submit();
    }
}
