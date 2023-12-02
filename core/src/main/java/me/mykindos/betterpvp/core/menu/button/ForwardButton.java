package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

public class ForwardButton extends PageItem {
    public ForwardButton() {
        super(true);
    }

    @Override
    public ItemProvider getItemProvider(PagedGui<?> gui) {
        final ItemView.ItemViewBuilder builder = ItemView.builder().material(Material.GREEN_STAINED_GLASS_PANE);
        if (gui.hasNextPage()) {
            builder.displayName(UtilMessage.deserialize("<green>Next Page <gray>(<white>%d</white>/%d)", gui.getCurrentPage() + 2, gui.getPageAmount()));
        } else {
            builder.displayName(Component.text("No next page", NamedTextColor.RED));
        }
        return builder.build();
    }
}