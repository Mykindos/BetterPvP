package me.mykindos.betterpvp.core.trade.menu;

import lombok.NonNull;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.profiles.PlayerProfiles;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.trade.TradeManager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * The screen between asking someone to trade and hearing back.
 * <p>
 * It exists so a request is never a message that scrolls away: the asker is held on a screen that can
 * only end one of three ways, and each of the three is shown here rather than inferred from silence.
 */
public class TradeWaitingMenu extends AbstractGui implements Windowed {

    private static final int HEAD_SLOT = 22;

    /** How long the red failure state is left on screen before the broker list comes back. */
    private static final long FAILURE_TICKS = 60L;

    private final TradeManager tradeManager;
    private final Player viewer;
    private final Player trader;

    private boolean failed;
    private boolean closed;

    public TradeWaitingMenu(@NotNull TradeManager tradeManager, @NotNull Player viewer, @NotNull Player trader) {
        super(9, 5);
        this.tradeManager = tradeManager;
        this.viewer = viewer;
        this.trader = trader;

        render(Menu.BACKGROUND_ITEM, Translations.component("core.trade.waiting.name").color(NamedTextColor.YELLOW));
    }

    private void render(@NotNull ItemProvider background, @NotNull Component status) {
        fill(0, 45, new SimpleItem(background), true);

        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(PlayerProfiles.CACHE.get(trader.getUniqueId(), key -> trader.getPlayerProfile()));
        head.setItemMeta(meta);

        setItem(HEAD_SLOT, new SimpleItem(ItemView.of(head).toBuilder()
                .displayName(Component.text(trader.getName(), NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(status)
                .build()));
    }

    /**
     * Shows why the request ended and sends the viewer back to the broker list a moment later.
     */
    private void fail(boolean timedOut) {
        failed = true;

        // The reason goes on the panes as well as the head, so it is readable wherever the cursor
        // happens to be rather than only when hovering the one slot in the middle.
        final Component reason = Translations
                .component(timedOut ? "core.trade.waiting.timed-out" : "core.trade.waiting.rejected")
                .color(NamedTextColor.RED);

        render(ItemView.builder().material(Material.RED_STAINED_GLASS_PANE).displayName(reason).build(), reason);
        new SoundEffect(Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f).play(viewer);

        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
            // Closing the screen is a decision to stop trading, so it beats the scheduled return.
            if (!closed && viewer.isOnline()) {
                tradeManager.openBroker(viewer);
            }
        }, FAILURE_TICKS);
    }

    @Override
    public Window show(@NonNull Player player) {
        final Window window = Windowed.super.show(player);

        tradeManager.addWaiter(player.getUniqueId(), this::fail);
        window.addCloseHandler(() -> {
            closed = true;
            tradeManager.removeWaiter(player.getUniqueId());

            // Walking away from the waiting screen withdraws the ask - but not when the screen is
            // being replaced by the failure notice or by the trade window itself.
            if (!failed) {
                tradeManager.dismissRequest(trader.getUniqueId(), player.getUniqueId());
            }
        });

        return window;
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("core.trade.menu.waiting.title");
    }
}
