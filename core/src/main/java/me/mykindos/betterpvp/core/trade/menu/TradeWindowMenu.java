package me.mykindos.betterpvp.core.trade.menu;

import lombok.NonNull;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.trade.TradeCancelReason;
import me.mykindos.betterpvp.core.trade.TradeCurrency;
import me.mykindos.betterpvp.core.trade.TradeManager;
import me.mykindos.betterpvp.core.trade.TradeOffer;
import me.mykindos.betterpvp.core.trade.TradeSession;
import me.mykindos.betterpvp.core.trade.menu.button.CurrencyDemandButton;
import me.mykindos.betterpvp.core.trade.menu.button.CurrencyOfferButton;
import me.mykindos.betterpvp.core.trade.menu.button.RequestMoreButton;
import me.mykindos.betterpvp.core.trade.menu.button.TradeAcceptButton;
import me.mykindos.betterpvp.core.trade.menu.button.TradeDenyButton;
import me.mykindos.betterpvp.core.trade.menu.button.TradeStatusButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The trade itself, from one player's point of view.
 * <p>
 * Each side of a trade gets its own instance: the left half is always yours and editable, the right
 * half is always theirs and inert. Both instances watch the same {@link TradeSession}, so a change
 * either player makes redraws the other's window on the same tick it happens - which is what makes the
 * "any change clears both acceptances" rule visible rather than merely true.
 */
public class TradeWindowMenu extends AbstractGui implements Windowed {

    private static final int OFFER_WIDTH = 4;
    private static final int OFFER_HEIGHT = TradeOffer.SLOTS / OFFER_WIDTH;
    private static final int OPPONENT_COLUMN = 5;
    private static final int ERROR_SLOT = 4;
    private static final int NOTICE_SLOT = 22;
    private static final int STATUS_SLOT = 40;

    /** How long an input error stays on screen before the divider goes back to normal. */
    private static final long ERROR_MILLIS = 4000L;
    private static final int ACCEPT_SLOT = 46;
    private static final int NUDGE_SLOT = 49;
    private static final int DENY_SLOT = 52;

    private final TradeManager tradeManager;
    private final TradeSession session;
    private final TradeOffer ownOffer;
    private final TradeOffer opponentOffer;

    /** How many slots at each end of the currency row are taken by currency buttons. */
    private final int currencyCount;

    private final List<Item> liveButtons = new ArrayList<>();

    private Component error;
    private long errorUntil;

    public TradeWindowMenu(@NotNull TradeManager tradeManager, @NotNull TradeSession session, @NotNull Player viewer) {
        super(9, 6);
        this.tradeManager = tradeManager;
        this.session = session;
        this.opponentOffer = session.opponentOf(viewer.getUniqueId());
        this.ownOffer = session.offerOf(viewer.getUniqueId());

        fill(0, 54, Menu.BACKGROUND_GUI_ITEM, true);
        fillRectangle(0, 0, OFFER_WIDTH, ownOffer.getItems(), Menu.INVISIBLE_BACKGROUND_ITEM, true);

        addLiveButton(STATUS_SLOT, new TradeStatusButton(session, viewer.getUniqueId()));
        addLiveButton(ACCEPT_SLOT, new TradeAcceptButton(tradeManager, session, viewer));
        addLiveButton(NUDGE_SLOT, new RequestMoreButton(tradeManager, session, viewer));
        addLiveButton(DENY_SLOT, new TradeDenyButton(tradeManager, session));

        int index = 0;
        for (TradeCurrency currency : tradeManager.getCurrencyRegistry().all()) {
            if (index >= OFFER_WIDTH) {
                break;
            }
            addLiveButton(36 + index, new CurrencyOfferButton(session, ownOffer, currency, tradeManager.getGamer(viewer), this::showError));
            addLiveButton(44 - index, new CurrencyDemandButton(opponentOffer, currency));
            index++;
        }
        this.currencyCount = index;

        session.observe(this::refresh);
        refresh();
    }

    private void addLiveButton(int slot, @NotNull Item button) {
        setItem(slot, button);
        liveButtons.add(button);
    }

    /**
     * Puts a complaint about the viewer's last input on the divider above the two offers. It clears
     * itself on the next refresh after {@link #ERROR_MILLIS}, so nothing has to remember to remove it.
     */
    public void showError(@NotNull Component message) {
        this.error = message;
        this.errorUntil = System.currentTimeMillis() + ERROR_MILLIS;
        refresh();
    }

    /**
     * Redraws everything that depends on the session: the mirror of the other side's goods, the clock,
     * the standing ask for more, and the accept button's current meaning.
     */
    public void refresh() {
        if (error != null && System.currentTimeMillis() > errorUntil) {
            error = null;
        }

        setItem(ERROR_SLOT, error == null
                ? Menu.BACKGROUND_GUI_ITEM
                : new SimpleItem(ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/exclamation_mark_icon"))
                        .displayName(error.color(NamedTextColor.RED))
                        .build()));

        final boolean nudged = opponentOffer.getPlayer().equals(session.getNudgedBy());
        setItem(NOTICE_SLOT, !nudged
                ? Menu.BACKGROUND_GUI_ITEM
                : new SimpleItem(ItemView.builder()
                .material(Material.BELL)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/bell_icon"))
                .displayName(Translations.component("core.trade.notice.nudged").color(NamedTextColor.GOLD))
                .lore(Component.empty())
                .lore(Translations.component("core.trade.notice.nudged.lore").color(NamedTextColor.GRAY))
                .build()));

        final ItemStack[] theirs = opponentOffer.getItems().getItems();
        for (int row = 0; row < OFFER_HEIGHT; row++) {
            for (int column = 0; column < OFFER_WIDTH; column++) {
                final ItemStack item = theirs[row * OFFER_WIDTH + column];
                final int slot = row * 9 + OPPONENT_COLUMN + column;
                setItem(slot, item == null || item.getType().isAir()
                        ? pane(Material.WHITE_STAINED_GLASS_PANE)
                        : new SimpleItem(ItemView.of(item).toBuilder().build()));
            }
        }

        // Each side's half of the currency row doubles as its acceptance light. The currency buttons
        // sit at the outer ends, so only the slots between them are repainted.
        for (int slot = 36 + currencyCount; slot <= 39; slot++) {
            setItem(slot, acceptancePane(ownOffer.isAccepted()));
        }
        for (int slot = 41; slot <= 44 - currencyCount; slot++) {
            setItem(slot, acceptancePane(opponentOffer.isAccepted()));
        }

        for (Item button : liveButtons) {
            button.notifyWindows();
        }
    }

    private Item acceptancePane(boolean accepted) {
        return pane(accepted ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE);
    }

    private Item pane(@NotNull Material material) {
        return new SimpleItem(ItemView.builder().material(material).hideTooltip(true).displayName(Component.empty()).build());
    }

    @Override
    public Window show(@NonNull Player player) {
        final Window window = Windowed.super.show(player);

        // Closing the window is a way out of the trade, so it has to end it rather than leave a session
        // running with nobody looking at it. Cancelling a session that has already settled or been
        // cancelled is a no-op, so this fires harmlessly on every normal exit too.
        window.addCloseHandler(() -> tradeManager.cancel(session, TradeCancelReason.CLOSED));

        return window;
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("core.trade.menu.window.title");
    }
}
