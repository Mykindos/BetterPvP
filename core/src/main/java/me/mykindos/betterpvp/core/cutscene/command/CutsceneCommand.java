package me.mykindos.betterpvp.core.cutscene.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cutscene.Cutscene;
import me.mykindos.betterpvp.core.cutscene.CutsceneManager;
import me.mykindos.betterpvp.core.cutscene.CutsceneRegistry;
import me.mykindos.betterpvp.core.cutscene.CutsceneValidator;
import me.mykindos.betterpvp.core.cutscene.camera.Beat;
import me.mykindos.betterpvp.core.cutscene.camera.CameraMarkers;
import me.mykindos.betterpvp.core.cutscene.skip.CutsceneViewStore;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Authoring and debugging tools for cutscenes.
 * <p>
 * {@code from} is the one that matters most while building: a shot that reads wrong forty seconds in is otherwise
 * forty seconds of watching per attempt, and jumping straight to it turns each iteration into a couple of seconds.
 * {@code markers} answers the other recurring question - whether the id a beat names is actually the id on the marker
 * in front of you.
 */
@Singleton
public class CutsceneCommand extends Command {

    private final CutsceneManager manager;
    private final CutsceneRegistry registry;
    private final CameraMarkers markers;
    private final CutsceneViewStore views;
    private final CutsceneValidator validator;

    @Inject
    private CutsceneCommand(CutsceneManager manager, CutsceneRegistry registry, CameraMarkers markers,
                            CutsceneViewStore views, CutsceneValidator validator) {
        this.manager = manager;
        this.registry = registry;
        this.markers = markers;
        this.views = views;
        this.validator = validator;
    }

    @Override
    public String getName() {
        return "cutscene";
    }

    @Override
    public String getDescription() {
        return "Play, inspect and debug cutscenes";
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            usage(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "play" -> play(player, args);
            case "from" -> from(player, args);
            case "markers" -> listMarkers(player);
            case "list" -> list(player);
            case "stop" -> stop(player);
            case "reload" -> reload(player);
            case "forget" -> forget(player, args);
            case "validate" -> validate(player, args);
            default -> usage(player);
        }
    }

    private void usage(@NotNull Player player) {
        UtilMessage.simpleMessage(player, "Cutscene",
                "Usage: /cutscene <play|from|markers|list|stop|reload|forget|validate> [id] [beat]");
    }

    private void play(@NotNull Player player, String... args) {
        if (args.length < 2) {
            UtilMessage.simpleMessage(player, "Cutscene", "Usage: /cutscene play <id>");
            return;
        }
        if (!manager.start(player, args[1])) {
            UtilMessage.simpleMessage(player, "Cutscene", "Could not start <green>%s</green>.", args[1]);
        }
    }

    /** Plays a cutscene from a named beat, so a shot deep in the timeline can be re-watched on its own. */
    private void from(@NotNull Player player, String... args) {
        if (args.length < 3) {
            UtilMessage.simpleMessage(player, "Cutscene", "Usage: /cutscene from <id> <beat>");
            return;
        }

        final Optional<Cutscene> built = registry.build(player, args[1]);
        if (built.isEmpty()) {
            UtilMessage.simpleMessage(player, "Cutscene", "No cutscene called <green>%s</green>.", args[1]);
            return;
        }

        final Cutscene cutscene = built.get();
        if (!cutscene.hasCamera() || cutscene.getCamera().indexOf(args[2]) < 0) {
            UtilMessage.simpleMessage(player, "Cutscene", "<green>%s</green> has no beat called <green>%s</green>.",
                    args[1], args[2]);
            return;
        }
        if (manager.start(player, cutscene)) {
            manager.jumpTo(player, args[2]);
        }
    }

