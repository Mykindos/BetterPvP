package me.mykindos.betterpvp.progression.profession.fishing.fish;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Data;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLoot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.UUID;

/**
 * Represents a fish that has a minimum and maximum weight with no
 * custom implementation.
 */
@Data
@CustomLog
public class SimpleFishType implements FishType {

    private static final Random RANDOM = new Random();

    private final String key;
    private final ItemFactory itemFactory;
    private NamespacedKey itemKey;
    private String name;
    private int minWeight;
    private int maxWeight;
    private int frequency;

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.name = config.getOrSaveString("fishing.loot." + key + ".name", "Fish");
        this.frequency = config.getOrSaveInt("fishing.loot." + key + ".frequency", 1);
        this.minWeight = config.getOrSaveInt("fishing.loot." + key + ".minWeight", 1);
        this.maxWeight = config.getOrSaveInt("fishing.loot." + key + ".maxWeight", 1);
        String itemKey = config.getOrSaveString("fishing.loot." + key + ".item", "minecraft:cod");
        if (itemKey == null) {
            throw new IllegalArgumentException("Item key cannot be null!");
        }

        final NamespacedKey namespacedKey = NamespacedKey.fromString(itemKey);
        if (namespacedKey == null) {
            throw new IllegalArgumentException("Invalid item key: " + itemKey);
        }
        this.itemKey = namespacedKey;
    }

    public ItemStack generateItem(int count) {
        final BaseItem baseItem =  itemFactory.getItemRegistry().getItem(itemKey);
        Preconditions.checkArgument(baseItem != null, "Invalid item key: " + itemKey);
        final ItemStack itemStack = itemFactory.create(baseItem).createItemStack();
        itemStack.setAmount(count);
        return itemStack;
    }

    @Override
    public FishingLoot generateLoot() {
        final int weight = RANDOM.ints(minWeight, maxWeight + 1)
                .findFirst()
                .orElse(minWeight);
        UUID uuid = UUID.randomUUID();
        return new Fish(uuid, this, weight);
    }
}
