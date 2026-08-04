package me.mykindos.betterpvp.clans.world.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteeringInputTest {

    private static final UUID HELMSMAN = UUID.randomUUID();
    private static final UUID MATE = UUID.randomUUID();
    private static final UUID STOWAWAY = UUID.randomUUID();

    private static SteeringInput crewed() {
        final Set<UUID> roster = Set.of(HELMSMAN, MATE);
        return new SteeringInput(roster::contains);
    }

    @Test
    @DisplayName("a fresh press on the starboard control asks for a turn to starboard")
    void starboardPressTurnsStarboard() {
        final SteeringInput input = crewed();

        assertEquals(SteeringInput.Response.ACCEPTED, input.press(SteerSide.STARBOARD, HELMSMAN, 1000));

        assertEquals(1, input.net(1000));
    }

    @Test
    @DisplayName("a fresh press on the port control asks for a turn to port")
    void portPressTurnsPort() {
        final SteeringInput input = crewed();

        input.press(SteerSide.PORT, HELMSMAN, 1000);

        assertEquals(-1, input.net(1000));
    }

    @Test
    @DisplayName("nobody steering is no input")
    void idleIsNoInput() {
        assertEquals(0, crewed().net(1000));
    }

    @Test
    @DisplayName("both controls held is a stalemate, not a turn")
    void bothHeldCancelsOut() {
        final SteeringInput input = crewed();

        input.press(SteerSide.PORT, HELMSMAN, 1000);
        input.press(SteerSide.STARBOARD, MATE, 1000);

        assertEquals(0, input.net(1000));
    }

    @Test
    @DisplayName("a control still counts as held right up to the edge of the grace window")
    void inputSurvivesTheGracePeriod() {
        final SteeringInput input = crewed();

        input.press(SteerSide.STARBOARD, HELMSMAN, 1000);

        assertEquals(1, input.net(1000 + SteeringInput.GRACE_MILLIS - 1));
    }

    @Test
    @DisplayName("a control the client stopped re-sending goes slack once the grace window lapses")
    void inputExpiresAfterTheGracePeriod() {
        final SteeringInput input = crewed();

        input.press(SteerSide.STARBOARD, HELMSMAN, 1000);

        assertEquals(0, input.net(1000 + SteeringInput.GRACE_MILLIS));
        assertFalse(input.isActive(SteerSide.STARBOARD, 1000 + SteeringInput.GRACE_MILLIS));
    }

    @Test
    @DisplayName("re-pressing a control the holder already has keeps it alive rather than re-claiming it")
    void holdingRefreshesTheGracePeriod() {
        final SteeringInput input = crewed();

        input.press(SteerSide.STARBOARD, HELMSMAN, 1000);
        assertEquals(SteeringInput.Response.ACCEPTED, input.press(SteerSide.STARBOARD, HELMSMAN, 1200));

        assertEquals(1, input.net(1400));
    }

    @Test
    @DisplayName("a second player cannot take a control out of the first one's hands")
    void oneHolderPerControl() {
        final SteeringInput input = crewed();

        input.press(SteerSide.STARBOARD, HELMSMAN, 1000);

        assertEquals(SteeringInput.Response.HELD_BY_ANOTHER, input.press(SteerSide.STARBOARD, MATE, 1100));
        assertEquals(HELMSMAN, input.holder(SteerSide.STARBOARD, 1100).orElseThrow());
    }

    @Test
    @DisplayName("a control let go of is free for the next person to grab")
    void releasedControlIsClaimable() {
        final SteeringInput input = crewed();

        input.press(SteerSide.STARBOARD, HELMSMAN, 1000);

        final long afterRelease = 1000 + SteeringInput.GRACE_MILLIS;
        assertEquals(SteeringInput.Response.ACCEPTED, input.press(SteerSide.STARBOARD, MATE, afterRelease));
        assertEquals(MATE, input.holder(SteerSide.STARBOARD, afterRelease).orElseThrow());
    }

    @Test
    @DisplayName("the two controls are claimed independently of each other")
    void holdingOneControlDoesNotClaimTheOther() {
        final SteeringInput input = crewed();

        input.press(SteerSide.STARBOARD, HELMSMAN, 1000);

        assertEquals(SteeringInput.Response.ACCEPTED, input.press(SteerSide.PORT, MATE, 1000));
        assertEquals(Set.of(HELMSMAN, MATE), input.holders(1000));
    }

    @Test
    @DisplayName("somebody who is not on the roster cannot steer, and does not displace whoever is")
    void nonCrewIsRejected() {
        final SteeringInput input = crewed();

        assertEquals(SteeringInput.Response.NOT_CREW, input.press(SteerSide.STARBOARD, STOWAWAY, 1000));
        assertEquals(0, input.net(1000));

        input.press(SteerSide.STARBOARD, HELMSMAN, 1000);
        assertEquals(SteeringInput.Response.NOT_CREW, input.press(SteerSide.STARBOARD, STOWAWAY, 1100));
        assertEquals(HELMSMAN, input.holder(SteerSide.STARBOARD, 1100).orElseThrow());
    }

    @Test
    @DisplayName("one control lapsing while the other is still held ends the stalemate")
    void stalemateResolvesWhenOneHandLetsGo() {
        final SteeringInput input = crewed();

        input.press(SteerSide.PORT, HELMSMAN, 1000);
        input.press(SteerSide.STARBOARD, MATE, 1000);
        input.press(SteerSide.STARBOARD, MATE, 1300);

        assertEquals(1, input.net(1400));
        assertTrue(input.holder(SteerSide.PORT, 1400).isEmpty());
    }
}
