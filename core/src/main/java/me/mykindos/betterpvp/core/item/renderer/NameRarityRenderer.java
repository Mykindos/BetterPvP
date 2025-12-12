package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Optional;

/**
 * This class is responsible for rendering the names of items based on their rarity.
 */
public class NameRarityRenderer implements ItemNameRenderer {

    private final Component name;

    public NameRarityRenderer(String name) {
        this(Component.text(name));
    }

    public NameRarityRenderer(Component name) {
        this.name = name;
    }

    @Override
    public Component createName(ItemInstance item) {
        // Check for purity-based name color override
        final Optional<PurityComponent> purityComponent = item.getComponent(PurityComponent.class);
        if (purityComponent.isPresent()) {
            final TextColor nameColorOverride = purityComponent.get().getPurity().getNameColorOverride();
            if (nameColorOverride != null) {
                return name.applyFallbackStyle(nameColorOverride);
            }
        }

        // Default to rarity color
        return name.applyFallbackStyle(item.getRarity().getColor());
    }

}
