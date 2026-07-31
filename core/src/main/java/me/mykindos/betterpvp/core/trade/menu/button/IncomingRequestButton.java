package me.mykindos.betterpvp.core.trade.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.framework.profiles.PlayerProfiles;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.trade.TradeManager;
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
 * Somebody who has asked the viewer for a trade, pinned to the top band of the broker list.
 * <p>
 * The only answers here are accept and dismiss: haggling belongs in the trade window, not in a queue
 * of requests.
 */
@RequiredArgsConstructor
public class IncomingRequestButton extends AbstractItem {

    private final TradeManager tradeManager;
    private final Player viewer;
    private final Player requester;

    @Override
    public ItemProvider getItemProvider() {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(PlayerProfiles.CACHE.get(requester.getUniqueId(), key -> requester.getPlayerProfile()));
        head.setItemMeta(meta);

        return ItemView.of(head).toBuilder()
                .displayName(Component.text(requester.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(Translations.component("core.trade.button.request.wants").color(NamedTextColor.GRAY))
                .action(ClickActions.LEFT, Translations.component("core.trade.button.request.accept"))
                .action(ClickActions.RIGHT, Translations.component("core.trade.button.request.dismiss"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isRightClick()) {
            tradeManager.dismissRequest(viewer.getUniqueId(), requester.getUniqueId());
            new SoundEffect(Sound.UI_BUTTON_CLICK, 0.8f, 1.0f).play(player);
            return;
        }

        tradeManager.acceptRequest(viewer, requester.getUniqueId());
    }
}
