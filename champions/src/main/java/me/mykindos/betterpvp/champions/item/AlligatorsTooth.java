package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.GatorStrokeAbility;
import me.mykindos.betterpvp.champions.item.ability.UnderwaterBreathingAbility;
import me.mykindos.betterpvp.champions.item.ability.WaterDamageAbility;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
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
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("champions:alligators_tooth")
public class AlligatorsTooth extends WeaponItem implements Reloadable {

    private static final ItemStack model;

    static {
        model = Item.model("alligators_tooth");
        model.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(Float.MAX_VALUE)
                .animation(ItemUseAnimation.NONE)
                .build());
        model.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
                .canSprint(true)
                .speedMultiplier(1f)
                .build());
    }

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
        super(champions, "Alligator's Tooth", model, ItemRarity.LEGENDARY);
        this.gatorStrokeAbility = gatorStrokeAbility;
        this.underwaterBreathingAbility = underwaterBreathingAbility;
        this.waterDamageAbility = new WaterDamageAbility(champions, itemFactory, this);
        this.itemFactory = itemFactory;

        // Add abilities to container
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.HOLD_RIGHT_CLICK, gatorStrokeAbility)
                .root(InteractionInputs.HOLD, underwaterBreathingAbility)
                .root(InteractionInputs.PASSIVE, waterDamageAbility)
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
