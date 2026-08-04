package me.mykindos.betterpvp.clans.world.crew;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Keeps crew membership in step with who is actually standing on the ship.
 * <p>
 * Walking aboard and walking off <em>is</em> the join/leave gesture — there is no command for it. That only holds while
 * a crew is at the dock: once it sails the roster is fixed, and stepping off the deck in mid-ocean means nothing.
 */
@BPvPListener
@Singleton
public class CrewListener implements Listener {

    private final CrewService crewService;
    private final ShipService shipService;
    private final ClanManager clanManager;

    @Inject
    public CrewListener(@NotNull CrewService crewService, @NotNull ShipService shipService,
                        @NotNull ClanManager clanManager) {
        this.crewService = crewService;
        this.shipService = shipService;
        this.clanManager = clanManager;
    }

    /**
     * Leaving the hull leaves the crew, and takes any request standing on it with them.
     * <p>
     * Gated on actually changing block, because this runs for every movement packet and a hull test on each one would
     * be paid by every player on the server.
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final Player player = event.getPlayer();
        final Optional<Berth> standingOn = shipService.berthAt(event.getTo());

        crewService.crewOf(player.getUniqueId())
                .filter(crew -> !crew.isSailing())
                .ifPresent(crew -> {
                    if (standingOn.isEmpty() || !standingOn.get().getId().equals(crew.getBerthId())) {
                        partWays(player, crew);
                    }
                });

        // A request only means anything while its author is stood on the deck to be taken aboard.
        if (standingOn.isEmpty()) {
            crewService.withdrawRequests(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        crewService.captainedBy(player.getUniqueId())
                .filter(crew -> !crew.isSailing())
                .ifPresent(crew -> notifyDisbanded(crew, player.getName()));
        crewService.forget(player.getUniqueId());
    }

    /**
     * Clicking somebody asks to sail with them. Clanmates and allies skip the asking — they have already vouched for
     * each other elsewhere, and making them queue is ceremony for its own sake.
     */
    @EventHandler
    public void onClickPlayer(PlayerInteractEntityEvent event) {
        if (!EquipmentSlot.HAND.equals(event.getHand()) || !(event.getRightClicked() instanceof Player target)) {
            return;
        }

        final Player clicker = event.getPlayer();
        final Optional<Crew> targetCrew = crewService.captainedBy(target.getUniqueId());
        if (targetCrew.isEmpty() || targetCrew.get().isSailing()) {
            return;
        }

        final Berth berth = shipService.berthAt(clicker.getLocation()).orElse(null);
        if (berth == null || !berth.getId().equals(targetCrew.get().getBerthId())) {
            return; // not aboard the ship they are asking about
        }

        event.setCancelled(true);
        final Crew crew = targetCrew.get();
        final boolean friendly = clanManager.isAlly(clicker, target);
        final JoinOutcome outcome = friendly
                ? crewService.join(clicker.getUniqueId(), crew)
                : crewService.request(clicker.getUniqueId(), crew);

        report(clicker, target, outcome);
    }

    private void report(@NotNull Player clicker, @NotNull Player captain, @NotNull JoinOutcome outcome) {
        switch (outcome) {
            case JOINED -> {
                UtilMessage.message(clicker, "clans.prefix.crew", "clans.crew.joined", Component.text(captain.getName()));
                new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 0.8f).play(clicker);
                UtilMessage.message(captain, "clans.prefix.crew", "clans.crew.joined-yours", Component.text(clicker.getName()));
                new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1.4f, 0.5f).play(captain);
            }
            case REQUESTED -> {
                UtilMessage.message(clicker, "clans.prefix.crew", "clans.crew.requested", Component.text(captain.getName()));
                UtilMessage.message(captain, "clans.prefix.crew", "clans.crew.request-received", Component.text(clicker.getName()));
                // Subtle on purpose: a captain gathering a crew should not be pinged like a duel challenge.
                new SoundEffect(Sound.BLOCK_NOTE_BLOCK_HAT, 1.5f, 0.4f).play(captain);
            }
            case ALREADY_REQUESTED ->
                    UtilMessage.message(clicker, "clans.prefix.crew", "clans.crew.already-requested", Component.text(captain.getName()));
            case ALREADY_MEMBER ->
                    UtilMessage.message(clicker, "clans.prefix.crew", "clans.crew.already-member", Component.text(captain.getName()));
            case CREW_FULL ->
                    UtilMessage.message(clicker, "clans.prefix.crew", "clans.crew.full", Component.text(captain.getName()));
            case IS_CAPTAIN ->
                    UtilMessage.message(clicker, "clans.prefix.crew", "clans.crew.is-captain");
            case SAILING ->
                    UtilMessage.message(clicker, "clans.prefix.crew", "clans.crew.already-sailed");
        }
    }

    /** A captain stepping off takes the crew with them; a member only takes themselves. */
    private void partWays(@NotNull Player player, @NotNull Crew crew) {
        if (crew.getCaptain().equals(player.getUniqueId())) {
            notifyDisbanded(crew, player.getName());
            crewService.disband(crew);
            UtilMessage.message(player, "clans.prefix.crew", "clans.crew.captain-left");
        } else {
            crewService.removeMember(crew, player.getUniqueId());
            UtilMessage.message(player, "clans.prefix.crew", "clans.crew.stepped-ashore");
        }
        new SoundEffect(Sound.BLOCK_WOODEN_DOOR_CLOSE, 0.9f, 0.6f).play(player);
    }

    private void notifyDisbanded(@NotNull Crew crew, @NotNull String captainName) {
        for (UUID member : crew.getMembers()) {
            final Player online = Bukkit.getPlayer(member);
            if (online == null) {
                continue;
            }
            UtilMessage.message(online, "clans.prefix.crew", "clans.crew.disbanded", Component.text(captainName));
            new SoundEffect(Sound.BLOCK_WOODEN_DOOR_CLOSE, 0.7f, 0.8f).play(online);
        }
    }
}
