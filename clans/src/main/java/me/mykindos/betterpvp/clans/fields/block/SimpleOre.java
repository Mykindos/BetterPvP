package me.mykindos.betterpvp.clans.fields.block;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.fields.model.FieldsOre;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

@Getter
public enum SimpleOre implements FieldsOre {

    // Stone
    GOLD("Gold Ore", Material.GOLD_ORE, Material.STONE, Material.GOLD_INGOT, 3),
    IRON("Iron Ore", Material.IRON_ORE, Material.STONE, Material.IRON_INGOT, 3),
    COAL("Coal Ore", Material.COAL_ORE, Material.STONE, Material.COAL, 4),
    COPPER("Leather Ore", Material.COPPER_ORE, Material.STONE, Material.LEATHER, 3),
    DIAMOND("Diamond Ore", Material.DIAMOND_ORE, Material.STONE, Material.DIAMOND, 3),
    EMERALD("Emerald Ore", Material.EMERALD_ORE, Material.STONE, Material.EMERALD, 2),
    LAPIS("Lapis Ore", Material.LAPIS_ORE, Material.STONE, Material.LAPIS_LAZULI, 5),
    REDSTONE("Redstone Ore", Material.REDSTONE_ORE, Material.STONE, Material.REDSTONE, 1),
    GILDED_BLACKSTONE("Netherite Ore", Material.GILDED_BLACKSTONE, Material.STONE, Material.NETHERITE_INGOT, 3),

    // Deepslate
    GOLD_DEEPSLATE("Condensed Gold Ore", Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE, Material.GOLD_INGOT, 3),
    IRON_DEEPSLATE("Condensed Iron Ore", Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE, Material.IRON_INGOT, 3),
    COAL_DEEPSLATE("Condensed Coal Ore", Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE, Material.COAL, 4),
    COPPER_DEEPSLATE("Condensed Leather Ore", Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE, Material.LEATHER, 3),
    DIAMOND_DEEPSLATE("Condensed Diamond Ore", Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE, Material.DIAMOND, 3),
    EMERALD_DEEPSLATE("Condensed Emerald Ore", Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE, Material.EMERALD, 2),
    LAPIS_DEEPSLATE("Condensed Lapis Ore", Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE, Material.LAPIS_LAZULI, 5),
    REDSTONE_DEEPSLATE("Condensed Redstone Ore", Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE, Material.REDSTONE, 1),

    // Netherrack
    GOLD_NETHER("Gold Ore", Material.NETHER_GOLD_ORE, Material.NETHERRACK, Material.GOLD_INGOT, 3),
    QUARTZ("Quartz Ore", Material.NETHER_QUARTZ_ORE, Material.NETHERRACK, Material.QUARTZ, 5),
    ;

    private static final Random RANDOM = new Random();

    private final @NotNull BlockData type;

    private final @NotNull BlockData replacement;

    private final @NotNull Material drop;

    @Setter
    private double respawnDelay = 60; // All default to 60 seconds

    private final String name;

    private int min;
    private int max;

    SimpleOre(@NotNull String name, Material type, Material replacement, Material drop, int max) {
        this.type = type.createBlockData();
        this.replacement = replacement.createBlockData();
        this.drop = drop;
        this.name = name;
        this.max = max;
    }

    @Override
    public String getName() {
        return name;
    }

    public ItemStack @NotNull [] generateDrops(@NotNull FieldsBlock fieldsBlock) {
        return new ItemStack[] {
                new ItemStack(drop, RANDOM.ints(min, max + 1).findAny().orElse(1))
        };
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        final String key = "fields.blocks." + getName().toLowerCase().replace(" ", "");
        min = config.getOrSaveInt(key + ".min", min);
        max = config.getOrSaveInt(key + ".max", max);
    }

}
