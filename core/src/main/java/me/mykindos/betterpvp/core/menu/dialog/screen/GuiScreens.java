package me.mykindos.betterpvp.core.menu.dialog.screen;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.dialog.DialogClick;
import me.mykindos.betterpvp.core.menu.dialog.DialogSessions;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Loads screens and opens them for players. Each player has a stack of open screens, so {@code open} actions stack
 * and {@code back} returns. See {@code docs/core-gui-screens.md}.
 */
@BPvPListener
@Singleton
@CustomLog
public class GuiScreens implements Listener {

    /** The files the pack generator reads too: screens, components and asset files, no subfolders. */
    private static final Pattern SCREEN_FILE = Pattern.compile("gui/[^/]+\\.json");
    private static final Pattern COMPONENT_FILE = Pattern.compile("gui/components/[^/]+\\.json");
    private static final Pattern ASSET_FILE = Pattern.compile("gui/assets/[^/]+\\.json");

    private final GuiRegistry registry;
    private final DialogSessions sessions;
    private final Map<String, ScreenDefinition> screens = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> declared = new ConcurrentHashMap<>();
    private final Map<String, AssetTable> tables = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Frame>> stacks = new ConcurrentHashMap<>();

    @Inject
    public GuiScreens(GuiRegistry registry, DialogSessions sessions) {
        this.registry = registry;
        this.sessions = sessions;
    }

    /**
     * Loads a namespace's files: screens ({@code gui/*.json}), components ({@code gui/components/*.json}) and asset
     * files ({@code gui/assets/*.json}), given as file name to content. Call {@link #validate()} once every plugin has
     * registered its extensions.
     */
    public void load(String namespace, Map<String, String> files) {
        final Set<String> assets = new HashSet<>();
        files.forEach((path, json) -> {
            if (COMPONENT_FILE.matcher(path).matches()) {
                guard(path, () -> ScreenParser.components(namespace, path, json)
                        .forEach((name, component) -> registry.component(namespace, name, component)));
            }
        });
        files.forEach((path, json) -> {
            if (ASSET_FILE.matcher(path).matches()) {
                guard(path, () -> assets.addAll(ScreenAssets.collect(ScreenParser.assets(namespace, path, json), this::component)));
            } else if (SCREEN_FILE.matcher(path).matches()) {
                guard(path, () -> {
                    final ScreenDefinition screen = ScreenParser.screen(namespace, path, json);
                    assets.addAll(ScreenAssets.collect(screen, this::component));
                    screens.put(screen.key(), screen);
                });
            }
        });
        declared.put(namespace, assets);
        tables.put(namespace, new AssetTable(namespace, assets));
    }

    /** A broken file is logged and skipped, so one typo never stops the plugin from enabling. */
    private void guard(String path, Runnable load) {
        try {
            load.run();
        } catch (RuntimeException e) {
            log.error("Could not load GUI file {}: {}", path, e.getMessage()).submit();
        }
    }

