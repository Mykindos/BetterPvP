package me.mykindos.betterpvp.core.scene.mob.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIControllerTest {

    private final List<String> log = new ArrayList<>();
    private final AIController controller = new AIController();

    private FakeComponent component(String name, AIControl... controls) {
        return new FakeComponent(name, log, controls);
    }

    private List<String> starts() {
        return log.stream().filter(entry -> entry.startsWith("start")).toList();
    }

    @Test
    void ac1_addPutsComponentsAtTheLowestPriorityInOrder() {
        controller.add(component("a"));
        controller.add(component("b"));
        controller.add(component("c"));

        controller.tick();

        assertEquals(List.of("start a", "start b", "start c"), starts());
    }

    @Test
    void ac1_addFirstPutsAComponentAtTheHighestPriority() {
        controller.add(component("a"));
        controller.addFirst(component("first"));

        controller.tick();

        assertEquals(List.of("start first", "start a"), starts());
    }

    @Test
    void ac1_addBeforeAndAddAfterPlaceNextToTheReference() {
        final FakeComponent a = component("a");
        final FakeComponent c = component("c");
        controller.add(a);
        controller.add(c);
        controller.addBefore(c, component("b"));
        controller.addAfter(c, component("d"));
        controller.addBefore(a, component("z"));

        controller.tick();

        assertEquals(List.of("start z", "start a", "start b", "start c", "start d"), starts());
    }

    @Test
    void ac1_addBeforeAndAfterAnUnregisteredComponentAddAtTheLowestPriority() {
        final FakeComponent stranger = component("stranger");
        controller.add(component("a"));
        controller.addBefore(stranger, component("b"));
        controller.addAfter(stranger, component("c"));

        controller.tick();

        assertEquals(List.of("start a", "start b", "start c"), starts());
    }

    @Test
    void ac1_priorityFollowsInsertionWhenControlsConflict() {
        final FakeComponent low = component("low", AIControl.MOVE);
        final FakeComponent high = component("high", AIControl.MOVE);
        controller.add(low);
        controller.addFirst(high);

        controller.tick();

        assertTrue(high.isRunning());
        assertFalse(low.isRunning());
    }

    @Test
    void ac2_stopsThenStartsThenTicks() {
        final FakeComponent leaving = component("leaving", AIControl.MOVE);
        final FakeComponent joining = component("joining", AIControl.LOOK);
        joining.canStart = false;
        controller.add(leaving);
        controller.add(joining);
        controller.tick();
        log.clear();

        leaving.shouldContinue = false;
        leaving.canStart = false;
        joining.canStart = true;
        controller.tick();

        assertEquals(List.of("stop leaving", "start joining", "tick joining"), log);
    }

    @Test
    void ac2_startsEligibleComponentsFromHighestToLowestPriority() {
        controller.add(component("a", AIControl.MOVE));
        controller.add(component("b", AIControl.LOOK));
        controller.add(component("c"));

        controller.tick();

        assertEquals(List.of("start a", "start b", "start c"), starts());
    }

    @Test
    void ac2_everyRunningComponentTicksOncePerTick() {
        final FakeComponent a = component("a", AIControl.MOVE);
        final FakeComponent b = component("b", AIControl.LOOK);
        controller.add(a);
        controller.add(b);

        controller.tick();
        controller.tick();
        controller.tick();

        assertEquals(3, a.ticks);
        assertEquals(3, b.ticks);
        assertEquals(1, a.starts);
    }

    @Test
    void ac2_withoutShouldContinueAComponentRunsWhileItCouldStart() {
        final FakeComponent a = component("a", AIControl.MOVE);
        controller.add(a);
        controller.tick();
        controller.tick();
        assertTrue(a.isRunning());

        a.canStart = false;
        controller.tick();

        assertFalse(a.isRunning());
        assertEquals(2, a.ticks);
    }

    @Test
    void ac3_startingTakesControlsFromLowerPriority() {
        final FakeComponent high = component("high", AIControl.MOVE);
        final FakeComponent low = component("low", AIControl.MOVE);
        high.canStart = false;
        controller.add(high);
        controller.add(low);
        controller.tick();
        assertTrue(low.isRunning());

        high.canStart = true;
        controller.tick();

        assertTrue(high.isRunning());
        assertFalse(low.isRunning());
        assertEquals(1, low.stops);
    }

    @Test
    void ac3_startingStopsEveryHolderOfAClaimedControl() {
        final FakeComponent high = component("high", AIControl.MOVE, AIControl.LOOK);
        final FakeComponent mover = component("mover", AIControl.MOVE);
        final FakeComponent looker = component("looker", AIControl.LOOK);
        high.canStart = false;
        controller.add(high);
        controller.add(mover);
        controller.add(looker);
        controller.tick();

        high.canStart = true;
        controller.tick();

        assertTrue(high.isRunning());
        assertFalse(mover.isRunning());
        assertFalse(looker.isRunning());
    }

    @Test
    void ac3_neverTakesAControlFromHigherPriority() {
        final FakeComponent high = component("high", AIControl.MOVE);
        final FakeComponent low = component("low", AIControl.MOVE);
        controller.add(high);
        controller.add(low);

        controller.tick();
        controller.tick();

        assertTrue(high.isRunning());
        assertEquals(0, low.starts);
    }

    @Test
    void ac3_aComponentDoesNotRestartWhileItHoldsItsControls() {
        final FakeComponent only = component("only", AIControl.MOVE);
        controller.add(only);

        controller.tick();
        controller.tick();

        assertEquals(1, only.starts);
        assertEquals(0, only.stops);
    }

    @Test
    void ac3_componentsWithoutControlsNeverConflict() {
        final FakeComponent high = component("high", AIControl.MOVE, AIControl.LOOK, AIControl.TARGET, AIControl.JUMP);
        final FakeComponent free = component("free");
        controller.add(high);
        controller.add(free);

        controller.tick();

        assertTrue(high.isRunning());
        assertTrue(free.isRunning());
    }

    @Test
    void ac4_stopAllStopsEveryRunningComponent() {
        final FakeComponent a = component("a", AIControl.MOVE);
        final FakeComponent b = component("b");
        controller.add(a);
        controller.add(b);
        controller.tick();

        controller.stopAll();

        assertFalse(a.isRunning());
        assertFalse(b.isRunning());
        assertEquals(1, a.stops);
    }

    @Test
    void ac4_stopAllFreesEveryControl() {
        final FakeComponent high = component("high", AIControl.MOVE);
        final FakeComponent low = component("low", AIControl.MOVE);
        controller.add(high);
        controller.add(low);
        controller.tick();

        controller.stopAll();
        high.canStart = false;
        controller.tick();

        assertTrue(low.isRunning());
    }
}
