package me.mykindos.betterpvp.core.menu.impl;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerColor;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class GuiSelectPattern extends AbstractGui implements Windowed {

    private static final int[] RESTRICTED_SLOTS = {0, 8, 22, 36, 44};

    private final Consumer<Pattern> callback;
    private final Component title;

    public GuiSelectPattern(Component title, Consumer<Pattern> callback) {
        super(9, 5);
        this.callback = callback;
        this.title = title;
        populateInventory();
    }

    public GuiSelectPattern(Consumer<Pattern> callback) {
        this(Component.text("Select a Pattern"), callback);
    }

    protected void populateInventory() {
        List<Integer> restricted = new ArrayList<>();
        for (int slot : RESTRICTED_SLOTS) {
            restricted.add(slot);
        }
        Iterator<PatternType> types = Arrays.stream(PatternType.values()).iterator();
        int slot = 0;
        while (slot < 45 && types.hasNext()) {
            if (restricted.contains(slot)) {
                slot++;
                continue;
            }
            PatternType type = types.next();
            if (type == PatternType.BASE) {
                slot--;
                slot++;
                continue;
            }

            Pattern pattern = new Pattern(DyeColor.BLACK, type);
            BannerWrapper banner = BannerWrapper.builder()
                    .baseColor(BannerColor.WHITE)
                    .pattern(pattern)
                    .build();

            String name = UtilFormat.cleanString(type.name());
            ItemView item = ItemView.builder()
                    .with(banner.get())
                    .displayName(Component.text("Select ", NamedTextColor.WHITE, TextDecoration.BOLD)
                            .append(Component.text(name, NamedTextColor.GREEN, TextDecoration.BOLD)))
                    .flag(ItemFlag.HIDE_ITEM_SPECIFICS)
                    .build();

            setItem(slot, new SimpleItem(item) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                    SoundEffect.HIGH_PITCH_PLING.play(player);
                    new GuiSelectColor(Component.text("Select Pattern Color"), dyeColor -> {
                        Pattern pattern = new Pattern(dyeColor, type);
                        callback.accept(pattern);
                    }).show(player);
                }
            });
            slot++;
        }

        this.setBackground(Menu.BACKGROUND_ITEM);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return title;
    }
}