package me.mykindos.betterpvp.core.trade.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.framework.profiles.PlayerProfiles;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.trade.TradeManager;
import me.mykindos.betterpvp.core.trade.menu.TradeWaitingMenu;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

/**
 * One person standing at the broker, in the lower band of the list. Clicking asks them for a trade.
 */
@RequiredArgsConstructor
public class TraderButton extends AbstractItem {

    private final TradeManager tradeManager;
    private final Player viewer;
    private final Player trader;

    @Override
    public ItemProvider getItemProvider() {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(PlayerProfiles.CACHE.get(trader.getUniqueId(), key -> trader.getPlayerProfile()));
        head.setItemMeta(meta);

        final ItemView.ItemViewBuilder builder = ItemView.of(head).toBuilder()
                .displayName(Component.text(trader.getName(), NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(Component.empty());

        if (tradeManager.hasRequested(viewer.getUniqueId(), trader.getUniqueId())) {
            builder.lore(Translations.component("core.trade.button.trader.awaiting").color(NamedTextColor.YELLOW));
        } else {
            builder.action(ClickActions.LEFT, Translations.component("core.trade.button.trader.request"));
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!clickType.isLeftClick() || tradeManager.hasRequested(viewer.getUniqueId(), trader.getUniqueId())) {
            return;
        }

        tradeManager.request(viewer, trader);
        new SoundEffect(Sound.UI_BUTTON_CLICK, 1.2f, 1.0f).play(player);
        new TradeWaitingMenu(tradeManager, viewer, trader).show(viewer);
    }
}
