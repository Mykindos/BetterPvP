package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.CleanseAbility;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:purification_potion")
@EqualsAndHashCode(callSuper = false)
public class PurificationPotion extends BaseItem implements Reloadable {

    private static final ItemStack model;
    private final CleanseAbility cleanseAbility;

    static {
        model = ItemStack.of(Material.HONEY_BOTTLE);
        model.setData(DataComponentTypes.MAX_STACK_SIZE, 64);
    }

    @Inject
    private PurificationPotion(EffectManager effectManager) {
        super("Purification Potion", model, ItemGroup.CONSUMABLE, ItemRarity.UNCOMMON);
        this.cleanseAbility = new CleanseAbility(effectManager);
        this.cleanseAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(cleanseAbility).build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Champions.class, this);
        double duration = config.getConfig("duration", 1.5, Double.class);
        double cooldown = config.getConfig("cooldown", 10.0, Double.class);
        cleanseAbility.setDuration(duration);
        cleanseAbility.setCooldown(cooldown);
    }
}