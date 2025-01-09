package me.mykindos.betterpvp.core.client.punishments.menu;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.PunishmentHandler;
import me.mykindos.betterpvp.core.client.punishments.rules.Rule;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@CustomLog
public class ApplyPunishmentItem extends AbstractItem {
    private final Client punisher;
    private final Rule rule;
    private final Client target;
    private final String reason;
    private final PunishmentHandler punishmentHandler;
    private final Windowed previous;

    public ApplyPunishmentItem(Client punisher, Rule rule, Client target, String reason, PunishmentHandler punishmentHandler, Windowed previous) {
        this.punisher = punisher;
        this.rule = rule;
        this.target = target;
        this.reason = reason;
        this.punishmentHandler = punishmentHandler;
        this.previous = previous;
    }

    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider() {
        return rule.getItemView(reason);
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isLeftClick()) {
            new ConfirmationMenu("Punish " + target.getName() + " For: " + rule.getKey() + " Reason: " + reason, (success) -> {
                if (success.equals(Boolean.TRUE)) {
                    punishmentHandler.punish(punisher, target, reason, rule);
                } else {
                    previous.show(player);
                }
            }).show(player);
        }
    }
}
