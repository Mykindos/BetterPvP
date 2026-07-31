package me.mykindos.betterpvp.core.trade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Performs the swap.
 * <p>
 * Everything that could refuse the trade is checked before anything moves, and the move itself is a
 * single uninterrupted block on the main thread. There is therefore no point at which one side has
 * paid and the other has not - a disconnect either happens before the checks, and the trade is
 * cancelled with both escrows intact, or after the swap, by which time both players own their goods.
 * This is what the acknowledgement countdown is protecting: it is a window to change your mind, not a
 * window in which the transfer is half-done.
 */
@CustomLog
@Singleton
public class TradeSettlement {

    private final ClientManager clientManager;

    @Inject
    public TradeSettlement(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /**
     * Settles the session if it still can be.
     *
     * @return {@code null} if both sides were paid, otherwise the reason the trade must be cancelled.
     * Nothing has moved when a reason is returned.
     */
    @Nullable
    public TradeCancelReason settle(@NotNull TradeSession session) {
        final TradeOffer initiatorOffer = session.getInitiator();
        final TradeOffer targetOffer = session.getTarget();

        final Player initiator = Bukkit.getPlayer(initiatorOffer.getPlayer());
        final Player target = Bukkit.getPlayer(targetOffer.getPlayer());
        if (initiator == null || target == null) {
            return TradeCancelReason.DISCONNECTED;
        }

        final Gamer initiatorGamer = clientManager.search().online(initiator).getGamer();
        final Gamer targetGamer = clientManager.search().online(target).getGamer();

        if (!canAfford(initiatorGamer, initiatorOffer) || !canAfford(targetGamer, targetOffer)) {
            return TradeCancelReason.INSUFFICIENT_FUNDS;
        }

        // Each player receives the other's items, so each is checked against the opposite offer.
        if (!hasRoomFor(initiator, targetOffer) || !hasRoomFor(target, initiatorOffer)) {
            return TradeCancelReason.NO_SPACE;
        }

        // Currencies move before the escrows are drained. Both are effectively infallible by this
        // point, but if one were to throw, an undrained escrow is still returnable whereas a drained
        // one is items held by nothing.
        pay(initiatorGamer, targetGamer, initiatorOffer);
        pay(targetGamer, initiatorGamer, targetOffer);

        final List<ItemStack> fromInitiator = initiatorOffer.drainItems();
        final List<ItemStack> fromTarget = targetOffer.drainItems();

        for (ItemStack item : fromInitiator) {
            UtilItem.insert(target, item);
        }
        for (ItemStack item : fromTarget) {
            UtilItem.insert(initiator, item);
        }

        session.setState(TradeState.SETTLED);
        log.info("{} traded with {} ({} item(s) and {} currencies for {} item(s) and {} currencies)",
                initiator.getName(), target.getName(),
                fromInitiator.size(), initiatorOffer.getCurrencies().size(),
                fromTarget.size(), targetOffer.getCurrencies().size()).submit();
        return null;
    }

    private boolean canAfford(@NotNull Gamer gamer, @NotNull TradeOffer offer) {
        for (Map.Entry<TradeCurrency, Integer> entry : offer.getCurrencies().entrySet()) {
            if (entry.getKey().getBalance(gamer) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void pay(@NotNull Gamer from, @NotNull Gamer to, @NotNull TradeOffer offer) {
        for (Map.Entry<TradeCurrency, Integer> entry : offer.getCurrencies().entrySet()) {
            entry.getKey().transfer(from, to, entry.getValue());
        }
    }

    /**
     * Deliberately counts whole free slots and ignores partial stacks that the incoming items could
     * merge into. Refusing a trade that would in fact have fitted is a message; letting one through
     * that does not fit means dropping traded goods on the floor at spawn.
     */
    private boolean hasRoomFor(@NotNull Player player, @NotNull TradeOffer incoming) {
        int free = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                free++;
            }
        }
        return free >= incoming.getOccupiedSlots();
    }
}
