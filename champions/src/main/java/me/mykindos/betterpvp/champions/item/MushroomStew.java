package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.RegenerationAbility;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class MushroomStew extends BaseItem implements ReloadHook {

    private static final ItemStack model;
    private final RegenerationAbility regenerationAbility;

    static {
        model = ItemStack.of(Material.MUSHROOM_STEW);
        //noinspection UnstableApiUsage
        model.setData(DataComponentTypes.MAX_STACK_SIZE, 64);
    }

    @Inject
    private MushroomStew(RegenerationAbility regenerationAbility) {
        super("Mushroom Stew", model, ItemGroup.CONSUMABLE, ItemRarity.UNCOMMON);
        this.regenerationAbility = regenerationAbility;
        regenerationAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(regenerationAbility).build());
    }

    @Override
    public void reload() {
        final ItemConfig config = ItemConfig.of(Champions.class, this);
        double duration = config.getConfig("duration", 5.0, Double.class);
        double cooldown = config.getConfig("cooldown", 8.0, Double.class);
        int level = config.getConfig("level", 2, Integer.class);
        regenerationAbility.setDuration(duration);
        regenerationAbility.setCooldown(cooldown);
        regenerationAbility.setLevel(level);
    }
}
