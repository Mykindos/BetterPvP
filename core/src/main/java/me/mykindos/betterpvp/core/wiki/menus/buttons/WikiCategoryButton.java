package me.mykindos.betterpvp.core.wiki.menus.buttons;

import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.wiki.types.WikiCategory;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.List;

public class WikiCategoryButton extends AbstractItem {
    private final WikiCategory category;
    private final String title;
    private final List<String> description;
    private final Material material;
    private final int modelData;

    /**
     *
     * @param category the category this button represents
     * @param title the mini-message formatted title
     * @param description a list of strings that are mini-message formatted
     * @param material the material
     * @param modelData the model data for the material
     */
    public WikiCategoryButton(WikiCategory category, String title, List<String> description, Material material, int modelData) {
        this.category = category;
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

    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

        //Not used
    }
}
