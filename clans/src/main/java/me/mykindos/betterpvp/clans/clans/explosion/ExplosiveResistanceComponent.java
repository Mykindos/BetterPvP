package me.mykindos.betterpvp.clans.clans.explosion;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Collections;
import java.util.List;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class ExplosiveResistanceComponent extends AbstractItemComponent implements LoreComponent {

    private final int resistance;

    public ExplosiveResistanceComponent(int resistance) {
        super("explosive_resistance");
        this.resistance = resistance;
    }

    public int getResistance() {
        return resistance;
    }

    @Override
    public ExplosiveResistanceComponent copy() {
        return new ExplosiveResistanceComponent(resistance);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        if (resistance <= 0) {
            return Collections.emptyList();
        }
        return List.of(
                Component.text("Explosive Resistance", TextColor.color(212, 212, 212), TextDecoration.BOLD),
                Component.empty()
                        .append(Component.text("<glyph:shield_icon>").font(NEXO))
                        .appendSpace()
                        .append(Component.text(resistance, TextColor.color(255, 153, 0)))
        );
    }

    @Override
    public int getRenderPriority() {
        return 10_000;
    }
}
