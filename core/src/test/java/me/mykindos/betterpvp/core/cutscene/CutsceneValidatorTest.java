package me.mykindos.betterpvp.core.cutscene;

import me.mykindos.betterpvp.core.cutscene.camera.CameraMarkers;
import me.mykindos.betterpvp.core.cutscene.camera.Easing;
import me.mykindos.betterpvp.core.cutscene.signal.Signal;
import me.mykindos.betterpvp.core.quest.conversation.Conversations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The validator exists for one failure in particular: a wait nothing will ever release does not throw, it just leaves
 * the player behind a camera forever. These check that the set difference actually catches it.
 */
@DisplayName("Cutscene validator")
class CutsceneValidatorTest {

    private static CutsceneValidator validator() {
        // Only the world-free checks are exercised here; marker lookup needs a live world and is checked in-game.
        return new CutsceneValidator(new CutsceneRegistry(), mock(CameraMarkers.class));
    }

    @Test
    @DisplayName("accepts a beat waiting on a node the dialogue actually has")
    void acceptsReachableWait() {
        final Cutscene cutscene = Cutscene.builder("tour")
                .dialogue(Conversations.builder("tour")
                        .node("greet", node -> node.speaker("Guide").body("Welcome."))
                        .build())
                .camera(camera -> camera
                        .shot("gate", shot -> shot.cut().until(Signal.nodeEnd("greet"))))
                .build();

        assertEquals(List.of(), validator().validate(cutscene));
    }

    @Test
    @DisplayName("catches a beat waiting on a node that does not exist")
    void catchesUnreachableWait() {
        // The renamed-node case: nothing errors at build time, and at runtime the camera simply never moves again.
        final Cutscene cutscene = Cutscene.builder("tour")
                .dialogue(Conversations.builder("tour")
                        .node("greet", node -> node.speaker("Guide").body("Welcome."))
                        .build())
                .camera(camera -> camera
                        .shot("gate", shot -> shot.cut().until(Signal.nodeEnd("mine_intro"))))
                .build();

        final List<String> issues = validator().validate(cutscene);
        assertEquals(1, issues.size());
        assertTrue(issues.getFirst().contains("node:mine_intro:end"), issues.toString());
    }

    @Test
    @DisplayName("catches a node waiting on a beat that does not exist")
    void catchesUnreachableNodeAwait() {
        final Cutscene cutscene = Cutscene.builder("tour")
                .dialogue(Conversations.builder("tour")
                        .node("greet", node -> node.speaker("Guide").body("Welcome.")
                                .await(Signal.beatKey("harbour")))
                        .build())
                .camera(camera -> camera.shot("gate", shot -> shot.cut()))
                .build();

        final List<String> issues = validator().validate(cutscene);
        assertEquals(1, issues.size());
        assertTrue(issues.getFirst().contains("beat:harbour"), issues.toString());
    }

    @Test
    @DisplayName("catches two beats sharing an id")
    void catchesDuplicateBeatIds() {
        // Duplicate ids make every signal about them ambiguous - a wait on one is released by the other.
        final Cutscene cutscene = Cutscene.builder("tour")
                .camera(camera -> camera
                        .shot("gate", shot -> shot.cut())
                        .shot("gate", shot -> shot.travel(1, Easing.LINEAR)))
                .build();

        final List<String> issues = validator().validate(cutscene);
        assertEquals(1, issues.size());
        assertTrue(issues.getFirst().contains("two beats"), issues.toString());
    }

    @Test
    @DisplayName("a beat waiting on another beat is fine")
    void beatWaitingOnBeat() {
        final Cutscene cutscene = Cutscene.builder("tour")
                .camera(camera -> camera
                        .shot("gate", shot -> shot.cut().until(Signal.beat("harbour")))
                        .shot("harbour", shot -> shot.travel(2, Easing.EASE_IN_OUT)))
                .build();

        assertEquals(List.of(), validator().validate(cutscene));
    }

    @Test
    @DisplayName("flags a cutscene with neither track")
    void flagsEmptyCutscene() {
        assertEquals(1, validator().validate(Cutscene.builder("nothing").build()).size());
    }
}
