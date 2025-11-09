package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.HyperRushAbility;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.repo.MeleeAttackSpeedStat;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.OverchargedCrystal;
import me.mykindos.betterpvp.core.item.impl.RazorEdge;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.ToolComponent;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class HyperAxe extends WeaponItem implements Reloadable {

    private static final ItemStack model;

    private transient boolean registered;
    private double attackSpeed;
    private final HyperRushAbility hyperRushAbility;
    private final ItemFactory itemFactory;

    static {
        model = Item.model(Material.NETHERITE_AXE, "hyper_axe");

        // Register as an axe for block breaking
        model.editMeta(meta -> {
            ToolComponent toolComponent = meta.getTool();
            toolComponent.addRule(Tag.MINEABLE_AXE, 33f, true);
            meta.setTool(toolComponent);
        });
    }

    @Inject
    private HyperAxe(Champions champions, 
                    CooldownManager cooldownManager,
                    EffectManager effectManager,
                    ItemFactory itemFactory) {
        super(champions, "Hyper Axe", model, ItemRarity.EPIC);
        this.hyperRushAbility = new HyperRushAbility(champions, cooldownManager, effectManager);
        this.itemFactory = itemFactory;
        this.attackSpeed = 1; // 100%+ attack speed by default

        // Add attack speed stat
        updateAttackSpeed();

        // Add components
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(hyperRushAbility)
                .build());
    }

    private void updateAttackSpeed() {
        getComponent(StatContainerComponent.class)
                .orElseGet(() -> {
                    StatContainerComponent newContainer = new StatContainerComponent();
                    addBaseComponent(newContainer);
                    return newContainer;
                })
                .withBaseStat(new MeleeAttackSpeedStat(attackSpeed));
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Configure HyperRush ability
        hyperRushAbility.setCooldown(config.getConfig("hyperRushCooldown", 16.0, Double.class));
        hyperRushAbility.setSpeedAmplifier(config.getConfig("hyperRushSpeedLevel", 3, Integer.class));
        hyperRushAbility.setDurationTicks(config.getConfig("hyperRushDuration", 160, Integer.class));
        
        // Configure attack speed
        this.attackSpeed = config.getConfig("attackSpeedPercentage", 1.3, Double.class);
        updateAttackSpeed();
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                RazorEdge razorEdge, OverchargedCrystal overchargedCrystal) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                " RO",
                " SR",
                "S"
        };
        final BaseItem stick = itemFactory.getFallbackItem(Material.STICK);
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('R', new RecipeIngredient(razorEdge, 1));
        builder.setIngredient('O', new RecipeIngredient(overchargedCrystal, 1));
        builder.setIngredient('S', new RecipeIngredient(stick, 1));
        registry.registerRecipe(new NamespacedKey("champions", "hyper_axe"), builder.build());
    }
} 