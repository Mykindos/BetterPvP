package me.mykindos.betterpvp.core.trade.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.trade.TradeOffer;
import me.mykindos.betterpvp.core.trade.TradeSession;
import me.mykindos.betterpvp.core.trade.TradeState;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * The clock between the two halves of the trade window: how long is left, and whether the other side
 * has accepted.
 */
@RequiredArgsConstructor
public class TradeStatusButton extends AbstractItem {

    private final TradeSession session;
    private final UUID viewer;

    @Override
    public ItemProvider getItemProvider() {
        final TradeOffer opponent = session.opponentOf(viewer);

        if (session.getState() == TradeState.ACKNOWLEDGING) {
            final long seconds = (session.getAcknowledgeRemaining() + 999L) / 1000L;
            return ItemView.builder()
                    .material(Material.CLOCK)
                    .itemModel(Key.key("betterpvp", "menu/icon/regular/hourglass_icon"))
                    .displayName(Translations.component("core.trade.button.status.settling").color(NamedTextColor.GREEN))
                    .lore(Component.empty())
                    .lore(Translations.component("core.trade.button.status.settling-in").color(NamedTextColor.GRAY)
                            .append(Component.space())
                            .append(Component.text(seconds + "s", NamedTextColor.WHITE)))
                    .build();
        }

        return ItemView.builder()
                .material(Material.CLOCK)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/hourglass_icon"))
                .displayName(Translations.component("core.trade.button.status.negotiating").color(NamedTextColor.YELLOW))
                .lore(Component.empty())
                .lore(Translations.component("core.trade.button.status.remaining").color(NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text(session.getNegotiationRemaining() / 1000L + "s", NamedTextColor.WHITE)))
                .lore(opponent.isAccepted()
                        ? Translations.component("core.trade.button.status.they-accepted").color(NamedTextColor.GREEN)
                        : Translations.component("core.trade.button.status.they-have-not").color(NamedTextColor.RED))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Intentionally inert.
    }
}
