package me.mykindos.betterpvp.core.wiki.menus.buttons;

import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.List;

public class StaticWikiDescriptionButton extends AbstractItem {

    private final String title;
    private final List<String> description;
    private final Material material;
    private final int modelData;

    public StaticWikiDescriptionButton(String title, List<String> description, Material material, int modelData) {
        this.title = title;
        this.description = description;
        this.material = material;
        this.modelData = modelData;
    }
    @Override
    public ItemProvider getItemProvider() {
        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder().material(material).customModelData(modelData)
                .displayName(MiniMessage.miniMessage().deserialize(title))
                .frameLore(true);
        for (String line : description) {
            itemViewBuilder.lore(MiniMessage.miniMessage().deserialize(line));
        }
        return itemViewBuilder.build();

    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        //Not used
    }
}

