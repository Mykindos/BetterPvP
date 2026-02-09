package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.VolticBashAbility;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.StormsteelPlate;
import me.mykindos.betterpvp.core.item.impl.VolticShield;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("champions:thunderclap_aegis")
public class ThunderclapAegis extends WeaponItem implements Reloadable {

    private static final ItemStack model;

    static {
        model = Item.model(Material.SHIELD, "thunderclap_aegis", 1);
        model.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(Float.MAX_VALUE)
                .animation(ItemUseAnimation.BLOCK)
                .build());
        model.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
                .speedMultiplier(1f)
                .build());
    }

    private transient boolean registered;
    private final VolticBashAbility volticBashAbility;

    @Inject
    private ThunderclapAegis(Champions champions,
                            VolticBashAbility volticBashAbility,
                            ItemFactory itemFactory) {
        super(champions, "Thunderclap Aegis", model, ItemRarity.LEGENDARY);
        this.volticBashAbility = volticBashAbility;

        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.HOLD_RIGHT_CLICK, volticBashAbility)
                .build());
    }
    
    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Configure VolticBash ability
        volticBashAbility.setVelocity(config.getConfig("velocity", 0.8, Double.class));
        volticBashAbility.setMaxChargeTicks(config.getConfig("maxChargeTicks", 60, Integer.class));
        volticBashAbility.setEnergyOnCollide(config.getConfig("energyOnCollide", 25.0, Double.class));
        volticBashAbility.setChargeDamage(config.getConfig("chargeDamage", 7.0, Double.class));
        volticBashAbility.setEnergyPerTick(config.getConfig("energyPerTick", 1.0, Double.class));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                StormsteelPlate stormsteelPlate, VolticShield volticShield) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "SSS",
                "SVS",
                "SSS"
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('S', new RecipeIngredient(stormsteelPlate, 1));
        builder.setIngredient('V', new RecipeIngredient(volticShield, 1));
        registry.registerRecipe(new NamespacedKey("champions", "thunderclap_aegis"), builder.build());
    }
} 