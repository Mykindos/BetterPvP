package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.SpeedBoostAbility;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:rabbit_stew")
@EqualsAndHashCode(callSuper = false)
public class RabbitStew extends BaseItem implements Reloadable {

    private static final ItemStack model;
    private final SpeedBoostAbility speedBoostAbility;

    static {
        model = ItemStack.of(Material.RABBIT_STEW);
        model.setData(DataComponentTypes.MAX_STACK_SIZE, 64);
        model.unsetData(DataComponentTypes.CONSUMABLE);
    }

    @Inject
    private RabbitStew(SpeedBoostAbility speedBoostAbility) {
        super("Rabbit Stew", model, ItemGroup.CONSUMABLE, ItemRarity.UNCOMMON);
        this.speedBoostAbility = speedBoostAbility;
        this.speedBoostAbility.setConsumesItem(true);
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, speedBoostAbility)
                .build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Champions.class, this);
        double duration = config.getConfig("duration", 7.0, Double.class);
        double cooldown = config.getConfig("cooldown", 10.0, Double.class);
        int level = config.getConfig("level", 1, Integer.class);
        speedBoostAbility.setDuration(duration);
        speedBoostAbility.setCooldown(cooldown);
        speedBoostAbility.setLevel(level);
    }
}
