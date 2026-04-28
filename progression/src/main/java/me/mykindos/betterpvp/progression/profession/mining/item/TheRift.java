package me.mykindos.betterpvp.progression.profession.mining.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.blockbreak.component.ToolComponent;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.preset.BlockGroups;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.item.interaction.ChainThrowInteraction;
import me.mykindos.betterpvp.progression.profession.mining.item.interaction.ExplosiveExcavationInteraction;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("progression:the_rift")
public class TheRift extends BaseItem implements Reloadable {

    private static final Random RANDOM = new Random();
    private static final String CONFIG_FILE = "items/tool";
    private static final String ITEM_KEY = "the_rift";

    private final ExplosiveExcavationInteraction explosiveExcavationInteraction;
    private final ChainThrowInteraction chainThrowInteraction;

    // Map<Material, Weight> populated from items/tool.yml :: the_rift.oreWeights.
    // Both interactions read from this through a shared Supplier so config reloads
    // propagate without re-wiring.
    private final Map<Material, Double> oreWeights = new LinkedHashMap<>();

    @EqualsAndHashCode.Exclude
    private final Progression progression;

    @Inject
    private TheRift(Progression progression,
                    CooldownManager cooldownManager,
                    ItemFactory itemFactory) {
        super("The Rift",
                Item.model(Material.DIAMOND_PICKAXE, "the_rift"),
                ItemGroup.TOOL,
                ItemRarity.EPIC);
        this.progression = progression;

        this.explosiveExcavationInteraction = new ExplosiveExcavationInteraction(
                itemFactory,
                0.5, // trigger chance per stone mine
                2,    // sphere radius (~5-block diameter)
                0.35  // chance per shell block to be replaced with ore
        );
        this.explosiveExcavationInteraction.setOreSupplier(this::pickWeightedOre);

        this.chainThrowInteraction = new ChainThrowInteraction(
                this.explosiveExcavationInteraction, cooldownManager, itemFactory, this,
                15.0,  // cooldown seconds
                4.0,   // alive time seconds
                1.4,   // speed blocks/tick
                0.3,   // explosion interval seconds
                2,     // explosion radius
                0.25,  // chance per shell block to be lined with ore
                10,     // max bounces before auto-recall
                true   // allow manual recall via second press
        );
        this.chainThrowInteraction.setOreSupplier(this::pickWeightedOre);

        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.BLOCK_BREAK, explosiveExcavationInteraction)
                .root(InteractionInputs.RIGHT_CLICK, chainThrowInteraction)
                .build());

        addBaseComponent(new ToolComponent()
                .addRule(BlockBreakRule.of(BlockGroups.STONES, BlockBreakProperties.breakable(180))));
    }

    @Override
    public void reload() {
        final Config excavationConfig = Config.item(progression, this).fork("excavation");
        explosiveExcavationInteraction.setTriggerChance(excavationConfig.getConfig("trigger-chance", 0.5, Double.class));
        explosiveExcavationInteraction.setRadius(excavationConfig.getConfig("radius", 2, Integer.class));
        explosiveExcavationInteraction.setOreChance(excavationConfig.getConfig("ore-chance", 0.35, Double.class));

        final Config chainThrowConfig = Config.item(progression, this).fork("chain-throw");
        chainThrowInteraction.setCooldown(chainThrowConfig.getConfig("cooldown", 15.0, Double.class));
        chainThrowInteraction.setAliveTime(chainThrowConfig.getConfig("alive-time", 4.0, Double.class));
        chainThrowInteraction.setSpeed(chainThrowConfig.getConfig("speed", 1.4, Double.class));
        chainThrowInteraction.setExplosionInterval(chainThrowConfig.getConfig("explosion-interval", 0.3, Double.class));
        chainThrowInteraction.setExplosionRadius(chainThrowConfig.getConfig("explosion-radius", 2, Integer.class));
        chainThrowInteraction.setOreChance(chainThrowConfig.getConfig("ore-chance", 0.25, Double.class));
        chainThrowInteraction.setMaxBounces(chainThrowConfig.getConfig("max-bounces", 10, Integer.class));
        chainThrowInteraction.setAllowRecall(chainThrowConfig.getConfig("allow-recall", true, Boolean.class));

        loadOreWeights();
    }

    /**
     * Loads the {@code oreWeights} map from {@code items/tool.yml} under
     * {@code the_rift.oreWeights}. Keys are Material names; values are positive numeric weights.
     * Unknown materials and non-positive weights are silently skipped.
     * If the section is missing or empty, default weights are written so the user has something
     * to edit.
     */
    private void loadOreWeights() {
        final ExtendedYamlConfiguration config = progression.getConfig(CONFIG_FILE);
        final String path = ITEM_KEY + ".ore-weights";

        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null || section.getKeys(false).isEmpty()) {
            section = config.createSection(path);
            section.set(Material.COAL_ORE.getKey().toString(), 30.0);
            section.set(Material.IRON_ORE.getKey().toString(), 30.0);
            section.set(Material.COPPER_ORE.getKey().toString(), 30.0);
            section.set(Material.GOLD_ORE.getKey().toString(), 30.0);
            section.set(Material.REDSTONE_ORE.getKey().toString(), 30.0);
            section.set(Material.LAPIS_ORE.getKey().toString(), 30.0);
            section.set(Material.DIAMOND_ORE.getKey().toString(), 30.0);
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

    /**
     * Weighted-random draw from {@link #oreWeights}. Returns {@code null} if the map is empty
     * or all weights are non-positive.
     */
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
