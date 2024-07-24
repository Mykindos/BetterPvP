package me.mykindos.betterpvp.core.utilities.model.description;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import me.mykindos.betterpvp.core.inventory.item.Click;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a description of something that can be displayed in a UI.
 */
@Value
@Builder(toBuilder = true)
public class Description {

    /**
     * The icon to display with description with in a UI.
     */
    @NotNull
    ItemProvider icon;

    /**
     * A name to value map of properties to display with the description in a UI.
     * Properties are only used in some UIs, and they usually override the item's lore.
     */
    @Nullable @Singular
    Map<String, Component> properties;

    /**
     * The click consumer for whenever somebody clicks this description in a UI.
     * This is not used by the description itself, but by the UI that displays it.
     * A null value indicates that the description is not clickable.
     */
    @Nullable
    Consumer<Click> clickFunction;

    public static class DescriptionBuilder {

        public DescriptionBuilder icon(ItemStack icon) {
            this.icon = ItemView.of(icon);
            return this;
        }

        public DescriptionBuilder icon(ItemProvider icon) {
            this.icon = icon;
            return this;
        }
    }

}
