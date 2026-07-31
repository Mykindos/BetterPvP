package me.mykindos.betterpvp.core.trade.menu;

import lombok.NonNull;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.filter.NameSearchButton;
import me.mykindos.betterpvp.core.menu.impl.HorizontalScrollGui;
import me.mykindos.betterpvp.core.trade.TradeManager;
import me.mykindos.betterpvp.core.trade.TradeRequest;
import me.mykindos.betterpvp.core.trade.menu.button.IncomingRequestButton;
import me.mykindos.betterpvp.core.trade.menu.button.TraderButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The broker's list: who is here, and who wants to trade with you.
 * <p>
 * Two bands, each scrolling sideways on its own. The top band is people who have already asked the
 * viewer, so an answer is never buried behind a long list of strangers; the lower band is everyone
 * else at the broker, searchable by name.
 */
public class TradeBrowserMenu extends AbstractGui implements Windowed {

    private static final int SEARCH_SLOT = 13;

    private final TradeManager tradeManager;
    private final Player viewer;

    private final HorizontalScrollGui requestBand = new HorizontalScrollGui(9, 1);
    private final HorizontalScrollGui traderBand = new HorizontalScrollGui(9, 3);

    private String nameSearch;

    public TradeBrowserMenu(@NotNull TradeManager tradeManager, @NotNull Player viewer) {
        super(9, 6);
        this.tradeManager = tradeManager;
        this.viewer = viewer;

        fillRectangle(0, 0, requestBand, true);
        fillRectangle(0, 2, traderBand, true);

        fill(9, 18, Menu.BACKGROUND_GUI_ITEM, true);
        fill(45, 54, Menu.BACKGROUND_GUI_ITEM, true);
        setItem(SEARCH_SLOT, new NameSearchButton(Translations.component("core.menu.search.subject.players"), () -> nameSearch, search -> {
            nameSearch = search;
            refresh();
        }));

        refresh();
    }

    /**
     * Rebuilds both bands from the manager's current view of who is available.
     */
    public void refresh() {
        final List<Item> requests = new ArrayList<>();
        for (TradeRequest request : tradeManager.getIncomingRequests(viewer.getUniqueId())) {
            final Player requester = Bukkit.getPlayer(request.getRequester());
            if (requester != null) {
                requests.add(new IncomingRequestButton(tradeManager, viewer, requester));
            }
        }
        requestBand.setItems(requests);

        final String query = nameSearch == null ? null : nameSearch.toLowerCase(Locale.ROOT);
        final List<Item> traders = new ArrayList<>();
        for (Player trader : tradeManager.getAvailableTraders(viewer.getUniqueId())) {
            if (query != null && !trader.getName().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            traders.add(new TraderButton(tradeManager, viewer, trader));
        }
        traderBand.setItems(traders);
    }

    @Override
    public Window show(@NonNull Player player) {
        final Window window = Windowed.super.show(player);

        // Registering on show rather than in the constructor keeps the two facts identical: being in
        // the manager's list and having this menu open. Closing it - including when accepting a request
        // replaces it with the trade window - takes the viewer straight back out of the list.
        tradeManager.addBrowser(player.getUniqueId(), this::refresh);
        window.addCloseHandler(() -> tradeManager.removeBrowser(player.getUniqueId()));

        return window;
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("core.trade.menu.browser.title");
    }
}
