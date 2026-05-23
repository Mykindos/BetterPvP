package me.mykindos.betterpvp.progression.profession.fishing.loot;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Loot that drops a fish as item stacks at the hook location.
 *
 * <p><b>Lifecycle contract:</b> A {@code LootTable} reuses the same {@code Loot<?,?>} instance
 * across rolls. Because fish weight is mutable by skills (ThickerLines, CatchWeightAttribute),
 * callers MUST invoke {@link #rollFish()} immediately after retrieving this entry from the bundle
 * and BEFORE firing {@code PlayerCaughtFishEvent}. The roll sets a fresh {@link Fish} with a
 * random weight in [{@code minWeight}, {@code maxWeight}]. Skills may then mutate the fish weight.
 * {@link #award} drops the stacks based on whatever weight is current.
 *
 * <p>JSON shape:
 * <pre>
 * { "type": "fish", "itemId": "minecraft:cod", "displayName": "Cod", "minWeight": 1, "maxWeight": 5 }
 * </pre>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public final class FishLoot extends Loot<Fish, Item> {

    private final ItemFactory itemFactory;
    private final NamespacedKey itemKey;
    private final String displayName;
    private final int minWeight;
    private final int maxWeight;

    /**
     * The current fish for this catch bundle. Set by {@link #rollFish()} before award.
     * Not part of equals/hashCode — it is transient per-catch state.
     */
    @Setter
    @EqualsAndHashCode.Exclude
    private Fish currentFish;

    public FishLoot(ItemFactory itemFactory, NamespacedKey itemKey, String displayName,
                    int minWeight, int maxWeight,
                    ReplacementStrategy replacementStrategy, Predicate<LootContext> condition) {
        super(replacementStrategy, condition);
        this.itemFactory = itemFactory;
        this.itemKey = itemKey;
        this.displayName = displayName;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        rollFish();
    }

    /**
     * Rolls a fresh {@link Fish} for the current catch bundle.
     * Must be called once per catch event, before skills mutate the fish and before {@link #award}.
     */
    public void rollFish() {
        int weight = (minWeight == maxWeight)
                ? minWeight
                : ThreadLocalRandom.current().nextInt(minWeight, maxWeight + 1);
        this.currentFish = new Fish(UUID.randomUUID(), displayName, weight);
    }

    @Override
    public Fish getReward() {
        return currentFish;
    }

    /**
     * Drops fish item stacks at the context's location, sliced into 64-stack chunks.
     * Player is resolved from the session audience.
     *
     * @return The last dropped {@link Item} entity, or {@code null} if none was dropped.
     */
    @Override
    protected Item award(LootContext context) {
        final Audience audience = context.getSession().getAudience();

        Player player = null;
        if (audience instanceof Player p) {
            player = p;
        } else if (audience instanceof ForwardingAudience forwardingAudience) {
            for (Audience member : forwardingAudience.audiences()) {
                if (member instanceof Player p) {
                    player = p;
                    break;
                }
            }
        }

        final BaseItem baseItem = itemFactory.getItemRegistry().getItem(itemKey);
        Preconditions.checkNotNull(baseItem, "FishLoot: item key not found in registry: " + itemKey);

        final Progression progression = JavaPlugin.getPlugin(Progression.class);
        // Reel-in vector pointing from the hook back toward the player, mirroring the
        // velocity Minecraft applies to the natural fishing-rod caught Item entity.
        final Vector reelVelocity;
        if (player != null) {
            reelVelocity = player.getLocation().toVector()
                    .subtract(context.getLocation().toVector())
                    .multiply(0.1)
                    .add(new Vector(0, 0.2, 0));
        } else {
            reelVelocity = new Vector(0, 0.2, 0);
        }

        Item lastDropped = null;
        int remaining = Math.max(1, currentFish.getWeight());

        while (remaining > 0) {
            final int dropAmount = Math.min(remaining, 64);
            remaining -= dropAmount;

            final ItemStack stack = itemFactory.create(baseItem).createItemStack();
            stack.setAmount(dropAmount);

            final Item dropped = context.getLocation().getWorld().dropItem(context.getLocation(), stack);
            if (player != null) {
                UtilItem.reserveItem(dropped, player, 10);
            }

            // Bukkit overrides velocity on the same tick the entity spawns, so apply it next tick.
            final Item finalDropped = dropped;
            UtilServer.runTaskLater(progression, () -> {
                if (finalDropped.isValid()) {
                    finalDropped.setVelocity(reelVelocity);
                }
            }, 1L);
            UtilServer.runTaskLater(progression, finalDropped::remove, 20L * 30L);

            lastDropped = dropped;
        }

        return lastDropped;
    }

    @Override
    public ItemView getIcon() {
        final BaseItem item = itemFactory.getItemRegistry().getItem(itemKey);
        final ItemView.ItemViewBuilder builder = item == null
                ? ItemView.builder().material(Material.COD)
                : ItemView.of(itemFactory.create(item).getView().get()).toBuilder();
        builder.lore(Component.empty());
        builder.lore(Component.empty()
                .append(Component.text("Minimum Weight: ", NamedTextColor.GRAY))
                .append(Component.text(minWeight, NamedTextColor.WHITE)));
        builder.lore(Component.empty()
                .append(Component.text("Maximum Weight: ", NamedTextColor.GRAY))
                .append(Component.text(maxWeight, NamedTextColor.WHITE)));
        return builder.build();
    }

    @Override
    public String toString() {
        return "FishLoot{itemKey=" + itemKey + ", displayName='" + displayName + "', weight=[" + minWeight + "," + maxWeight + "]}";
    }
}
