package me.mykindos.betterpvp.core.cutscene;

import com.google.inject.Singleton;
import lombok.Value;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every cutscene the code declares, addressable by id.
 * <p>
 * A cutscene is registered as a <em>factory</em> rather than an instance, for the same reason
 * {@link me.mykindos.betterpvp.core.quest.conversation.Conversations} recommends building dialogue per interaction: its
 * gates and its text routinely depend on who is watching, and a factory can have answered those questions before the
 * first frame instead of from inside a predicate that cannot wait.
 * <p>
 * This registry is also what the admin console sees. Cutscenes are code, so they reach the console through the game
 * manifest - the same route items and zones take - rather than through the content table. A console-authored quest or
 * conversation therefore points at a cutscene by an id that is guaranteed to exist, and there is no publish step
 * between writing one and being able to reference it.
 */
@Singleton
public class CutsceneRegistry {

    private final Map<String, CutsceneScript> scripts = new LinkedHashMap<>();

    /**
     * @param id          what quests, conversations and commands name this cutscene by
     * @param displayName how it reads in the console's picker
     * @param factory     builds the cutscene for one viewer, at the moment they start it
     */
    public void register(@NotNull String id, @NotNull String displayName, @NotNull Function<Player, Cutscene> factory) {
        scripts.put(id.toLowerCase(Locale.ROOT), new CutsceneScript(id, displayName, factory));
    }

    public @NotNull Optional<CutsceneScript> get(@NotNull String id) {
        return Optional.ofNullable(scripts.get(id.toLowerCase(Locale.ROOT)));
    }

    /** Builds this cutscene for one viewer, if it is registered. */
    public @NotNull Optional<Cutscene> build(@NotNull Player viewer, @NotNull String id) {
        return get(id).map(script -> script.getFactory().apply(viewer));
    }

    public @NotNull Collection<CutsceneScript> all() {
        return Collections.unmodifiableCollection(scripts.values());
    }

    @Value
    public static class CutsceneScript {
        @NotNull String id;
        @NotNull String displayName;
        @NotNull Function<Player, Cutscene> factory;
    }
}
