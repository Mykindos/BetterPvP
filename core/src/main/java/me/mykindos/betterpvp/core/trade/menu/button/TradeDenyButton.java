package me.mykindos.betterpvp.core.trade.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.trade.TradeCancelReason;
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
 * Ends the trade outright. Available in every state up to settlement.
 */
@RequiredArgsConstructor
public class TradeDenyButton extends AbstractItem {

    private final TradeManager tradeManager;
    private final TradeSession session;

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .material(Material.BARRIER)
                .itemModel(Key.key("betterpvp", "menu/icon/shadowed/cross_icon"))
                .displayName(Translations.component("core.trade.button.deny.name").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(Translations.component("core.trade.button.deny.lore").color(NamedTextColor.GRAY))
                .action(ClickActions.LEFT, Translations.component("core.trade.button.deny.action"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        tradeManager.cancel(session, TradeCancelReason.CANCELLED);
    }
}
