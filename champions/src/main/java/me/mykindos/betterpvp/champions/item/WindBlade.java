package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.FeatherFeetAbility;
import me.mykindos.betterpvp.champions.item.ability.ZephyrFlightAbility;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.TooltipSpriteComponent;
import me.mykindos.betterpvp.core.item.component.impl.temper.TemperComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.AetherCore;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.FeatherOfZephyr;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.item.temper.TemperProfile;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("champions:wind_blade")
public class WindBlade extends WeaponItem implements Reloadable {

    private static final ItemStack model;

    static {
        model = Item.model("windblade");
        model.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(Float.MAX_VALUE)
                .animation(ItemUseAnimation.NONE)
                .build());
        model.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
                .canSprint(true)
                .speedMultiplier(1f)
                .build());
    }

    private final ZephyrFlightAbility zephyrFlightAbility;
    private final FeatherFeetAbility featherFeetAbility;
    private transient boolean registered;

    @Inject
    private WindBlade(Champions champions, ItemFactory itemFactory, ZephyrFlightAbility zephyrFlightAbility) {
        super(champions, translatableName("champions.item.wind-blade.name"), model, ItemRarity.LEGENDARY, List.of(Group.MELEE, Group.RANGED));
        this.featherFeetAbility = new FeatherFeetAbility(itemFactory);
        this.zephyrFlightAbility = zephyrFlightAbility;

        // Add ability container
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.HOLD_RIGHT_CLICK, zephyrFlightAbility)
                .root(InteractionInputs.PASSIVE, featherFeetAbility)
                .build());

        addBaseComponent(TooltipSpriteComponent.of("\uE000"));
        addSerializableComponent(new TemperComponent(TemperProfile.medium().recoveringWhileGrounded()));
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);

        // Zephyr Flight
        zephyrFlightAbility.setFlightSpeed(config.getConfig("flightSpeed", 0.7, Double.class));
        zephyrFlightAbility.setClimbDrainMultiplier(config.getConfig("climbDrainMultiplier", 2.0, Double.class));
        getComponent(TemperComponent.class).ifPresent(temper -> temper.getProfile().configure(config));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                AetherCore aetherCore, FeatherOfZephyr featherOfZephyr,
                                DurakHandle durakHandle) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "F",
                "A",
                "D"
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('F', new RecipeIngredient(featherOfZephyr, 1));
        builder.setIngredient('A', new RecipeIngredient(aetherCore, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        registry.registerRecipe(new NamespacedKey("champions", "windblade"), builder.build());
    }
}
