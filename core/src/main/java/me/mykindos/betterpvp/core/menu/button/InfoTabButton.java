package me.mykindos.betterpvp.core.menu.button;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.LinkedList;
import java.util.Map;

/**
 * Button that shows a description and wiki entries.
 */
@Getter
@Builder
public class InfoTabButton extends AbstractItem {

    private final @Nullable Component description;

    @Singular("wikiEntry")
    private final Map<String, URL> wikiEntries;

    private final @Nullable ItemStack icon;

    @Override
    public ItemProvider getItemProvider() {
        final ItemView.ItemViewBuilder builder;

        if (icon == null) {
            builder = ItemView.builder();
            builder.material(Material.PAPER);
            builder.itemModel(Key.key("betterpvp", "menu/tab/info"));
        } else {
            builder = ItemView.of(icon).toBuilder();
        }

        boolean started = false;
        if (description != null) {
            final Component descriptionComponent = description.applyFallbackStyle(NamedTextColor.WHITE);
            final LinkedList<Component> components = new LinkedList<>(ComponentWrapper.wrapLine(descriptionComponent, 40, true));
            Preconditions.checkState(!components.isEmpty(), "Description must have at least one line");

            // This first component will be the display name
            builder.displayName(components.poll());
            builder.lore(components);
            started = true;
        }

        if (wikiEntries.isEmpty()) {
            return builder.build();
        }

        builder.lore(Component.empty());
        final TextComponent header = Component.empty()
                .append(Component.text("<glyph:book_icon>").font(Resources.Font.NEXO))
                .appendSpace()
                .append(Component.text("Relevant articles:", TextColor.color(191, 191, 191)));
        if (!started) builder.displayName(header);
        else builder.lore(header);

        for (String wikiEntry : wikiEntries.keySet()) {
            URL url = wikiEntries.get(wikiEntry);
            String prefix = wikiEntry.isEmpty() ? "" : wikiEntry + ": ";
            final Component text = Component.text("● " + prefix, TextColor.color(191, 191, 191))
                    .append(Component.text(url.toString(), TextColor.color(201, 165, 0)));
            builder.lore(text);
        }

        builder.action(ClickActions.ALL, Component.empty()
                .append(Component.text("<glyph:magnifying_glass_icon>").font(Resources.Font.NEXO))
                .append(Component.text("View Articles")));
        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType,
                            @NotNull Player player,
                            @NotNull InventoryClickEvent event) {
        if (wikiEntries.isEmpty()) {
            return;
        }

        UtilMessage.message(player, Component.empty());
        UtilMessage.message(player, Component.empty());
        UtilMessage.message(player, Component.empty());
        for (String wikiEntry : wikiEntries.keySet()) {
            URL url = wikiEntries.get(wikiEntry);
            String prefix = wikiEntry.isEmpty() ? "" : wikiEntry + ": ";
            UtilMessage.message(player, Component.empty()
                    .append(Component.text("<glyph:book_open_icon_shadowed>").font(Resources.Font.NEXO))
                    .appendSpace()
                    .append(Component.text(prefix, NamedTextColor.WHITE))
                    .append(Component.text(url.toString(), TextColor.color(255, 225, 33), TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(Component.text("<glyph:check_icon>").font(Resources.Font.NEXO)))
                            .clickEvent(ClickEvent.openUrl(url))));
        }
        UtilMessage.message(player, Component.empty());
        UtilMessage.message(player, Component.empty());
        UtilMessage.message(player, Component.empty());

        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 0.5f).play(player);
        new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f).play(player);
        player.closeInventory();
    }
}
