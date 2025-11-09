package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.GatorStrokeAbility;
import me.mykindos.betterpvp.champions.item.ability.UnderwaterBreathingAbility;
import me.mykindos.betterpvp.champions.item.ability.WaterDamageAbility;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.AlligatorScale;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.FangOfTheDeep;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class AlligatorsTooth extends WeaponItem implements Reloadable {

    private transient boolean registered;

    private final GatorStrokeAbility gatorStrokeAbility;
    private final WaterDamageAbility waterDamageAbility;
    private final UnderwaterBreathingAbility underwaterBreathingAbility;
    private final ItemFactory itemFactory;

    @Inject
    private AlligatorsTooth(Champions champions, 
                           GatorStrokeAbility gatorStrokeAbility,
                           UnderwaterBreathingAbility underwaterBreathingAbility,
                           ItemFactory itemFactory) {
        super(champions, "Alligator's Tooth", Item.model("alligators_tooth"), ItemRarity.LEGENDARY);
        this.gatorStrokeAbility = gatorStrokeAbility;
        this.underwaterBreathingAbility = underwaterBreathingAbility;
        this.waterDamageAbility = new WaterDamageAbility(champions, itemFactory, this);
        this.itemFactory = itemFactory;
        
        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(gatorStrokeAbility)
                .ability(underwaterBreathingAbility)
                .ability(waterDamageAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Configure GatorStroke ability
        gatorStrokeAbility.setVelocityStrength(config.getConfig("velocityStrength", 0.7, Double.class));
        gatorStrokeAbility.setEnergyPerTick(config.getConfig("energyPerTick", 1.0, Double.class));
        gatorStrokeAbility.setSkimmingEnergyMultiplier(config.getConfig("skimmingEnergyMultiplier", 3.0, Double.class));
        
        // Configure damage values
        double bonusDamage = config.getConfig("bonusDamage", 4.0, Double.class);
        waterDamageAbility.setBonusDamage(bonusDamage);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                AlligatorScale alligatorScale, FangOfTheDeep fangOfTheDeep, DurakHandle durakHandle) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "SFS",
                "SFS",
                "SDS"
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('S', new RecipeIngredient(alligatorScale, 1));
        builder.setIngredient('F', new RecipeIngredient(fangOfTheDeep, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        registry.registerRecipe(new NamespacedKey("champions", "alligators_tooth"), builder.build());
    }
} 