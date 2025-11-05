package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipeRegistry;
import me.mykindos.betterpvp.core.imbuement.StandardImbuementRecipe;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.MagicSeal;
import me.mykindos.betterpvp.core.item.impl.OverchargedCrystal;
import me.mykindos.betterpvp.core.item.impl.ability.EnhancedMiningAbility;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class RunedPickaxe extends BaseItem implements ReloadHook {

    private transient boolean registered;

    private final EnhancedMiningAbility ability;
    @EqualsAndHashCode.Exclude
    private final Champions champions;

    @Inject
    private RunedPickaxe(Champions champions) {
        super("Runed Pickaxe",
                Item.model(Material.DIAMOND_PICKAXE, "runed_pickaxe"),
                ItemGroup.TOOL,
                ItemRarity.EPIC);
        this.champions = champions;

        // Create and add the mining speed ability
        this.ability = new EnhancedMiningAbility();
        addSerializableComponent(new RuneContainerComponent(3));
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(ability)
                .build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(champions.getClass(), this);
        double miningSpeed = config.getConfig("miningSpeed", 30.0, Double.class);
        ability.setMiningSpeed(miningSpeed);
    }

    /**
     * Apply the mining speed to any item created from this BaseItem
     */
    @Override
    public @NotNull ItemStack getModel() {
        ItemStack model = super.getModel().clone();
        ability.applyMiningSpeed(model);
        return model;
    }

    @Inject
    private void registerRecipe(ImbuementRecipeRegistry registry, ItemFactory itemFactory,
                                MagicSeal magicSeal, OverchargedCrystal overchargedCrystal) {
        if (registered) return;
        registered = true;
        final BaseItem diamondPickaxe = itemFactory.getFallbackItem(Material.DIAMOND_PICKAXE);
        final Map<BaseItem, Integer> ingredients = Map.of(
                magicSeal, 1,
                overchargedCrystal, 1,
                diamondPickaxe, 1
        );
        final StandardImbuementRecipe recipe = new StandardImbuementRecipe(ingredients, this, itemFactory);
        registry.registerRecipe(new NamespacedKey("champions", "runed_pickaxe"), recipe);
    }
}
