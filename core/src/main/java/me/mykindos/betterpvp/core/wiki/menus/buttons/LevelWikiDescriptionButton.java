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
import java.util.function.Function;

public class LevelWikiDescriptionButton extends AbstractItem {

    private final Function<Integer, String> titleFunction;
    private final Function<Integer,List<String>> descriptionFunction;
    private final Function<Integer, Material> materialFunction;
    private final Function<Integer, Integer> modelDataFunction;
    private final int maxLevel;
    private int level;

    /**
     * @param titleFunction
     * @param descriptionFunction
     * @param materialFunction
     * @param modelDataFunction
     * @param maxLevel
     */
    public LevelWikiDescriptionButton(Function<Integer, String> titleFunction,
                                      Function<Integer,List<String>> descriptionFunction,
                                      Function<Integer, Material> materialFunction,
                                      Function<Integer, Integer> modelDataFunction,
                                      int maxLevel) {
        this.titleFunction = titleFunction;
        this.descriptionFunction = descriptionFunction;
        this.materialFunction = materialFunction;
        this.modelDataFunction = modelDataFunction;
        this.maxLevel = maxLevel;
        this.level = 1;
    }
    @Override
    public ItemProvider getItemProvider() {
        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder().material(materialFunction.apply(level)).customModelData(modelDataFunction.apply(level))
                .displayName(MiniMessage.miniMessage().deserialize(titleFunction.apply(level)))
                .frameLore(true);
        for (String line : descriptionFunction.apply(level)) {
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

