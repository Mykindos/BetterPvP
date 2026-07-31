package me.mykindos.betterpvp.core.trade.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.trade.TradeManager;
import me.mykindos.betterpvp.core.trade.TradeSession;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Nudges the other side to put more on the table.
 * <p>
 * Deliberately just a message: it carries no amount and changes no state, so it cannot be used to
 * pressure anyone into a deal they have already accepted.
 */
@RequiredArgsConstructor
public class RequestMoreButton extends AbstractItem {

    private final TradeManager tradeManager;
    private final TradeSession session;
    private final Player viewer;

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .material(Material.BELL)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/bell_icon"))
                .displayName(Translations.component("core.trade.button.request-more.name").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(Translations.component("core.trade.button.request-more.lore").color(NamedTextColor.GRAY))
                .action(ClickActions.LEFT, Translations.component("core.trade.button.request-more.action"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        tradeManager.requestMore(session, viewer);
    }
}
