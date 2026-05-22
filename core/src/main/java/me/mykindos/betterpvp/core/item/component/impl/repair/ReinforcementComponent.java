package me.mykindos.betterpvp.core.item.component.impl.repair;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/**
 * Marks an item as a "Reinforcement" — the consumable that authorises repairing gear
 * of a given {@link ItemRarity} tier on the anvil.
 * <br>
 * This is a non-persistent definition component (added via {@code addBaseComponent}),
 * not per-instance state. Because {@code ItemInstance} copies every base component onto
 * each instance, the tier is discoverable at runtime via
 * {@code instance.getComponent(ReinforcementComponent.class)} — no separate registry or
 * registration step is required.
 */
@Getter
public class ReinforcementComponent extends AbstractItemComponent implements LoreComponent {

    private final ItemRarity tier;

    public ReinforcementComponent(ItemRarity tier) {
        super("reinforcement");
        this.tier = tier;
    }

    @Override
    public ReinforcementComponent copy() {
        return new ReinforcementComponent(tier);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        return List.of(
                Component.text("Repairs ", NamedTextColor.GRAY)
                        .append(Component.text(tier.getName(), tier.getColor()))
                        .append(Component.text(" gear in an", NamedTextColor.GRAY))
                        .append(Component.text(" Anvil", NamedTextColor.WHITE))
                        .append(Component.text(".", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false)
        );
    }

    @Override
    public int getRenderPriority() {
        return 0;
    }
}
