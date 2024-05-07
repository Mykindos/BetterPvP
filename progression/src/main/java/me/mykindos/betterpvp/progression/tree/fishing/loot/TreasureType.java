package me.mykindos.betterpvp.progression.tree.fishing.loot;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;

@Data
public class TreasureType implements FishingLootType {

    private static final Random RANDOM = new Random();

    @Getter(AccessLevel.NONE)
    private final String key;

    private int frequency;
    private Material material;
    private int minAmount;
    private int maxAmount;
    private @Nullable Integer customModelData;

    @Override
    public @NotNull String getName() {
        return "Treasure";
    }

    @Override
    public FishingLoot generateLoot() {
        return new FishingLoot() {
            @Override
            public @NotNull FishingLootType getType() {
                return TreasureType.this;
            }

            @Override
            public void processCatch(PlayerCaughtFishEvent event) {
                final int count = RANDOM.ints(minAmount, maxAmount + 1)
                        .findFirst()
                        .orElse(minAmount);
                final ItemStack itemStack = new ItemStack(material, count);
                itemStack.editMeta(meta -> meta.setCustomModelData(customModelData));

                final Item item = (Item) Objects.requireNonNull(event.getCaught());
                item.setItemStack(itemStack);
            }
        };
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.frequency = config.getOrSaveInt("fishing.loot." + key + ".frequency", 1);
        this.minAmount = config.getOrSaveInt("fishing.loot." + key + ".minAmount", 1);
        this.maxAmount = config.getOrSaveInt("fishing.loot." + key + ".maxAmount", 1);
        this.customModelData = config.getObject("fishing.loot." + key + ".customModelData", Integer.class, null);

        final String materialKey = config.getOrSaveString("fishing.loot." + key + ".material", "STONE");
        if (materialKey == null) {
            throw new IllegalArgumentException("Material key cannot be null!");
        }
        try {
            this.material = Material.valueOf(materialKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid material key: " + materialKey, e);
        }
    }
}
