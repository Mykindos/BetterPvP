package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.impl.ability.EnhancedMiningAbility;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class RunedPickaxe extends BaseItem implements ReloadHook {

    private static final ItemStack model = ItemView.builder()
            .material(Material.LEATHER_HORSE_ARMOR)
            .itemModel(Material.MUSIC_DISC_WARD.key())
            .customModelData(1)
            .build().get();

    private final EnhancedMiningAbility ability;
    @EqualsAndHashCode.Exclude
    private final Champions champions;

    @Inject
    private RunedPickaxe(Champions champions) {
        super("Runed Pickaxe", model, ItemGroup.TOOL, ItemRarity.EPIC);
        this.champions = champions;

        // Create and add the mining speed ability
        this.ability = new EnhancedMiningAbility();
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(ability)
                .build());
    }

    @Override
    public void reload() {
        final ItemConfig config = ItemConfig.of(champions.getClass(), this);
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
}
