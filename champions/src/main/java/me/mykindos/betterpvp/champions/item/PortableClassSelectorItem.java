package me.mykindos.betterpvp.champions.item;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.ArmorStorageEditAbility;
import me.mykindos.betterpvp.champions.item.ability.PortableClassAbility;
import me.mykindos.betterpvp.champions.item.component.storage.ArmorStorageComponent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;

public abstract class PortableClassSelectorItem extends BaseItem implements Reloadable {

    private transient final Champions plugin;
    private final PortableClassAbility ability;

    protected PortableClassSelectorItem(Champions champions, Role role, Material material) {
        super("Portable " + role.getName() + " Selector",
                Item.builder(material).maxStackSize(1).build(),
                ItemGroup.CONSUMABLE,
                ItemRarity.UNCOMMON);
        this.plugin = champions;
        this.ability = new PortableClassAbility(role);
        this.ability.setConsumesItem(true);
        addSerializableComponent(new ArmorStorageComponent(role, false));
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, this.ability)
                .root(InteractionInputs.SHIFT_RIGHT_CLICK, new ArmorStorageEditAbility())
                .build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(plugin, this);
        ability.setCastTime(config.getConfig("cast-time", 2.0, Double.class));
    }
}
