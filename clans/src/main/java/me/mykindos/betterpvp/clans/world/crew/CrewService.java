package me.mykindos.betterpvp.clans.world.crew;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Who is crewed with whom, and the rules for changing that.
 * <p>
 * Deliberately knows nothing about Bukkit, hulls or clans: it is handed decisions ("this player may join outright")
 * and answers what the roster becomes. Whether somebody is standing on the ship, and whether they are an ally, is the
 * caller's business — which keeps every membership rule here testable without a server.
 * <p>
 * Crews are keyed by captain, so several can form on one hull at once.
 */
@Singleton
public class CrewService {

    /** Insertion-ordered so the crews forming on a hull are offered in the order they were started. */
    private final Map<UUID, Crew> byCaptain = new LinkedHashMap<>();
    private final Map<UUID, Crew> byPlayer = new LinkedHashMap<>();

    /**
     * Makes {@code captain} the captain of a new crew, or returns the one they already have.
     * <p>
     * Re-boarding is not an error: clicking the navigator again just puts you back on your own ship rather than
     * silently starting a second crew and stranding the first.
     */
    public @NotNull Crew muster(@NotNull UUID captain, @NotNull String berthId, @NotNull String worldName, int capacity) {
        final Crew existing = byCaptain.get(captain);
        if (existing != null) {
            return existing;
        }

        // Boarding as a captain means leaving whoever you were sailing with; you cannot be in two crews.
        leave(captain);

        final Crew crew = new Crew(captain, berthId, worldName, capacity);
        byCaptain.put(captain, crew);
        byPlayer.put(captain, crew);
        return crew;
    }

    public @NotNull Optional<Crew> crewOf(@NotNull UUID player) {
        return Optional.ofNullable(byPlayer.get(player));
    }

    public @NotNull Optional<Crew> captainedBy(@NotNull UUID player) {
        return Optional.ofNullable(byCaptain.get(player));
    }

    public boolean isCaptain(@NotNull UUID player) {
        return byCaptain.containsKey(player);
    }

    /** Crews still forming on one hull — the people a newcomer walking aboard could ask to join. */
    public @NotNull List<Crew> mustering(@NotNull String worldName, @NotNull String berthId) {
        final List<Crew> crews = new ArrayList<>();
        for (Crew crew : byCaptain.values()) {
            if (crew.getState() == CrewState.MUSTERING
                    && crew.getWorldName().equals(worldName)
                    && crew.getBerthId().equals(berthId)) {
                crews.add(crew);
            }
        }
        return crews;
    }

    /**
     * Asks to come aboard. Refusals are distinguished so the caller can say which one it was.
     */
    public @NotNull JoinOutcome request(@NotNull UUID requester, @NotNull Crew crew) {
        final JoinOutcome refusal = refuse(requester, crew);
        if (refusal != null) {
            return refusal;
        }
        if (crew.hasRequestFrom(requester)) {
            return JoinOutcome.ALREADY_REQUESTED;
        }

        crew.getRequests().add(requester);
        return JoinOutcome.REQUESTED;
    }

    /**
     * Puts somebody straight aboard, skipping the queue — for a clanmate or ally, who does not have to ask.
     */
    public @NotNull JoinOutcome join(@NotNull UUID player, @NotNull Crew crew) {
        final JoinOutcome refusal = refuse(player, crew);
        if (refusal != null) {
            return refusal;
        }

        admit(player, crew);
        return JoinOutcome.JOINED;
    }

    /**
     * Accepts a standing request.
     *
     * @return {@code false} if there was no such request, or the hull filled up while it was queued
     */
    public boolean accept(@NotNull Crew crew, @NotNull UUID requester) {
        if (crew.isSailing() || !crew.getRequests().contains(requester) || crew.isFull()) {
            return false;
        }

        crew.getRequests().remove(requester);
        admit(requester, crew);
        return true;
    }

    /** Turns a request down, or withdraws it. */
    public boolean decline(@NotNull Crew crew, @NotNull UUID requester) {
        return crew.getRequests().remove(requester);
    }

    /**
     * Puts a member off the ship. The captain cannot be removed this way — {@link #disband(Crew)} is what ends a crew.
     */
    public boolean removeMember(@NotNull Crew crew, @NotNull UUID member) {
        if (!crew.getMembers().remove(member)) {
            return false;
        }
        byPlayer.remove(member, crew);
        return true;
    }

    /**
     * A player leaves of their own accord — walking off the hull, or logging out.
     * <p>
     * A captain leaving takes the crew with them: the ship they gathered everyone onto is theirs, and there is no
     * sensible way to choose a successor from people who signed on with them specifically.
     *
     * @return the crew they left, if they were in one
     */
    public @NotNull Optional<Crew> leave(@NotNull UUID player) {
        final Crew crew = byPlayer.get(player);
        if (crew == null) {
            return Optional.empty();
        }

        if (crew.getCaptain().equals(player)) {
            disband(crew);
        } else {
            removeMember(crew, player);
        }
        return Optional.of(crew);
    }

    /** Ends a crew, releasing everyone aboard. */
    public void disband(@NotNull Crew crew) {
        for (UUID member : crew.roster()) {
            byPlayer.remove(member, crew);
        }
        crew.getMembers().clear();
        crew.getRequests().clear();
        byCaptain.remove(crew.getCaptain(), crew);
    }

    /**
     * Freezes the roster and sends the crew to sea. Outstanding requests are dropped: the ship has left, and leaving
     * them queued would show the captain a list of people standing on a dock they can no longer reach.
     */
    public void depart(@NotNull Crew crew) {
        crew.getRequests().clear();
        crew.setState(CrewState.SAILING);
    }

    /**
     * Removes every trace of a player — their crew membership and any request they left standing elsewhere. Used when
     * they disconnect, so a captain is never shown a queue of people who are not there.
     */
    public void forget(@NotNull UUID player) {
        withdrawRequests(player);
        leave(player);
    }

    /** Drops {@code player}'s pending requests from every crew. */
    public void withdrawRequests(@NotNull UUID player) {
        for (Crew crew : byCaptain.values()) {
            crew.getRequests().remove(player);
        }
    }

    /**
     * @return the reason {@code player} cannot come aboard, or {@code null} if they can
     */
    private JoinOutcome refuse(@NotNull UUID player, @NotNull Crew crew) {
        if (crew.isSailing()) {
            return JoinOutcome.SAILING;
        }
        if (crew.has(player)) {
            return JoinOutcome.ALREADY_MEMBER;
        }
        // A captain is already committed to their own crew; stepping off the hull is how they stop being one.
        if (isCaptain(player)) {
            return JoinOutcome.IS_CAPTAIN;
        }
        if (crew.isFull()) {
            return JoinOutcome.CREW_FULL;
        }
        return null;
    }

    private void admit(@NotNull UUID player, @NotNull Crew crew) {
        leave(player);
        // Whoever they were still waiting on cannot accept them now, and a queue entry for somebody already sailing
        // with a rival captain is only there to be clicked by mistake.
        withdrawRequests(player);

        crew.getMembers().add(player);
        byPlayer.put(player, crew);
    }
}
