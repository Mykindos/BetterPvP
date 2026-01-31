package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.RegenerationAbility;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:mushroom_stew")
@FallbackItem(Material.MUSHROOM_STEW)
@EqualsAndHashCode(callSuper = false)
public class MushroomStew extends BaseItem implements Reloadable {

    private static final ItemStack model;
    private final RegenerationAbility regenerationAbility;

    static {
        model = ItemStack.of(Material.MUSHROOM_STEW);
        model.setData(DataComponentTypes.MAX_STACK_SIZE, 64);
        model.unsetData(DataComponentTypes.CONSUMABLE);
    }

    @Inject
    private MushroomStew(RegenerationAbility regenerationAbility) {
        super("Mushroom Stew", model, ItemGroup.CONSUMABLE, ItemRarity.UNCOMMON);
        this.regenerationAbility = regenerationAbility;
        regenerationAbility.setConsumesItem(true);
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, regenerationAbility)
                .build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Champions.class, this);
        double duration = config.getConfig("duration", 5.0, Double.class);
        double cooldown = config.getConfig("cooldown", 8.0, Double.class);
        int level = config.getConfig("level", 2, Integer.class);
        regenerationAbility.setDuration(duration);
        regenerationAbility.setCooldown(cooldown);
        regenerationAbility.setLevel(level);
    }
}
