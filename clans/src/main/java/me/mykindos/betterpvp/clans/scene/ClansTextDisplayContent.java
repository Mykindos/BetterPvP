package me.mykindos.betterpvp.clans.scene;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.display.SceneTextDisplay;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentBinding;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.content.WorldSelector;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

/**
 * Spawns the clans text-display labels in the main world from Mapper data-points.
 * <p>
 * Data-points must be named {@code clans:text_<key>} (e.g. {@code clans:text_class_selector}).
 * Multiple data-points sharing the same name are all spawned, so a single label type can
 * appear at many locations across the map without any extra code.
 * <p>
 * To add a new label: add one {@link LabelDef} entry to {@link #LABELS}. The loading loop
 * is fully generic and never needs to change.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
public class ClansTextDisplayContent implements WorldContent {

    private static final String PREFIX = "clans:text_";
    private static final Map<String, LabelDef> LABELS = Map.ofEntries(
            entry("class_selector", LabelDef.of(1.2f, Display.Billboard.CENTER,
                    text("Class Selector", NamedTextColor.AQUA),
                    text("Right-Click", NamedTextColor.YELLOW))),

            entry("build_editor", LabelDef.of(1.2f, Display.Billboard.CENTER,
                    text("Build Editor", NamedTextColor.RED),
                    text("Right-Click", NamedTextColor.YELLOW)))
    );

    @Inject
    public ClansTextDisplayContent(Clans clans, WorldContentService contentService) {
        contentService.register(clans, new WorldContentBinding(WorldSelector.named(BPvPWorld.MAIN_WORLD_NAME),
                () -> List.of(this)));
    }

    @Override
    public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
        for (Region region : regions.all()) {
            if (!(region instanceof PointRegion p)) continue;
            if (!region.getName().startsWith(PREFIX)) continue;

            final String key = region.getName().substring(PREFIX.length());
            final LabelDef def = LABELS.get(key);
            if (def == null) {
                log.warn("No label definition for key '{}' - skipping data-point '{}'", key, region.getName()).submit();
                continue;
            }

            final Location loc = p.getLocation();
            loc.setWorld(world);
            scope.spawn(new SceneTextDisplay(def.build(), def.scale(), def.billboard()),
                    world.spawn(loc, TextDisplay.class));
        }
    }

    /**
     * Immutable definition of a text label: scale, billboard mode, and one or more
     * text lines that are joined with newlines when built into a single {@link Component}.
     */
    private record LabelDef(float scale, Display.Billboard billboard, List<Component> lines) {

        /**
         * Factory method accepting varargs so call-sites read like a declaration:
         * <pre>
         *   LabelDef.of(1.2f, CENTER,
         *       text("Title",    AQUA),
         *       text("Subtitle", YELLOW))
         * </pre>
         */
        static LabelDef of(float scale, Display.Billboard billboard, Component... lines) {
            return new LabelDef(scale, billboard, List.of(lines));
        }

        /**
         * Joins all lines into one {@link Component} separated by newlines.
         * Single-line labels return the line directly with no trailing newline.
         */
        Component build() {
            Component result = lines.getFirst();
            for (int i = 1; i < lines.size(); i++) {
                result = result.append(newline()).append(lines.get(i));
            }
            return result;
        }
    }

}
