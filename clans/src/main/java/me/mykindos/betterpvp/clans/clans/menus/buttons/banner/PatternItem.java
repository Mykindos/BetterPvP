package me.mykindos.betterpvp.clans.clans.menus.buttons.banner;

import me.mykindos.betterpvp.clans.clans.menus.BannerMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.impl.GuiSelectPattern;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PatternItem extends ControlItem<BannerMenu> {

    private final int index;
    private final Pattern pattern;

    public PatternItem(int index, Pattern pattern) {
        this.index = index;
        this.pattern = pattern;
    }

    @Override
    public ItemProvider getItemProvider(BannerMenu gui) {
        return ItemView.of(BannerWrapper.builder().pattern(pattern).build().get())
                .toBuilder()
                .displayName(Component.text("Pattern " + (index + 1), NamedTextColor.GREEN, TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ITEM_SPECIFICS)
                .action(ClickActions.LEFT, Component.text("Add Pattern Before"))
                .action(ClickActions.RIGHT, Component.text("Replace Pattern"))
                .action(ClickActions.SHIFT, Component.text("Remove Pattern"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final BannerWrapper.BannerBuilder builder = getGui().getBuilder();
        List<Pattern> patterns = new ArrayList<>(builder.build().getPatterns());
        boolean canAdd = patterns.size() < BannerMenu.MAX_PATTERNS; // Max
        if (ClickActions.RIGHT.accepts(clickType) && canAdd) {
            // Replace
            GuiSelectPattern guiSelectPattern = new GuiSelectPattern(replacement -> {
                patterns.remove(index);
                patterns.add(index, replacement);
                builder.clearPatterns();
                builder.patterns(patterns);

                getGui().update();
                getGui().show(player);
            });
            guiSelectPattern.show(player);
            SoundEffect.HIGH_PITCH_PLING.play(player);
        } else if (ClickActions.LEFT.accepts(clickType) && canAdd) {
            // Add before
            GuiSelectPattern guiSelectPattern = new GuiSelectPattern(replacement -> {
                patterns.add(index, replacement);
                builder.clearPatterns();
                builder.patterns(patterns);

                getGui().update();
                getGui().show(player);
            });
            guiSelectPattern.show(player);
            SoundEffect.HIGH_PITCH_PLING.play(player);
        } else if (ClickActions.SHIFT.accepts(clickType)) {
            // Remove
            patterns.remove(index);
            builder.clearPatterns();
            builder.patterns(patterns);

            getGui().update();
            SoundEffect.HIGH_PITCH_PLING.play(player);
        } else {
            SoundEffect.WRONG_ACTION.play(player);
        }
    }
}
