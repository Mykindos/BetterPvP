package me.mykindos.betterpvp.core.trade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.npc.NpcInteractionRegistry;
import me.mykindos.betterpvp.core.trade.event.TradeCancelledEvent;
import me.mykindos.betterpvp.core.trade.event.TradeCompletedEvent;
import me.mykindos.betterpvp.core.trade.event.TradeStartedEvent;
import me.mykindos.betterpvp.core.trade.menu.TradeBrowserMenu;
import me.mykindos.betterpvp.core.trade.menu.TradeWindowMenu;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Owns every live trade, and the queue of people waiting to start one.
 * <p>
 * The trading population is deliberately not "everyone online": a player appears in the broker's list
 * only while they are stood at the broker with the list open. Being tradeable is a thing you do, not a
 * state you are permanently in, which is what keeps the list short and keeps unsolicited requests from
 * reaching people who are busy elsewhere.
 */
@CustomLog
@Singleton
public class TradeManager {

    private static final long NUDGE_COOLDOWN_MILLIS = 3000L;

    private final TradeSettlement settlement;
    private final ClientManager clientManager;
    private final TradeCurrencyRegistry currencyRegistry;

    @Inject
    @Config(path = "trade.negotiation-seconds", defaultValue = "120")
    private int negotiationSeconds;

    @Inject
    @Config(path = "trade.acknowledge-seconds", defaultValue = "5")
    private int acknowledgeSeconds;

    @Inject
    @Config(path = "trade.request-seconds", defaultValue = "60")
    private int requestSeconds;

    /** Everyone with the broker list open, and the callback that redraws it for them. */
    private final Map<UUID, Runnable> browsers = new HashMap<>();
    private final List<TradeRequest> requests = new ArrayList<>();
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    /**
     * Set whenever the broker list would look different to somebody. Redrawing on this rather than on
     * every tick keeps a menu full of player heads from being rebuilt four times a second.
     */
    private boolean browsersDirty;

    private final Map<UUID, Long> lastNudge = new HashMap<>();

    /**
     * Requesters sat on the waiting screen, and how to tell them their request died. The flag says
     * whether it ran out of time rather than being turned down.
     */
    private final Map<UUID, Consumer<Boolean>> waiters = new HashMap<>();

    @Inject
    public TradeManager(TradeSettlement settlement, ClientManager clientManager, TradeCurrencyRegistry currencyRegistry,
                        NpcInteractionRegistry npcInteractions) {
        this.settlement = settlement;
        this.clientManager = clientManager;
        this.currencyRegistry = currencyRegistry;

        // Any NPC tagged interact:trade is a broker. Trading claims the name; the map decides who wears it.
        npcInteractions.register("trade", this::openBroker);
    }

    @NotNull
    public TradeCurrencyRegistry getCurrencyRegistry() {
        return currencyRegistry;
    }

    @NotNull
    public Gamer getGamer(@NotNull Player player) {
        return clientManager.search().online(player).getGamer();
    }

    public void openBroker(@NotNull Player player) {
        if (getSession(player.getUniqueId()) != null) {
            UtilMessage.message(player, "Trade", Translations.component("core.trade.already-trading").color(NamedTextColor.RED));
            return;
        }
        new TradeBrowserMenu(this, player).show(player);
    }

    public void addBrowser(@NotNull UUID player, @NotNull Runnable onChanged) {
        browsers.put(player, onChanged);
        browsersDirty = true;
    }

    /**
     * Takes a player out of the broker list. Requests aimed <i>at</i> them go too - there is nobody
     * left looking at the list to answer them - but their own outgoing request survives, because
     * leaving the list is exactly what happens when they move to the waiting screen.
     */
    public void removeBrowser(@NotNull UUID player) {
        if (browsers.remove(player) == null) {
            return;
        }
        requests.removeIf(request -> request.getTarget().equals(player));
        browsersDirty = true;
    }

    public void addWaiter(@NotNull UUID player, @NotNull Consumer<Boolean> onEnded) {
        waiters.put(player, onEnded);
    }

    public void removeWaiter(@NotNull UUID player) {
        waiters.remove(player);
    }

    private void endWaiter(@NotNull UUID requester, boolean timedOut) {
        final Consumer<Boolean> waiter = waiters.remove(requester);
        if (waiter != null) {
            waiter.accept(timedOut);
        }
    }

