package me.mykindos.betterpvp.core.trade.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.trade.TradeManager;
import me.mykindos.betterpvp.core.trade.TradeSession;
import me.mykindos.betterpvp.core.trade.TradeState;
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
 * Accept, then back out.
 * <p>
 * Once both sides have accepted this becomes the way out of the countdown, which is the whole point of
 * having one: the last thing a player pressed is always still undoable until the trade actually settles.
 */
@RequiredArgsConstructor
public class TradeAcceptButton extends AbstractItem {

    private final TradeManager tradeManager;
    private final TradeSession session;
    private final Player viewer;

    @Override
    public ItemProvider getItemProvider() {
        if (session.getState() == TradeState.ACKNOWLEDGING) {
            return ItemView.builder()
                    .material(Material.RED_CONCRETE)
                    .itemModel(Key.key("betterpvp", "menu/icon/shadowed/exclamation_mark_icon"))
                    .displayName(Translations.component("core.trade.button.accept.settling").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                    .lore(Component.empty())
                    .action(ClickActions.LEFT, Translations.component("core.trade.button.accept.back-out"))
                    .build();
        }

        if (session.offerOf(viewer.getUniqueId()).isAccepted()) {
            return ItemView.builder()
                    .material(Material.YELLOW_CONCRETE)
                    .itemModel(Key.key("betterpvp", "menu/icon/shadowed/question_mark_icon"))
                    .displayName(Translations.component("core.trade.button.accept.waiting").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                    .lore(Component.empty())
                    .action(ClickActions.LEFT, Translations.component("core.trade.button.accept.withdraw"))
                    .build();
        }

        return ItemView.builder()
                .material(Material.LIME_CONCRETE)
                .itemModel(Key.key("betterpvp", "menu/icon/shadowed/check_icon"))
                .displayName(Translations.component("core.trade.button.accept.name").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .lore(Component.empty())
                .action(ClickActions.LEFT, Translations.component("core.trade.button.accept.confirm"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (session.getState() == TradeState.ACKNOWLEDGING || session.offerOf(viewer.getUniqueId()).isAccepted()) {
            session.withdrawAcceptance(viewer.getUniqueId());
            return;
        }

        tradeManager.accept(session, viewer.getUniqueId());
    }
}
