package me.mykindos.betterpvp.core.menu.dialog.screen;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * What a click does. Built-in actions need no Java. {@link Call} runs a named handler bound when the screen opens or
 * registered globally. Values in {@code args}, {@code set} and {@code state} are templates evaluated against the state.
 */
public sealed interface ActionSpec permits ActionSpec.Call, ActionSpec.Set, ActionSpec.Open, ActionSpec.Back,
        ActionSpec.Close, ActionSpec.Sound, ActionSpec.Sequence {

    @Value
    final class Call implements ActionSpec {
        String name;
        Map<String, String> args;
    }

    /** Changes state keys and re-renders. */
    @Value
    final class Set implements ActionSpec {
        Map<String, String> values;
    }

    /** Opens another screen on top of this one, so {@link Back} returns here. */
    @Value
    final class Open implements ActionSpec {
        String screen;
        Map<String, String> state;
    }

    @Value
    final class Back implements ActionSpec {
    }

    @Value
    final class Close implements ActionSpec {
    }

    @Value
    final class Sound implements ActionSpec {
        String sound;
    }

    /** Runs each action in order. The first one that leaves the screen ends the sequence. */
    @Value
    final class Sequence implements ActionSpec {
        List<ActionSpec> actions;
    }

    static ActionSpec call(String name) {
        return new Call(name, Map.of());
    }

    @Nullable
    static ActionSpec sequence(List<ActionSpec> actions) {
        return actions.isEmpty() ? null : actions.size() == 1 ? actions.getFirst() : new Sequence(List.copyOf(actions));
    }
}
