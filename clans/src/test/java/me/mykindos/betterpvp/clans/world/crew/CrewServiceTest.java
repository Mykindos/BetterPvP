package me.mykindos.betterpvp.clans.world.crew;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrewServiceTest {

    private static final String BERTH = "north_berth";
    private static final String WORLD = "Season-2/Spawn";
    private static final int CAPACITY = 3;

    private CrewService service;
    private UUID alice;
    private UUID bob;
    private UUID carol;
    private UUID dave;

    @BeforeEach
    void setUp() {
        service = new CrewService();
        alice = UUID.randomUUID();
        bob = UUID.randomUUID();
        carol = UUID.randomUUID();
        dave = UUID.randomUUID();
    }

    private Crew musterAlice() {
        return service.muster(alice, BERTH, WORLD, CAPACITY);
    }

    @Nested
    @DisplayName("mustering")
    class Mustering {

        @Test
        @DisplayName("a new captain's crew contains only them")
        void newCrewHoldsOnlyTheCaptain() {
            final Crew crew = musterAlice();

            assertEquals(alice, crew.getCaptain());
            assertEquals(1, crew.size());
            assertTrue(crew.has(alice));
            assertTrue(service.isCaptain(alice));
        }

        @Test
        @DisplayName("boarding again returns the same crew rather than stranding the first")
        void reboardingIsIdempotent() {
            final Crew first = musterAlice();
            final Crew second = musterAlice();

            assertSame(first, second);
        }

        /**
         * Boarding as a captain while crewed under somebody else has to resolve one way or the other; leaving the old
         * crew is the only option that does not put a player in two crews at once.
         */
        @Test
        @DisplayName("a member who boards as a captain leaves the crew they were in")
        void becomingCaptainLeavesTheOldCrew() {
            final Crew alices = musterAlice();
            service.join(bob, alices);

            service.muster(bob, BERTH, WORLD, CAPACITY);

            assertFalse(alices.has(bob));
            assertEquals(1, alices.size());
            assertTrue(service.isCaptain(bob));
        }

        @Test
        @DisplayName("crews forming on the same hull are all listed, and sailing ones are not")
        void musteringListsCrewsOnTheHull() {
            final Crew alices = musterAlice();
            service.muster(carol, BERTH, WORLD, CAPACITY);
            service.muster(dave, "south_berth", WORLD, CAPACITY);

            assertEquals(2, service.mustering(WORLD, BERTH).size());

            service.depart(alices);
            final List<Crew> stillForming = service.mustering(WORLD, BERTH);
            assertEquals(1, stillForming.size());
            assertEquals(carol, stillForming.getFirst().getCaptain());
        }

        @Test
        @DisplayName("an identical berth id in another world is a different hull")
        void berthsAreScopedToTheirWorld() {
            musterAlice();
            assertTrue(service.mustering("islands/mining/a1b2c3d4", BERTH).isEmpty());
        }
    }

    @Nested
    @DisplayName("joining")
    class Joining {

        @Test
        @DisplayName("an ally joins outright without queuing")
        void allyJoinsDirectly() {
            final Crew crew = musterAlice();

            assertEquals(JoinOutcome.JOINED, service.join(bob, crew));
            assertTrue(crew.has(bob));
            assertTrue(crew.pendingRequests().isEmpty());
        }

        @Test
        @DisplayName("a stranger is queued rather than admitted")
        void strangerIsQueued() {
            final Crew crew = musterAlice();

            assertEquals(JoinOutcome.REQUESTED, service.request(bob, crew));
            assertFalse(crew.has(bob));
            assertTrue(crew.hasRequestFrom(bob));
        }

        @Test
        @DisplayName("asking twice does not queue twice")
        void repeatedRequestIsIdempotent() {
            final Crew crew = musterAlice();
            service.request(bob, crew);

            assertEquals(JoinOutcome.ALREADY_REQUESTED, service.request(bob, crew));
            assertEquals(1, crew.pendingRequests().size());
        }

        /** The rule that makes waiting state mean captaincy: a captain cannot go shopping for another ship. */
        @Test
        @DisplayName("a captain cannot request to join somebody else")
        void captainsCannotRequest() {
            musterAlice();
            final Crew carols = service.muster(carol, BERTH, WORLD, CAPACITY);

            assertEquals(JoinOutcome.IS_CAPTAIN, service.request(alice, carols));
            assertEquals(JoinOutcome.IS_CAPTAIN, service.join(alice, carols));
        }

        @Test
        @DisplayName("somebody already aboard is told so rather than added twice")
        void existingMemberIsRejected() {
            final Crew crew = musterAlice();
            service.join(bob, crew);

            assertEquals(JoinOutcome.ALREADY_MEMBER, service.join(bob, crew));
            assertEquals(2, crew.size());
        }

        @Test
        @DisplayName("a full hull refuses both joins and requests")
        void capacityIsEnforced() {
            final Crew crew = musterAlice();
            service.join(bob, crew);
            service.join(carol, crew);

            assertTrue(crew.isFull());
            assertEquals(CAPACITY, crew.size());
            assertEquals(JoinOutcome.CREW_FULL, service.join(dave, crew));
            assertEquals(JoinOutcome.CREW_FULL, service.request(dave, crew));
        }

        @Test
        @DisplayName("capacity counts the captain")
        void capacityIncludesTheCaptain() {
            final Crew crew = service.muster(alice, BERTH, WORLD, 1);
            assertTrue(crew.isFull());
            assertEquals(JoinOutcome.CREW_FULL, service.join(bob, crew));
        }

        @Test
        @DisplayName("a departed crew takes nobody else aboard")
        void sailingCrewIsClosed() {
            final Crew crew = musterAlice();
            service.depart(crew);

            assertEquals(JoinOutcome.SAILING, service.join(bob, crew));
            assertEquals(JoinOutcome.SAILING, service.request(bob, crew));
        }

        /**
         * Requesting several captains at once is allowed, so a mistimed choice does not leave somebody waiting on an
         * absent captain — but the moment one accepts, the rest must not still be able to.
         */
        @Test
        @DisplayName("being accepted withdraws the requester's other requests")
        void acceptingClearsRivalRequests() {
            final Crew alices = musterAlice();
            final Crew carols = service.muster(carol, BERTH, WORLD, CAPACITY);

            service.request(bob, alices);
            service.request(bob, carols);
            assertTrue(carols.hasRequestFrom(bob));

            assertTrue(service.accept(alices, bob));
            assertFalse(carols.hasRequestFrom(bob), "a queued request for somebody already sailing is only a mis-click");
        }
    }

    @Nested
    @DisplayName("the captain's queue")
    class Queue {

        @Test
        @DisplayName("accepting moves a requester onto the roster")
        void acceptAdmits() {
            final Crew crew = musterAlice();
            service.request(bob, crew);

            assertTrue(service.accept(crew, bob));
            assertTrue(crew.has(bob));
            assertFalse(crew.hasRequestFrom(bob));
        }

        @Test
        @DisplayName("accepting somebody who never asked does nothing")
        void acceptWithoutRequestFails() {
            final Crew crew = musterAlice();
            assertFalse(service.accept(crew, bob));
            assertFalse(crew.has(bob));
        }

        /**
         * The queue outlives the space: two people can be waiting for one seat, and the second accept has to fail
         * rather than overfill the hull.
         */
        @Test
        @DisplayName("accepting into a hull that filled up while they queued is refused")
        void acceptIntoFullCrewFails() {
            final Crew crew = musterAlice();
            service.request(bob, crew);
            service.request(dave, crew);

            service.join(carol, crew);
            assertTrue(service.accept(crew, bob));

            assertTrue(crew.isFull());
            assertFalse(service.accept(crew, dave));
            assertTrue(crew.hasRequestFrom(dave), "the refused request stays so the captain can make room");
        }

        @Test
        @DisplayName("declining drops the request without admitting anyone")
        void declineDropsRequest() {
            final Crew crew = musterAlice();
            service.request(bob, crew);

            assertTrue(service.decline(crew, bob));
            assertFalse(crew.hasRequestFrom(bob));
            assertFalse(crew.has(bob));
        }

        @Test
        @DisplayName("removing a member puts them off the ship but leaves the crew standing")
        void removeMember() {
            final Crew crew = musterAlice();
            service.join(bob, crew);

            assertTrue(service.removeMember(crew, bob));
            assertFalse(crew.has(bob));
            assertTrue(service.isCaptain(alice));
            assertTrue(service.crewOf(bob).isEmpty());
        }

        @Test
        @DisplayName("the captain cannot be removed as if they were a member")
        void captainCannotBeRemoved() {
            final Crew crew = musterAlice();
            assertFalse(service.removeMember(crew, alice));
            assertTrue(crew.has(alice));
        }
    }

    @Nested
    @DisplayName("leaving")
    class Leaving {

        @Test
        @DisplayName("a member leaving only removes themselves")
        void memberLeaves() {
            final Crew crew = musterAlice();
            service.join(bob, crew);
            service.join(carol, crew);

            service.leave(bob);

            assertFalse(crew.has(bob));
            assertTrue(crew.has(carol));
            assertEquals(2, crew.size());
        }

        @Test
        @DisplayName("the captain leaving disbands the crew and releases everyone")
        void captainLeavingDisbands() {
            final Crew crew = musterAlice();
            service.join(bob, crew);

            service.leave(alice);

            assertFalse(service.isCaptain(alice));
            assertTrue(service.crewOf(alice).isEmpty());
            assertTrue(service.crewOf(bob).isEmpty(), "the crew went with the ship they signed onto");
        }

        @Test
        @DisplayName("leaving when not in a crew is harmless")
        void leavingWithoutCrewIsSafe() {
            assertTrue(service.leave(dave).isEmpty());
        }

        @Test
        @DisplayName("disconnecting clears both membership and any request left standing elsewhere")
        void forgetRemovesEveryTrace() {
            final Crew alices = musterAlice();
            final Crew carols = service.muster(carol, BERTH, WORLD, CAPACITY);
            service.request(bob, carols);
            service.join(bob, alices);
            service.request(dave, alices);

            service.forget(dave);
            assertFalse(alices.hasRequestFrom(dave));

            service.forget(bob);
            assertFalse(alices.has(bob));
            assertFalse(carols.hasRequestFrom(bob));
        }
    }

    @Nested
    @DisplayName("departure")
    class Departure {

        @Test
        @DisplayName("departing freezes the roster and clears the queue")
        void departFreezes() {
            final Crew crew = musterAlice();
            service.join(bob, crew);
            service.request(carol, crew);

            service.depart(crew);

            assertTrue(crew.isSailing());
            assertTrue(crew.pendingRequests().isEmpty(), "those people are standing on a dock the ship has left");
            assertEquals(2, crew.size());
        }

        /**
         * At sea the destination is already committed and there is nowhere to go, so the dock's rules stop applying —
         * a captain who quits mid-voyage must not strand everyone who sailed with them.
         */
        @Test
        @DisplayName("the roster still resolves after departure")
        void rosterSurvivesDeparture() {
            final Crew crew = musterAlice();
            service.join(bob, crew);
            service.depart(crew);

            assertSame(crew, service.crewOf(bob).orElseThrow());
            assertSame(crew, service.crewOf(alice).orElseThrow());
        }

        @Test
        @DisplayName("the roster lists the captain first so the menu order is stable")
        void rosterPutsCaptainFirst() {
            final Crew crew = musterAlice();
            service.join(bob, crew);
            service.join(carol, crew);

            assertEquals(List.of(alice, bob, carol), List.copyOf(crew.roster()));
        }
    }
}
