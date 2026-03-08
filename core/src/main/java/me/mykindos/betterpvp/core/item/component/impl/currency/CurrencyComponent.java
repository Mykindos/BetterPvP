package me.mykindos.betterpvp.core.item.component.impl.currency;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;

/**
 * An attached component indicating what each unit of an itemstack is worth in the
 * server's currency
 */
@Getter
public class CurrencyComponent extends AbstractItemComponent implements LoreComponent {

    private final long value;

    public CurrencyComponent(long value) {
        super("currency");
        this.value = value;
    }

    @Override
    public ItemComponent copy() {
        return new CurrencyComponent(value);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        return List.of(Component.empty()
                .append(Component.text("Worth:", NamedTextColor.GRAY))
                .appendSpace()
                .append(Component.text(value + " " + CurrencyUtils.CURRENCY.toLowerCase(), TextColor.color(255, 235, 56)))
        );
    }

    @Override
    public int getRenderPriority() {
        return 0;
    }
}