    /** Loads every {@code gui/} file packaged with a plugin, under a namespace. */
    public void load(String namespace, Class<?> owner) {
        final Map<String, String> files = new LinkedHashMap<>();
        try {
            final URI location = owner.getProtectionDomain().getCodeSource().getLocation().toURI();
            final Path root = Path.of(location);
            if (Files.isDirectory(root)) {
                // Run from a build folder, where classes and resources sit in separate directories.
                final URL folder = owner.getClassLoader().getResource("gui");
                if (folder != null) {
                    final Path gui = Path.of(folder.toURI());
                    try (Stream<Path> paths = Files.walk(gui)) {
                        for (Path path : paths.filter(path -> path.toString().endsWith(".json")).toList()) {
                            files.put("gui/" + gui.relativize(path).toString().replace('\\', '/'), Files.readString(path));
                        }
                    }
                }
            } else {
                try (JarFile jar = new JarFile(root.toFile())) {
                    for (JarEntry entry : Collections.list(jar.entries())) {
                        if (entry.getName().startsWith("gui/") && entry.getName().endsWith(".json")) {
                            try (InputStream input = jar.getInputStream(entry)) {
                                files.put(entry.getName(), new String(input.readAllBytes(), StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Could not read the gui files of " + namespace, e);
        }
        load(namespace, files);
    }

    /** Adds a screen built in code. Its art must already be declared by the namespace's files. */
    public void register(ScreenDefinition screen) {
        screens.put(screen.key(), screen);
    }

    @Nullable
    public ScreenDefinition definition(String key) {
        return screens.get(key);
    }

    /** Problems with every loaded screen, each naming the screen. Empty when all are valid. */
    public List<String> validate() {
        final List<String> problems = new ArrayList<>();
        for (ScreenDefinition screen : screens.values()) {
            for (String problem : validate(screen)) {
                problems.add(screen.key() + ": " + problem);
            }
        }
        return problems;
    }

    /** Problems with one screen: art the pack will not have, unknown components, element types or actions. */
    public List<String> validate(ScreenDefinition screen) {
        final List<String> problems = new ArrayList<>();
        final Set<String> assets;
        try {
            assets = ScreenAssets.collect(screen, this::component);
        } catch (IllegalArgumentException e) {
            return List.of(e.getMessage());
        }
        final Set<String> available = declared.getOrDefault(screen.getNamespace(), Set.of());
        assets.stream().filter(asset -> !available.contains(asset)).sorted()
                .forEach(asset -> problems.add("art " + asset + " is not declared in a screen, component or asset file"));
        for (String asset : assets) {
            if (ScreenAssets.height(asset) > ScreenAssets.MAX_GLYPH) {
                problems.add("art " + asset + " is taller than " + ScreenAssets.MAX_GLYPH + " px");
            }
            if ((asset.startsWith("hover:") || asset.startsWith("anim:")) && ScreenAssets.width(asset) > ScreenAssets.MAX_GLYPH - 2) {
                problems.add("art " + asset + " is wider than " + (ScreenAssets.MAX_GLYPH - 2) + " px");
            }
            if (asset.startsWith("anim:") && ScreenAssets.height(asset) * Integer.parseInt(asset.split(":")[3]) > ScreenAssets.MAX_GLYPH) {
                problems.add("art " + asset + " is a frame strip taller than " + ScreenAssets.MAX_GLYPH + " px");
            }
        }
        if (!screen.getBackdrop().isEmpty() && screen.getCanvasHeight() > ScreenAssets.BACKDROP_MAX_HEIGHT) {
            problems.add("the canvas is " + screen.getCanvasHeight() + " px tall with a backdrop, which only lines up up to "
                    + ScreenAssets.BACKDROP_MAX_HEIGHT + " px. Draw the art as box elements in the canvas instead");
        }
        new Checker(screen, problems).walk(screen.getElements());
        return problems;
    }

    public void open(Player player, String key, Map<String, Object> state, Map<String, ActionHandler> bindings) {
        final ScreenDefinition screen = screens.get(key);
        if (screen == null) {
            throw new IllegalArgumentException("No screen " + key);
        }
        open(player, screen, state, bindings);
    }

    /** Opens a screen, replacing whatever screens the player has open. */
    public void open(Player player, ScreenDefinition screen, Map<String, Object> state, Map<String, ActionHandler> bindings) {
        final Deque<Frame> stack = new ArrayDeque<>();
        stack.push(frame(screen, state, bindings));
        stacks.put(player.getUniqueId(), stack);
        playSound(player, screen, "open");
        render(player);
    }

    public void close(Player player) {
        final Deque<Frame> stack = stacks.remove(player.getUniqueId());
        if (stack != null && !stack.isEmpty()) {
            playSound(player, stack.peek().screen, "close");
        }
        sessions.close(player);
    }

    /** The state of the screen the player sees, or null. */
    @Nullable
    public ScreenState state(Player player) {
        final Deque<Frame> stack = stacks.get(player.getUniqueId());
        return stack == null || stack.isEmpty() ? null : stack.peek().state;
    }

    /** Re-renders the player's screen, after Java changed its state from outside an action. */
    public void refresh(Player player) {
        if (stacks.containsKey(player.getUniqueId())) {
            render(player);
        }
    }

    /** Every plugin has registered its extensions by now, so named actions, styles and types can be checked. */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        validate().forEach(problem -> log.error("GUI screen problem: {}", problem).submit());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stacks.remove(event.getPlayer().getUniqueId());
    }

    private Frame frame(ScreenDefinition screen, Map<String, Object> state, Map<String, ActionHandler> bindings) {
        final Map<String, Object> values = new LinkedHashMap<>(screen.getState());
        values.putAll(state);
        return new Frame(screen, new ScreenState(values), Map.copyOf(bindings));
    }

    private void render(Player player) {
        final Frame frame = stacks.get(player.getUniqueId()).peek();
        final AssetTable table = tables.computeIfAbsent(frame.screen.getNamespace(), namespace -> new AssetTable(namespace, Set.of()));
        sessions.open(player, ScreenRenderer.render(player, frame.screen, frame.state, registry, table,
                (action, scope) -> click(frame, action, scope)));
    }

    private DialogClick click(Frame frame, ActionSpec action, Function<String, Object> scope) {
        return (player, inputs) -> {
            final Deque<Frame> stack = stacks.get(player.getUniqueId());
            if (stack == null || stack.peek() != frame) {
                return;
            }
            inputs.asMap().forEach(frame.state::set);
            frame.state.set("error", null);
            playSound(player, frame.screen, "click");
            apply(player, frame, run(player, frame, action, scope));
        };
    }

    /** Runs an action. Built-ins become results directly, named actions call their handler. */
    private ActionResult run(Player player, Frame frame, ActionSpec action, Function<String, Object> scope) {
        return switch (action) {
            case ActionSpec.Call call -> {
                ActionHandler handler = frame.bindings.get(call.getName());
                if (handler == null) {
                    handler = registry.action(frame.screen.getNamespace(), call.getName());
                }
                if (handler == null) {
                    throw new IllegalStateException(frame.screen.key() + " calls " + call.getName() + ", which is neither bound nor registered");
                }
                final Map<String, Object> args = new HashMap<>();
                call.getArgs().forEach((name, template) -> args.put(name, evaluate(template, scope)));
                yield handler.handle(new ActionHandler.Context(player, frame.state, args));
            }
            case ActionSpec.Set set -> {
                set.getValues().forEach((key, template) -> frame.state.set(key, evaluate(template, scope)));
                yield ActionResult.update();
            }
            case ActionSpec.Open open -> {
                final Map<String, Object> state = new HashMap<>();
                open.getState().forEach((key, template) -> state.put(key, evaluate(template, scope)));
                yield ActionResult.open(open.getScreen(), state);
            }
            case ActionSpec.Back back -> ActionResult.back();
            case ActionSpec.Close close -> ActionResult.close();
            case ActionSpec.Sound sound -> {
                player.playSound(Sound.sound(Key.key(sound.getSound()), Sound.Source.MASTER, 1f, 1f));
                yield ActionResult.none();
            }
            case ActionSpec.Sequence sequence -> {
                ActionResult result = ActionResult.none();
                for (ActionSpec step : sequence.getActions()) {
                    result = run(player, frame, step, scope);
                    if (result.getKind() == ActionResult.Kind.OPEN || result.getKind() == ActionResult.Kind.BACK
                            || result.getKind() == ActionResult.Kind.CLOSE || result.getKind() == ActionResult.Kind.ERROR) {
                        yield result;
                    }
                    // Applied here so the next step sees it. The sequence then re-renders once.
                    if (result.getKind() == ActionResult.Kind.UPDATE && result.getChange() != null) {
                        result.getChange().accept(frame.state);
                    }
                }
                yield ActionResult.update();
            }
        };
    }

    private void apply(Player player, Frame frame, ActionResult result) {
        final Deque<Frame> stack = stacks.get(player.getUniqueId());
        switch (result.getKind()) {
            case NONE -> {
            }
            case UPDATE -> {
                if (result.getChange() != null) {
                    result.getChange().accept(frame.state);
                }
                render(player);
            }
            case ERROR -> {
                frame.state.set("error", Translations.render(Translations.component(result.getErrorKey()), player.locale()));
                playSound(player, frame.screen, "error");
                render(player);
            }
            case OPEN -> {
                final String key = result.getScreen().contains(":") ? result.getScreen() : frame.screen.getNamespace() + ":" + result.getScreen();
                final ScreenDefinition next = screens.get(key);
                if (next == null) {
                    throw new IllegalStateException(frame.screen.key() + " opens " + key + ", which is not loaded");
                }
                stack.push(frame(next, result.getState(), frame.bindings));
                playSound(player, next, "open");
                render(player);
            }
            case BACK -> {
                stack.pop();
                if (stack.isEmpty()) {
                    close(player);
                } else {
                    render(player);
                }
            }
            case CLOSE -> close(player);
        }
    }

    private Object evaluate(String template, Function<String, Object> scope) {
        return Expressions.evaluate(template, scope, registry.formatters());
    }

    private void playSound(Player player, ScreenDefinition screen, String event) {
        final String sound = screen.getSounds().containsKey(event) ? screen.getSounds().get(event)
                : registry.sound(screen.getNamespace(), event);
        if (sound != null) {
            player.playSound(Sound.sound(Key.key(sound), Sound.Source.MASTER, 1f, 1f));
        }
    }

    private ScreenDefinition.Component component(ScreenDefinition screen, String name) {
        final ScreenDefinition.Component own = screen.getComponents().get(name);
        return own != null ? own : registry.component(screen.getNamespace(), name);
    }

    private static final class Frame {
        private final ScreenDefinition screen;
        private final ScreenState state;
        private final Map<String, ActionHandler> bindings;

        private Frame(ScreenDefinition screen, ScreenState state, Map<String, ActionHandler> bindings) {
            this.screen = screen;
            this.state = state;
            this.bindings = bindings;
        }
    }

    /** Checks the names a screen refers to: actions, element types and text styles. */
    private final class Checker {
        private final ScreenDefinition screen;
        private final List<String> problems;

        private Checker(ScreenDefinition screen, List<String> problems) {
            this.screen = screen;
            this.problems = problems;
        }

        void walk(List<Node> nodes) {
            nodes.forEach(this::walk);
        }

        private void walk(Node node) {
            switch (node) {
                case Node.Text text -> {
                    style(text.getStyle());
                    action(text.getOnClick());
                }
                case Node.Box box -> {
                }
                case Node.Button button -> {
                    style(button.getLabelStyle());
                    if (button.getSelectedLabelStyle() != null) {
                        style(button.getSelectedLabelStyle());
                    }
                    action(button.getOnClick());
                    if (button.getWidth() > ScreenAssets.MAX_GLYPH) {
                        problems.add("button " + button.getStyle() + " is wider than " + ScreenAssets.MAX_GLYPH + " px");
                    }
                }
                case Node.Icon icon -> action(icon.getOnClick());
                case Node.Group group -> walk(group.getChildren());
                case Node.Repeat repeat -> walk(repeat.getChildren());
                case Node.Switch choice -> {
                    choice.getCases().values().forEach(this::walk);
                    walk(choice.getFallback());
                }
                case Node.When when -> {
                    walk(when.getThen());
                    walk(when.getOtherwise());
                }
                case Node.Use use -> {
                    final ScreenDefinition.Component component = component(screen, use.getComponent());
                    if (component != null) {
                        walk(component.getElements());
                    }
                }
                case Node.Custom custom -> {
                    final ElementType type = registry.elementType(screen.getNamespace(), custom.getType());
                    if (type == null) {
                        problems.add("element type " + custom.getType() + " is not registered");
                    } else {
                        type.validate(custom).forEach(problem -> problems.add(custom.getType() + ": " + problem));
                    }
                }
            }
        }

        private void style(String name) {
            if (registry.textStyle(screen.getNamespace(), name) == null) {
                problems.add("text style " + name + " is not registered");
            }
        }

        private void action(@Nullable ActionSpec action) {
            switch (action) {
                case null -> {
                }
                case ActionSpec.Call call -> {
                    if (!screen.getActions().contains(call.getName()) && registry.action(screen.getNamespace(), call.getName()) == null) {
                        problems.add("action " + call.getName() + " is neither listed in \"actions\" nor registered");
                    }
                }
                case ActionSpec.Sequence sequence -> sequence.getActions().forEach(this::action);
                default -> {
                }
            }
        }
    }
}
