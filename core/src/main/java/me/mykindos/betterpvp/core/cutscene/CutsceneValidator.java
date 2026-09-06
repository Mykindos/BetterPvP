package me.mykindos.betterpvp.core.cutscene;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.cutscene.camera.Beat;
import me.mykindos.betterpvp.core.cutscene.camera.CameraMarkers;
import me.mykindos.betterpvp.core.cutscene.signal.Signal;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.conversation.ConvNode;
import me.mykindos.betterpvp.core.quest.conversation.ConvResponse;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Checks cutscenes for the mistakes that are invisible until somebody plays one.
 * <p>
 * The expensive failure in a rendezvous design is a wait that nothing will ever release: a beat holding for
 * {@code node:mine_intro} after the node was renamed does not error, it simply never advances, and the player is stuck
 * behind a camera with no way out. Every wait and every announcement in a cutscene is enumerable, so that whole class
 * of bug is a set difference done at boot rather than a bug report.
 * <p>
 * Marker checks need a world and worlds arrive later than boot, so those run on demand from {@code /cutscene validate}
 * in the world being authored - which is also where the answer is actually wanted.
 */
@Singleton
@BPvPListener
@CustomLog
public class CutsceneValidator implements Listener {

    private final CutsceneRegistry registry;
    private final CameraMarkers markers;

    @Inject
    public CutsceneValidator(CutsceneRegistry registry, CameraMarkers markers) {
        this.registry = registry;
        this.markers = markers;
    }

    /** Everything wrong with this cutscene that can be told without a world. */
    public @NotNull List<String> validate(@NotNull Cutscene cutscene) {
        final List<String> issues = new ArrayList<>();
        final Set<String> emitted = emittedSignals(cutscene);

        if (!cutscene.hasCamera() && !cutscene.hasDialogue()) {
            issues.add("has neither a camera track nor a dialogue track, so nothing would happen");
        }

        if (cutscene.hasCamera()) {
            final Set<String> beatIds = new HashSet<>();
            for (Beat beat : cutscene.getCamera().getBeats()) {
                if (!beatIds.add(beat.getId().toLowerCase(Locale.ROOT))) {
                    // Two beats sharing an id makes their signals ambiguous: a wait on one is released by the other.
                    issues.add("has two beats called '" + beat.getId() + "'");
                }
                final Signal awaits = beat.getAwaits();
                if (awaits != null && !emitted.contains(awaits.getKey())) {
                    issues.add("beat '" + beat.getId() + "' waits for '" + awaits.getKey()
                            + "', which nothing in this cutscene announces");
                }
            }
        }

        if (cutscene.hasDialogue()) {
            for (ConvNode node : cutscene.getDialogue().getNodes()) {
                final String await = node.getData().getAwait();
                if (await != null && !await.isBlank() && !emitted.contains(await.toLowerCase(Locale.ROOT))) {
                    issues.add("node '" + node.getId() + "' waits for '" + await
                            + "', which nothing in this cutscene announces");
                }
            }
        }
        return issues;
    }

    /** As {@link #validate(Cutscene)}, plus whether this world actually declares the camera markers it names. */
    public @NotNull List<String> validate(@NotNull Cutscene cutscene, @NotNull World world) {
        final List<String> issues = validate(cutscene);
        if (cutscene.hasCamera()) {
            final Set<String> declared = markers.ids(world);
            for (Beat beat : cutscene.getCamera().getBeats()) {
                if (!declared.contains(beat.getId().toLowerCase(Locale.ROOT))) {
                    issues.add("beat '" + beat.getId() + "' has no 'camera' marker with that id in "
                            + world.getName());
                }
            }
        }
        return issues;
    }

    /**
     * Everything this cutscene announces: what its beats emit on arrival and departure, what its dialogue emits on
     * entering, leaving and answering each node, plus the two that are always available.
     */
    private @NotNull Set<String> emittedSignals(@NotNull Cutscene cutscene) {
        final Set<String> emitted = new HashSet<>();
        emitted.add(Signal.input().getKey());
        emitted.add(Signal.conversationEnd().getKey());

        if (cutscene.hasCamera()) {
            for (Beat beat : cutscene.getCamera().getBeats()) {
                emitted.add(Signal.beat(beat.getId()).getKey());
                emitted.add(Signal.beatEnd(beat.getId()).getKey());
            }
        }
        if (cutscene.hasDialogue()) {
            for (ConvNode node : cutscene.getDialogue().getNodes()) {
                emitted.add(Signal.node(node.getId()).getKey());
                emitted.add(Signal.nodeEnd(node.getId()).getKey());
                for (ConvResponse response : node.getData().getResponses()) {
                    if (response.getId() != null && !response.getId().isBlank()) {
                        emitted.add(Signal.response(node.getId(), response.getId()).getKey());
                    }
                }
            }
        }
        return emitted;
    }

    /** Reports every registered cutscene's structural problems once, at boot, rather than on first play. */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        for (CutsceneRegistry.CutsceneScript script : registry.all()) {
            final Cutscene cutscene = build(script);
            if (cutscene == null) {
                continue;
            }
            for (String issue : validate(cutscene)) {
                log.warn("Cutscene '{}' {}", script.getId(), issue).submit();
            }
        }
    }

    /**
     * A cutscene built with no viewer, for inspection.
     *
     * @return null if its factory cannot build without one, which is allowed - it just cannot be checked here
     */
    private @Nullable Cutscene build(@NotNull CutsceneRegistry.CutsceneScript script) {
        try {
            return script.getFactory().apply(null);
        } catch (RuntimeException exception) {
            log.info("Cutscene '{}' needs a viewer to build, so it was not checked at boot", script.getId()).submit();
            return null;
        }
    }
}
