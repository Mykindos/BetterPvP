package me.mykindos.betterpvp.core.trade.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.trade.TradeCurrency;
import me.mykindos.betterpvp.core.trade.TradeOffer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * What the other side is putting up of one currency. Display only - the opposite offer is never
 * editable from this window.
 */
@RequiredArgsConstructor
public class CurrencyDemandButton extends AbstractItem {

    private final TradeOffer opponentOffer;
    private final TradeCurrency currency;

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .material(currency.getIcon())
                .displayName(currency.getDisplayName().decorate(TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(Translations.component("core.trade.button.currency.receiving").color(NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text(opponentOffer.getCurrency(currency), NamedTextColor.WHITE)))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Intentionally inert.
    }
}
