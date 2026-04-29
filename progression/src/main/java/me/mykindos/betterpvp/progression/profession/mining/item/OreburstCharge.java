package me.mykindos.betterpvp.progression.profession.mining.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableHandler;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.CoalBlockItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.item.interaction.OreburstChargeInteraction;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("progression:oreburst_charge")
public class OreburstCharge extends BaseItem implements Reloadable {

    private static final Random RANDOM = new Random();
    private static final String CONFIG_FILE = "items/tool";
    private static final String ITEM_KEY = "oreburst_charge";

    @EqualsAndHashCode.Exclude
    private final OreburstChargeInteraction oreburstChargeInteraction;

    @EqualsAndHashCode.Exclude
    private final Progression progression;

    private final transient Map<Material, Double> oreWeights = new LinkedHashMap<>();

    private transient boolean registered;

    @Inject
    private OreburstCharge(Progression progression,
                           CooldownManager cooldownManager,
                           ThrowableHandler throwableHandler,
                           BlockTagManager blockTagManager) {
        super("Oreburst Charge", ItemStack.of(Material.FIREWORK_STAR), ItemGroup.CONSUMABLE, ItemRarity.RARE);
        this.progression = progression;

        this.oreburstChargeInteraction = new OreburstChargeInteraction(
                cooldownManager,
                throwableHandler,
                blockTagManager,
                25.0, // cooldown seconds
                10.0, // throwable expiry seconds
                1.6,  // throw speed multiplier
                4,    // sphere radius
                0.55  // ore-conversion chance per qualifying block
        );
        this.oreburstChargeInteraction.setOreSupplier(this::pickWeightedOre);
        this.oreburstChargeInteraction.setConsumesItem(true);

        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.LEFT_CLICK, oreburstChargeInteraction)
                .build());
    }

    @Override
    public void reload() {
        final Config parent = Config.item(progression, this);
        oreburstChargeInteraction.setCooldown(parent.getConfig("cooldown", 25.0, Double.class));
        oreburstChargeInteraction.setThrowableExpiry(parent.getConfig("throwable-expiry", 10.0, Double.class));
        oreburstChargeInteraction.setThrowSpeed(parent.getConfig("throw-speed", 1.6, Double.class));
        oreburstChargeInteraction.setRadius(parent.getConfig("radius", 4, Integer.class));
        oreburstChargeInteraction.setOreChance(parent.getConfig("ore-chance", 0.55, Double.class));

        loadOreWeights();
    }

    private void loadOreWeights() {
        final ExtendedYamlConfiguration config = progression.getConfig(CONFIG_FILE);
        final String path = ITEM_KEY + ".ore-weights";

        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null || section.getKeys(false).isEmpty()) {
            section = config.createSection(path);
            section.set(Material.COAL_ORE.getKey().toString(), 30.0);
            section.set(Material.IRON_ORE.getKey().toString(), 30.0);
            section.set(Material.COPPER_ORE.getKey().toString(), 30.0);
            section.set(Material.GOLD_ORE.getKey().toString(), 20.0);
            section.set(Material.REDSTONE_ORE.getKey().toString(), 20.0);
            section.set(Material.LAPIS_ORE.getKey().toString(), 15.0);
            section.set(Material.DIAMOND_ORE.getKey().toString(), 5.0);
        }

        oreWeights.clear();
        for (String key : section.getKeys(false)) {
            final Material material = Material.matchMaterial(key);
            if (material == null || !material.isBlock()) continue;
            final double weight = section.getDouble(key, 0.0);
            if (weight <= 0) continue;
            oreWeights.put(material, weight);
        }
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                CoalBlockItem coalBlockItem) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "FFF",
                "FCF",
                "FFF",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('F', new RecipeIngredient(itemFactory.getFallbackItem(Material.FLINT), 1));
        builder.setIngredient('C', new RecipeIngredient(coalBlockItem, 1));
        registry.registerRecipe(new NamespacedKey("progression", "oreburst_charge"), builder.build());
    }

    private Material pickWeightedOre() {
        if (oreWeights.isEmpty()) return null;
        double total = 0;
        for (double w : oreWeights.values()) total += w;
        if (total <= 0) return null;

        double roll = RANDOM.nextDouble() * total;
        double cumulative = 0;
        for (Map.Entry<Material, Double> entry : oreWeights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) return entry.getKey();
        }
        return null;
    }
}
