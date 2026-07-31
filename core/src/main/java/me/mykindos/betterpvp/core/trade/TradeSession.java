package me.mykindos.betterpvp.core.trade;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One live trade between two players, and the rules about when it may change and when it may settle.
 * <p>
 * The session owns state only. It never touches an inventory or a balance beyond its own escrow -
 * moving value is {@link TradeSettlement}'s job, and showing the state is the trade window's. What it
 * does own is the invariant that makes the trade safe to accept: <b>any change to either offer clears
 * both acceptances</b>. Nobody can accept a deal and then have it altered under them.
 */
public class TradeSession {

    @Getter
    private final TradeOffer initiator;
    @Getter
    private final TradeOffer target;

    private final long negotiationMillis;
    private final long acknowledgeMillis;

    private final List<Runnable> observers = new ArrayList<>(2);

    @Getter
    private TradeState state = TradeState.NEGOTIATING;
    @Getter
    private final long startedAt = System.currentTimeMillis();

    private long acknowledgeStartedAt;

    /**
     * Who last asked for more, if anyone. Cleared by the next change to either offer, so the notice
     * stands exactly as long as the request goes unanswered.
     */
    @Getter
    @Setter
    private UUID nudgedBy;

    /** The last whole second of the countdown that was announced, so it is ticked once and not once per refresh. */
    @Getter
    @Setter
    private long lastCountdownSecond = -1;

    public TradeSession(@NotNull UUID initiator, @NotNull UUID target, long negotiationMillis, long acknowledgeMillis) {
        this.initiator = new TradeOffer(initiator);
        this.target = new TradeOffer(target);
        this.negotiationMillis = negotiationMillis;
        this.acknowledgeMillis = acknowledgeMillis;

        bindEscrow(this.initiator);
        bindEscrow(this.target);
    }

    /**
     * Wires an offer's escrow so the session hears about every item that moves in or out, and so the
     * escrow refuses changes once the offers are frozen.
     */
    private void bindEscrow(@NotNull TradeOffer offer) {
        offer.getItems().setPreUpdateHandler(event -> {
            if (event.getUpdateReason() instanceof PlayerUpdateReason && state != TradeState.NEGOTIATING) {
                event.setCancelled(true);
            }
        });
        offer.getItems().setPostUpdateHandler(event -> {
            if (event.getUpdateReason() instanceof PlayerUpdateReason) {
                markChanged();
            }
        });
    }

    /**
     * Registers something that should redraw whenever this session changes - one per open trade window.
     */
    public void observe(@NotNull Runnable observer) {
        observers.add(observer);
    }

    public void notifyObservers() {
        for (Runnable observer : observers) {
            observer.run();
        }
    }

    @NotNull
    public TradeOffer offerOf(@NotNull UUID player) {
        return initiator.getPlayer().equals(player) ? initiator : target;
    }

    @NotNull
    public TradeOffer opponentOf(@NotNull UUID player) {
        return initiator.getPlayer().equals(player) ? target : initiator;
    }

    public boolean involves(@NotNull UUID player) {
        return initiator.getPlayer().equals(player) || target.getPlayer().equals(player);
    }

    public boolean isActive() {
        return state == TradeState.NEGOTIATING || state == TradeState.ACKNOWLEDGING;
    }

    /**
     * Records that an offer changed: both acceptances drop and, if the countdown was already running,
     * it is abandoned.
     */
    public void markChanged() {
        initiator.setAccepted(false);
        target.setAccepted(false);
        nudgedBy = null;
        if (state == TradeState.ACKNOWLEDGING) {
            state = TradeState.NEGOTIATING;
        }
        notifyObservers();
    }

    /**
     * Records a player's acceptance. Once both sides have accepted, the offers freeze and the
     * countdown starts.
     */
    public void accept(@NotNull UUID player) {
        if (state != TradeState.NEGOTIATING) {
            return;
        }

        offerOf(player).setAccepted(true);
        if (initiator.isAccepted() && target.isAccepted()) {
            state = TradeState.ACKNOWLEDGING;
            acknowledgeStartedAt = System.currentTimeMillis();
        }
        notifyObservers();
    }

    /**
     * Backs out of the countdown without ending the trade, returning both sides to negotiating.
     */
    public void withdrawAcceptance(@NotNull UUID player) {
        offerOf(player).setAccepted(false);
        if (state == TradeState.ACKNOWLEDGING) {
            state = TradeState.NEGOTIATING;
        }
        notifyObservers();
    }

    /**
     * @return Milliseconds left on the acknowledgement countdown, or {@code 0} when it has elapsed or
     * is not running.
     */
    public long getAcknowledgeRemaining() {
        if (state != TradeState.ACKNOWLEDGING) {
            return 0L;
        }
        return Math.max(0L, acknowledgeMillis - (System.currentTimeMillis() - acknowledgeStartedAt));
    }

    /**
     * @return Milliseconds left before the negotiation times out.
     */
    public long getNegotiationRemaining() {
        return Math.max(0L, negotiationMillis - (System.currentTimeMillis() - startedAt));
    }

    /**
     * @return {@code true} once both sides have held their acceptance for the full countdown.
     */
    public boolean isReadyToSettle() {
        return state == TradeState.ACKNOWLEDGING && getAcknowledgeRemaining() == 0L;
    }

    void setState(@NotNull TradeState state) {
        this.state = state;
    }
}