    /** Every camera marker this world declares, which is the list a beat id has to come from. */
    private void listMarkers(@NotNull Player player) {
        final List<String> ids = new ArrayList<>(markers.ids(player.getWorld()));
        if (ids.isEmpty()) {
            UtilMessage.simpleMessage(player, "Cutscene", "This world declares no <green>camera</green> markers.");
            return;
        }
        ids.sort(String::compareTo);
        UtilMessage.simpleMessage(player, "Cutscene", "<green>%s</green> camera marker(s) in this world:", ids.size());
        for (String id : ids) {
            UtilMessage.simpleMessage(player, "Cutscene", " <gray>-</gray> <green>%s</green>", id);
        }
    }

    private void list(@NotNull Player player) {
        if (registry.all().isEmpty()) {
            UtilMessage.simpleMessage(player, "Cutscene", "Nothing has registered a cutscene.");
            return;
        }
        for (CutsceneRegistry.CutsceneScript script : registry.all()) {
            final boolean seen = views.hasSeen(player.getUniqueId(), script.getId());
            UtilMessage.simpleMessage(player, "Cutscene", "<green>%s</green> <gray>-</gray> %s %s",
                    script.getId(), script.getDisplayName(), seen ? "<gray>(seen)</gray>" : "");
        }
    }

    private void stop(@NotNull Player player) {
        if (!manager.isWatching(player)) {
            UtilMessage.simpleMessage(player, "Cutscene", "You are not watching anything.");
            return;
        }
        manager.stop(player);
    }

    /** Re-reads camera markers, for a builder who has just moved one and saved. */
    private void reload(@NotNull Player player) {
        markers.invalidate(player.getWorld());
        UtilMessage.simpleMessage(player, "Cutscene", "Re-read camera markers for <green>%s</green>.",
                player.getWorld().getName());
    }

    /** Clears a cutscene from this player's history, so a seen-before skip policy treats it as new again. */
    private void forget(@NotNull Player player, String... args) {
        if (args.length < 2) {
            UtilMessage.simpleMessage(player, "Cutscene", "Usage: /cutscene forget <id>");
            return;
        }
        views.forget(player.getUniqueId(), args[1]);
        UtilMessage.simpleMessage(player, "Cutscene", "<green>%s</green> will play as a first viewing again.", args[1]);
    }

    /**
     * Checks one cutscene, or all of them, against this world - waits nothing announces, duplicate beat ids, and
     * camera markers this world does not declare.
     */
    private void validate(@NotNull Player player, String... args) {
        final List<CutsceneRegistry.CutsceneScript> scripts = args.length >= 2
                ? registry.get(args[1]).map(List::of).orElseGet(List::of)
                : List.copyOf(registry.all());
        if (scripts.isEmpty()) {
            UtilMessage.simpleMessage(player, "Cutscene", "Nothing to validate.");
            return;
        }

        int problems = 0;
        for (CutsceneRegistry.CutsceneScript script : scripts) {
            final List<String> issues = validator.validate(script.getFactory().apply(player), player.getWorld());
            problems += issues.size();
            for (String issue : issues) {
                UtilMessage.simpleMessage(player, "Cutscene", "<red>%s</red> <gray>-</gray> %s", script.getId(), issue);
            }
        }
        if (problems == 0) {
            UtilMessage.simpleMessage(player, "Cutscene", "<green>%s</green> cutscene(s) check out in this world.",
                    scripts.size());
        }
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return filter(List.of("play", "from", "markers", "list", "stop", "reload", "forget", "validate"), args);
        }
        if (args.length == 2) {
            return filter(registry.all().stream().map(CutsceneRegistry.CutsceneScript::getId).toList(), args);
        }
        if (args.length == 3 && sender instanceof Player player) {
            return filter(registry.build(player, args[1])
                    .filter(Cutscene::hasCamera)
                    .map(cutscene -> cutscene.getCamera().getBeats().stream()
                            .map(Beat::getId).toList())
                    .orElseGet(List::of), args);
        }
        return List.of();
    }

    private static List<String> filter(@NotNull List<String> candidates, String[] args) {
        final String prefix = args[args.length - 1].toLowerCase();
        return candidates.stream().filter(candidate -> candidate.toLowerCase().startsWith(prefix)).toList();
    }
}