    /**
     * @return Everyone currently at the broker and free to be asked, excluding {@code viewer}.
     */
    @NotNull
    public List<Player> getAvailableTraders(@NotNull UUID viewer) {
        final List<Player> available = new ArrayList<>();
        for (UUID uuid : browsers.keySet()) {
            if (uuid.equals(viewer) || sessions.containsKey(uuid)) {
                continue;
            }
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                available.add(player);
            }
        }
        return available;
    }

    /**
     * @return The people who have asked {@code target} for a trade and are still waiting.
     */
    @NotNull
    public List<TradeRequest> getIncomingRequests(@NotNull UUID target) {
        return requests.stream().filter(request -> request.getTarget().equals(target)).toList();
    }

    public boolean hasRequested(@NotNull UUID requester, @NotNull UUID target) {
        return requests.stream().anyMatch(request -> request.isBetween(requester, target));
    }

    public void request(@NotNull Player requester, @NotNull Player target) {
        if (hasRequested(requester.getUniqueId(), target.getUniqueId())) {
            return;
        }

        requests.add(new TradeRequest(requester.getUniqueId(), target.getUniqueId()));
        browsersDirty = true;
        UtilMessage.message(requester, "Trade", Translations.component("core.trade.request-sent")
                .append(Component.space())
                .append(Component.text(target.getName(), NamedTextColor.YELLOW)));
        UtilMessage.message(target, "Trade", Component.text(requester.getName(), NamedTextColor.YELLOW)
                .append(Component.space())
                .append(Translations.component("core.trade.request-received")));
        new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.4f, 1.0f).play(target);
    }

    /**
     * Turns down a request without telling the requester, who simply stops seeing it accepted. There
     * is no rejection message on purpose - an ignored ask should cost the person ignoring it nothing.
     */
    public void dismissRequest(@NotNull UUID target, @NotNull UUID requester) {
        if (requests.removeIf(request -> request.isBetween(requester, target))) {
            browsersDirty = true;
            endWaiter(requester, false);
        }
    }

    /**
     * Accepts a standing request and puts both players into a trade window.
     */
    public void acceptRequest(@NotNull Player target, @NotNull UUID requesterId) {
        final Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || sessions.containsKey(requesterId) || sessions.containsKey(target.getUniqueId())) {
            requests.removeIf(request -> request.isBetween(requesterId, target.getUniqueId()));
            UtilMessage.message(target, "Trade", Translations.component("core.trade.request-unavailable").color(NamedTextColor.RED));
            return;
        }

        requests.removeIf(request -> request.getRequester().equals(requesterId) || request.getTarget().equals(requesterId)
                || request.getRequester().equals(target.getUniqueId()) || request.getTarget().equals(target.getUniqueId()));
        removeWaiter(requesterId);

        final TradeSession session = new TradeSession(requesterId, target.getUniqueId(),
                negotiationSeconds * 1000L, acknowledgeSeconds * 1000L);
        sessions.put(requesterId, session);
        sessions.put(target.getUniqueId(), session);
        browsersDirty = true;

        // Opening the trade window closes the broker list, which removes both of them from it.
        new TradeWindowMenu(this, session, requester).show(requester);
        new TradeWindowMenu(this, session, target).show(target);

        UtilServer.callEvent(new TradeStartedEvent(session));
    }

    @Nullable
    public TradeSession getSession(@NotNull UUID player) {
        return sessions.get(player);
    }

    /**
     * Asks the other side to sweeten their offer. Throttled, because it is a button that sends someone
     * else a message.
     */
    public void requestMore(@NotNull TradeSession session, @NotNull Player from) {
        final long now = System.currentTimeMillis();
        if (now - lastNudge.getOrDefault(from.getUniqueId(), 0L) < NUDGE_COOLDOWN_MILLIS) {
            return;
        }
        lastNudge.put(from.getUniqueId(), now);

        final Player opponent = Bukkit.getPlayer(session.opponentOf(from.getUniqueId()).getPlayer());
        if (opponent == null) {
            return;
        }

        session.setNudgedBy(from.getUniqueId());
        session.notifyObservers();

        UtilMessage.message(opponent, "Trade", Component.text(from.getName(), NamedTextColor.YELLOW)
                .append(Component.space())
                .append(Translations.component("core.trade.asks-for-more")));
        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.0f).play(opponent);
    }

    public void accept(@NotNull TradeSession session, @NotNull UUID player) {
        session.accept(player);
        forEachOnline(session, participant -> new SoundEffect(Sound.UI_BUTTON_CLICK, 1.2f, 1.0f).play(participant));
    }

    /**
     * Ends a trade without swapping and hands both escrows back.
     * <p>
     * Safe to call more than once and from anywhere - a session that is no longer active is ignored,
     * which is what lets the window's close handler, the tick, and the quit listener all call it
     * without coordinating.
     */
    public void cancel(@NotNull TradeSession session, @NotNull TradeCancelReason reason) {
        if (!session.isActive()) {
            return;
        }

        session.setState(TradeState.CANCELLED);
        sessions.remove(session.getInitiator().getPlayer());
        sessions.remove(session.getTarget().getPlayer());

        returnEscrow(session.getInitiator());
        returnEscrow(session.getTarget());

        forEachOnline(session, participant -> {
            participant.closeInventory();
            UtilMessage.message(participant, "Trade", Translations.component(reasonKey(reason)).color(NamedTextColor.RED));
            new SoundEffect(Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f).play(participant);
        });

        UtilServer.callEvent(new TradeCancelledEvent(session, reason));
    }

    private void settle(@NotNull TradeSession session) {
        final TradeCancelReason failure = settlement.settle(session);
        if (failure != null) {
            cancel(session, failure);
            return;
        }

        sessions.remove(session.getInitiator().getPlayer());
        sessions.remove(session.getTarget().getPlayer());

        forEachOnline(session, participant -> {
            participant.closeInventory();
            UtilMessage.message(participant, "Trade", Translations.component("core.trade.completed").color(NamedTextColor.GREEN));
            new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1.4f, 1.0f).play(participant);
        });

        UtilServer.callEvent(new TradeCompletedEvent(session));
    }

    /**
     * Advances every live trade: expires stale requests, times out slow negotiations, and settles the
     * ones whose countdown has run out.
     */
    public void tick() {
        final List<TradeRequest> dead = new ArrayList<>();
        requests.removeIf(request -> {
            final boolean gone = request.hasExpired(requestSeconds * 1000L)
                    || Bukkit.getPlayer(request.getRequester()) == null
                    || Bukkit.getPlayer(request.getTarget()) == null;
            if (gone) {
                dead.add(request);
            }
            return gone;
        });
        if (!dead.isEmpty()) {
            browsersDirty = true;
            dead.forEach(request -> endWaiter(request.getRequester(), true));
        }

        if (browsersDirty) {
            browsersDirty = false;
            for (Runnable refresh : new ArrayList<>(browsers.values())) {
                refresh.run();
            }
        }

        final Set<TradeSession> live = new HashSet<>(sessions.values());
        for (TradeSession session : live) {
            if (Bukkit.getPlayer(session.getInitiator().getPlayer()) == null
                    || Bukkit.getPlayer(session.getTarget().getPlayer()) == null) {
                cancel(session, TradeCancelReason.DISCONNECTED);
                continue;
            }

            if (session.isReadyToSettle()) {
                settle(session);
            } else if (session.getState() == TradeState.NEGOTIATING && session.getNegotiationRemaining() == 0L) {
                cancel(session, TradeCancelReason.TIMED_OUT);
            } else {
                tickCountdown(session);
                session.notifyObservers();
            }
        }
    }

    /**
     * Beats out the acknowledgement countdown, one click per second to both players. The tick runs far
     * faster than that, so the second the clock is showing is what decides whether to play.
     */
    private void tickCountdown(@NotNull TradeSession session) {
        if (session.getState() != TradeState.ACKNOWLEDGING) {
            return;
        }

        final long second = (session.getAcknowledgeRemaining() + 999L) / 1000L;
        if (session.getLastCountdownSecond() == second) {
            return;
        }

        session.setLastCountdownSecond(second);
        forEachOnline(session, participant -> new SoundEffect(Sound.UI_BUTTON_CLICK, 1.2f, 1.0f).play(participant));
    }

    /**
     * Hands a player back everything they had staged. Called on every path that ends a trade without
     * settling, including a disconnect, so escrowed items are never left in a dead session.
     */
    private void returnEscrow(@NotNull TradeOffer offer) {
        final List<ItemStack> staged = offer.drainItems();
        if (staged.isEmpty()) {
            return;
        }

        final Player owner = Bukkit.getPlayer(offer.getPlayer());
        if (owner != null) {
            for (ItemStack item : staged) {
                UtilItem.insert(owner, item);
            }
            return;
        }

        // Offline before the escrow could be handed back: the items are on the floor where they stood
        // rather than lost, and this is loud in the log because it is the one path that can be noticed
        // by a player as a missing item.
        log.warn("Trade escrow for offline player {} returned {} item(s) to the world", offer.getPlayer(), staged.size()).submit();
    }

    private void forEachOnline(@NotNull TradeSession session, @NotNull Consumer<Player> action) {
        for (TradeOffer offer : List.of(session.getInitiator(), session.getTarget())) {
            final Player player = Bukkit.getPlayer(offer.getPlayer());
            if (player != null) {
                action.accept(player);
            }
        }
    }

    private String reasonKey(@NotNull TradeCancelReason reason) {
        return "core.trade.cancelled." + reason.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Ends every live trade, handing back both escrows.
     * <p>
     * Escrowed items are held in memory, so a shutdown that did not do this would take them with it.
     * This covers restarts and reloads; a hard crash still cannot be caught here.
     */
    public void cancelAll() {
        for (TradeSession session : new HashSet<>(sessions.values())) {
            cancel(session, TradeCancelReason.DISCONNECTED);
        }
    }

    /**
     * Removes a player from trading entirely. Called while they are still resolvable, so any escrow
     * they were holding goes back into the inventory that is about to be saved.
     */
    public void clear(@NotNull UUID player, @NotNull TradeCancelReason reason) {
        removeBrowser(player);
        final TradeSession session = sessions.get(player);
        if (session != null) {
            cancel(session, reason);
        }
    }
}
