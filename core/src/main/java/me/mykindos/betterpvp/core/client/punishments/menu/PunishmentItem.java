package me.mykindos.betterpvp.core.client.punishments.menu;

import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentHandler;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The punishment item
 * This is a blocking item and should be created async
 */
public class PunishmentItem extends AbstractItem {

    private final Punishment punishment;
    private final PunishmentHandler punishmentHandler;
    private final boolean showPunisher;
    private final String revokeReason;

    private final Windowed previous;


    /**
     * Display the punishment, without the option to revoke it
     * Blocking, should be created async
     * @param punishment the punishment
     * @param punishmentHandler the punishment manager
     * @param showPunisher whether to show the punisher and revoker or not
     */
    public PunishmentItem(Punishment punishment, PunishmentHandler punishmentHandler, boolean showPunisher, Windowed previous) {
        this(punishment, punishmentHandler, showPunisher, null, previous);
    }

    /**
     * Display the punishment, without the option to revoke it
     * Blocking, should be created async
     * @param punishment the punishment
     * @param punishmentHandler the punishment manager
     * @param revokeReason the reason for this revoke
     */
    public PunishmentItem(Punishment punishment, PunishmentHandler punishmentHandler, @NotNull String revokeReason, Windowed previous) {
        this(punishment, punishmentHandler, true, revokeReason, previous);
    }

    /**
     * Display the punishment
     * Blocking, should be created async
     * @param punishment the punishment
     * @param punishmentHandler the punishment manager
     * @param showPunisher whether to show the punisher and revoker or not
     * @param revokeReason the reason for this revoke, null if not revoking
     */
    public PunishmentItem(Punishment punishment, PunishmentHandler punishmentHandler, boolean showPunisher, @Nullable String revokeReason, Windowed previous) {
        this.punishment = punishment;
        this.punishmentHandler = punishmentHandler;
        this.showPunisher = showPunisher;
        this.revokeReason = revokeReason;
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
        return punishment.getItemView(punishmentHandler.getClientManager(), showPunisher);
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
        if (!punishment.isRevoked() && showPunisher && revokeReason != null) {
            new RevokeMenu(punishment, revokeReason, this, punishmentHandler, previous).show(player);
        }
    }
}
