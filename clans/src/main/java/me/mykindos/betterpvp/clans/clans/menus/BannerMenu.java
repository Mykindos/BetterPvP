package me.mykindos.betterpvp.clans.clans.menus;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.buttons.banner.AddPatternItem;
import me.mykindos.betterpvp.clans.clans.menus.buttons.banner.CancelItem;
import me.mykindos.betterpvp.clans.clans.menus.buttons.banner.PatternItem;
import me.mykindos.betterpvp.clans.clans.menus.buttons.banner.PreviewItem;
import me.mykindos.betterpvp.clans.clans.menus.buttons.banner.SaveItem;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerWrapper;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BannerMenu extends AbstractGui implements Windowed {

    public static final int MAX_PATTERNS = 24;

    @Getter
    @Setter
    private BannerWrapper.BannerBuilder builder;
    private static final SimpleItem missingPatternItem = ItemView.builder()
            .material(Material.RED_STAINED_GLASS_PANE)
            .displayName(Component.text(""))
            .build().toSimpleItem();

    public BannerMenu(Clan clan, Windowed previous, Runnable onBack) {
        super(9, 6);
        this.builder = clan.getBanner().clone().toBuilder();

        // Left-side column of buttons
        setItem(10, new PreviewItem());
        setItem(28, new SaveItem(clan));
        setItem(37, new CancelItem(clan));
        setItem(53, new BackButton(previous, onBack));
        setPatterns();

        // Right-side
        setBackground(Menu.BACKGROUND_ITEM);
    }

    public BannerMenu(Clan clan, Windowed previous) {
        this(clan, previous, null);
    }

    public void update() {
        updateControlItems();
        setPatterns();
    }

    private void setPatterns() {
        // Loop between rows 2 and 5 from column 4 to 9
        // All patterns are added in order.
        // If there are fewer patterns than the max, then an 'Add Pattern' item is added.
        // If there are any remaining slots, they will become 'Missing Pattern' items.
        List<Pattern> patterns = builder.build().getPatterns();
        for (int row = 2; row <= 5; row++) {
            for (int column = 4; column <= 9; column++) {
                int index = (row - 2) * 6 + (column - 4);
                AbstractItem item;
                if (index < patterns.size()) {
                    Pattern pattern = patterns.get(index);
                    item = new PatternItem(index, pattern);
                } else if (index == patterns.size()) {
                    item = new AddPatternItem();
                } else {
                    item = missingPatternItem;
                }

                // 0 - based
                setItem(column - 1, row - 1, item);
            }
        }
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Banner Editor");
    }
}