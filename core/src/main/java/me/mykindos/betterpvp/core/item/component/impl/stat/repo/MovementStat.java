package me.mykindos.betterpvp.core.item.component.impl.stat.repo;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.PercentageItemStat;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
public class MovementStat extends PercentageItemStat {

    public MovementStat(double percentage) {
        super("move-speed", "Movement", "Increases the walking speed of the holder.", percentage);
    }

    @Override
    public void onApply(Item item, ItemStack stack) {
        stack.editMeta(meta -> meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, getModifier()));
    }

    @Override
    public void onRemove(Item item, ItemStack stack) {
        stack.editMeta(meta -> meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED, getModifier()));
    }

    private @NotNull AttributeModifier getModifier() {
        return new AttributeModifier(getKey(), getValue(), AttributeModifier.Operation.ADD_NUMBER);
    }

    @Override
    public MovementStat copy() {
        return new MovementStat(getValue());
    }

    @Override
    public MovementStat withValue(Double newValue) {
        return new MovementStat(newValue);
    }
}
