package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.MapperPlugin;
import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import dev.brauw.mapper.tag.PatternTag;
import dev.brauw.mapper.tag.SimpleTag;
import dev.brauw.mapper.tag.Tag;
import dev.brauw.mapper.tag.TagRegistry;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ModuleReloadLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader;
import me.mykindos.betterpvp.core.scene.loader.ServerStartLoadStrategy;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads resource nodes from {@code scenes/props/*.yml} (one file per node type). For each definition it matches Mapper
 * regions — by tag (every region carrying the tag) or by name — and per match builds the gate {@link Zone}, spawns the
 * {@link ResourceNodeProp} label, and registers the node with {@link ResourceNodeManager}. Reuses the same
 * server-start / module-reload lifecycle as the other clans scene loaders.
 * <p>
 * A file may declare a {@code parent} to inherit from another definition as a template, setting only the keys it wants
 * to override. The parent is referenced either by id ({@code parent: copper_mine} or {@code parent: {id: copper_mine}})
 * or by file ({@code parent: {file: templates/ore_base.yml}}, relative to {@code scenes/props}). Inheritance is a deep
 * merge — nested blocks such as {@code ore:} merge key-by-key, while scalars and lists are replaced wholesale — and
 * chains to any depth. Template-only files (no {@code match} selector) live in a subfolder so they resolve as parents
 * but are never spawned themselves.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
public class ResourceNodeLoader extends SceneObjectLoader {

    private final Clans clans;
    private final ClientManager clientManager;
    private final ZoneManager zoneManager;
    private final SceneObjectRegistry registry;
    private final ResourceNodeFactory factory;
    private final ResourceArchetypeRegistry archetypeRegistry;
    private final ResourceNodeManager manager;
    private final ResourceNodeLabelService labelService;

    private static final String PARENT_KEY = "parent";

    private final List<Zone> registeredZones = new ArrayList<>();
    private boolean tagsRegistered;

    @Inject
    public ResourceNodeLoader(Clans clans, ClientManager clientManager, ZoneManager zoneManager,
                              SceneObjectRegistry registry, ResourceNodeFactory factory,
                              ResourceArchetypeRegistry archetypeRegistry, ResourceNodeManager manager,
                              ResourceNodeLabelService labelService, SceneLoaderManager sceneLoaderManager) {
        this.clans = clans;
        this.clientManager = clientManager;
        this.zoneManager = zoneManager;
        this.registry = registry;
        this.factory = factory;
        this.archetypeRegistry = archetypeRegistry;
        this.manager = manager;
        this.labelService = labelService;
        sceneLoaderManager.register(this, clans);
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        return List.of(new ServerStartLoadStrategy(), new ModuleReloadLoadStrategy());
    }

    @Override
    protected void load() {
        final File folder = new File(clans.getDataFolder(), "scenes/props");
        final File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            log.warn("No resource node files found in {}", folder).submit();
            return;
        }

        // Index every yml (recursively, so template-only files in subfolders count) by id for `parent` resolution.
        // Spawning still only considers top-level files, so a pure template is never loaded as a (broken) node.
        final Map<String, File> templatesById = new HashMap<>();
        indexById(folder, templatesById);

        // Parse every node definition once (paired with its archetype); they are world-agnostic.
        final List<Pair<ResourceNodeDefinition, ResourceArchetype>> definitions = new ArrayList<>();
        for (File file : files) {
            final String id = file.getName().substring(0, file.getName().lastIndexOf('.'));
            final ConfigurationSection config = resolveConfig(file, folder, templatesById, new LinkedHashSet<>());
            if (config == null) {
                continue; // unresolved/cyclic parent - already logged
            }
            final ResourceNodeDefinition definition = ResourceNodeDefinition.from(id, config);
            if (definition == null) {
                log.warn("Resource node file '{}' is missing 'archetype' or a 'match' selector - skipping", file.getName()).submit();
                continue;
            }
            final Optional<ResourceArchetype> archetype = archetypeRegistry.get(definition.getArchetype());
            if (archetype.isEmpty()) {
                log.warn("Resource node '{}' uses unknown archetype '{}' - skipping", id, definition.getArchetype()).submit();
                continue;
            }
            definitions.add(Pair.of(definition, archetype.get()));
        }
        if (definitions.isEmpty()) {
            return;
        }

        // Offer this framework's region tags in Mapper's in-world GUI/commands (one-shot; see registerTags).
        if (!tagsRegistered) {
            registerTags(definitions);
            tagsRegistered = true;
        }

        // Spawn a node for every matching region in every loaded world.
        int nodes = 0;
        for (World world : Bukkit.getWorlds()) {
            final Collection<Region> regions = regionsFor(world);
            if (regions.isEmpty()) {
                continue;
            }
            for (Pair<ResourceNodeDefinition, ResourceArchetype> node : definitions) {
                final ResourceNodeDefinition definition = node.getLeft();
                final ResourceArchetype archetype = node.getRight();
                for (Region region : match(definition, regions, archetype.regionType())) {
                    region.setWorld(world);
                    spawnNode(definition, archetype, region, world, regions);
                    nodes++;
                }
            }
        }
        log.info("Loaded {} resource node(s) from {} file(s) across {} world(s)", nodes, files.length, Bukkit.getWorlds().size()).submit();
    }

    /**
     * Registers this framework's region tags with Mapper so they are offered in the in-world tag GUI/commands when a
     * builder edits a resource region. Mapper offers a tag on a region keyed by the region's <em>name</em>
     * ({@link Tag#supportsRegion}), so we collect every node's binding identifier — its {@code match.name}, or its
     * {@code match.tag} which by convention doubles as the region name — and offer the shared modifier tags
     * ({@code level:NN}, {@code name:...}, {@code label}) across all of them at once. Each tag-bound node additionally
     * contributes its own marker {@link SimpleTag} so the binding tag itself is suggestable.
     * <p>
     * Mapper's {@link TagRegistry} is process-global and append-only (it has no unregister), so this runs once per
     * server lifetime even though {@link #load()} re-runs on module reload — tags for regions added to a map after
     * startup become available on a full restart.
     */
    private void registerTags(@NotNull List<Pair<ResourceNodeDefinition, ResourceArchetype>> definitions) {
        final Set<String> regionNames = new LinkedHashSet<>();
        for (Pair<ResourceNodeDefinition, ResourceArchetype> entry : definitions) {
            final ResourceNodeDefinition definition = entry.getLeft();
            final String regionName = definition.getMatchName();
            if (regionName == null || regionName.isBlank()) {
                continue;
            }
            regionNames.add(regionName);
        }
        if (regionNames.isEmpty()) {
            return;
        }

        final TagRegistry tagRegistry = MapperPlugin.getInstance().getTagRegistry();
        tagRegistry.register(
                new PatternTag("level", "level:\\d+", "level:<number>",
                        "Overrides the required profession level for this placement", true, regionNames),
                new PatternTag("name", "name:.+", "name:<text>",
                        "Overrides the display name for this placement", true, regionNames));
        log.info("Registered resource node tags on {} region name(s)", regionNames.size()).submit();
    }

    private @NotNull Collection<Region> regionsFor(@NotNull World world) {
        try {
            return MapperHelper.getRegions(world);
        } catch (Exception exception) {
            return List.of(); // world has no Mapper data-points
        }
    }

    /** Recursively maps {@code <id> -> file} for every {@code .yml} under {@code folder}, so templates can be referenced by id. */
    private void indexById(@NotNull File folder, @NotNull Map<String, File> out) {
        final File[] entries = folder.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                indexById(entry, out);
            } else if (entry.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                final String id = entry.getName().substring(0, entry.getName().lastIndexOf('.'));
                final File clash = out.putIfAbsent(id, entry);
                if (clash != null) {
                    log.warn("Duplicate resource node id '{}' ({} and {}) - keeping the first for parent resolution",
                            id, clash.getName(), entry.getName()).submit();
                }
            }
        }
    }

    /**
     * Loads {@code file} and, if it declares a {@code parent}, overlays it onto the resolved parent so a file can inherit
     * a template and override only the keys it sets. Returns the file's own config verbatim when it has no parent, or
     * {@code null} if a declared parent is missing or the inheritance chain is cyclic (both logged).
     *
     * @param visiting the chain currently being resolved (canonical files), used to detect cycles
     */
    private @Nullable ConfigurationSection resolveConfig(@NotNull File file, @NotNull File propsFolder,
                                                         @NotNull Map<String, File> templatesById,
                                                         @NotNull Set<File> visiting) {
        final File canonical = file.getAbsoluteFile();
        if (!visiting.add(canonical)) {
            log.warn("Resource node template cycle detected at '{}' - skipping", file.getName()).submit();
            return null;
        }

        final ExtendedYamlConfiguration config = ExtendedYamlConfiguration.loadConfiguration(file);
        final File parentFile = parentFile(file, config, propsFolder, templatesById);
        if (parentFile == null) {
            return config.isSet(PARENT_KEY) ? null : config; // declared-but-unresolved already logged in parentFile()
        }

        final ConfigurationSection parent = resolveConfig(parentFile, propsFolder, templatesById, visiting);
        if (parent == null) {
            return null; // broken parent chain - don't spawn a half-configured node
        }

        final YamlConfiguration merged = new YamlConfiguration();
        deepMerge(merged, parent);  // template first...
        deepMerge(merged, config);  // ...then this file's overrides win
        merged.set(PARENT_KEY, null); // strip the control key so it never reaches archetype parsing
        return merged;
    }

    /** Resolves a file's {@code parent} declaration (scalar id, {@code parent.id}, or {@code parent.file}) to a file. */
    private @Nullable File parentFile(@NotNull File file, @NotNull ConfigurationSection config,
                                      @NotNull File propsFolder, @NotNull Map<String, File> templatesById) {
        String byId = null;
        String byFile = null;
        if (config.isConfigurationSection(PARENT_KEY)) {
            final ConfigurationSection parent = config.getConfigurationSection(PARENT_KEY);
            byId = parent.getString("id");
            byFile = parent.getString("file");
        } else if (config.isString(PARENT_KEY)) {
            byId = config.getString(PARENT_KEY); // shorthand: a bare string is an id
        } else if (!config.isSet(PARENT_KEY)) {
            return null;
        }

        if (byFile != null && !byFile.isBlank()) {
            final String relative = byFile.toLowerCase(Locale.ROOT).endsWith(".yml") ? byFile : byFile + ".yml";
            final File resolved = new File(propsFolder, relative);
            if (!resolved.isFile()) {
                log.warn("Resource node '{}' references missing parent file '{}' - skipping", file.getName(), byFile).submit();
                return null;
            }
            return resolved;
        }
        if (byId != null && !byId.isBlank()) {
            final File resolved = templatesById.get(byId);
            if (resolved == null) {
                log.warn("Resource node '{}' references unknown parent id '{}' - skipping", file.getName(), byId).submit();
            }
            return resolved;
        }
        log.warn("Resource node '{}' has an empty 'parent' declaration - skipping", file.getName()).submit();
        return null;
    }

    /** Deep-merges {@code overlay} into {@code base}: nested sections recurse; scalars and lists are replaced wholesale. */
    private static void deepMerge(@NotNull ConfigurationSection base, @NotNull ConfigurationSection overlay) {
        for (String key : overlay.getKeys(false)) {
            if (overlay.isConfigurationSection(key)) {
                final ConfigurationSection target = base.isConfigurationSection(key)
                        ? base.getConfigurationSection(key) : base.createSection(key);
                deepMerge(target, overlay.getConfigurationSection(key));
            } else {
                base.set(key, overlay.get(key));
            }
        }
    }

    private List<Region> match(@NotNull ResourceNodeDefinition definition, @NotNull Collection<Region> regions,
                               @NotNull Region.RegionType type) {
        final Class<? extends Region> regionClass =
                type == Region.RegionType.PERSPECTIVE ? PerspectiveRegion.class : CuboidRegion.class;
        if (definition.getMatchName() != null) {
            return new ArrayList<>(MapperHelper.findRegions(regions, definition.getMatchName(), regionClass));
        }
        return List.of();
    }

    private void spawnNode(@NotNull ResourceNodeDefinition definition, @NotNull ResourceArchetype archetype,
                           @NotNull Region region, @NotNull World world, @NotNull Collection<Region> regions) {
        final CuboidRegion bounds = archetype.zoneBounds(definition, region);
        bounds.setWorld(world);
        final RegionTags tags = new RegionTags(region.getOptions().getTags());
        final int level = tags.getInt("level", definition.getLevel());
        final String displayName = tags.getString("name", definition.getDisplayName());

        final ZoneRuleContainer rules = new ZoneRuleContainer();
        rules.add(new ResourceNodeRule(clientManager));

        final Zone.ZoneBuilder builder = Zone.builder()
                .key(nodeKey(region))
                .displayName(Component.text(displayName))
                .bounds(RegionBounds.of(bounds))
                .priority(ClanZones.SERVER_REGION_PRIORITY + 5)
                .tag("resource_node");
        archetype.zoneTags().forEach(builder::tag);
        final Zone zone = builder.rules(rules).build();
        zoneManager.register(zone);
        registeredZones.add(zone);

        final Component label = Component.text(displayName, NamedTextColor.GREEN)
                .appendNewline()
                .append(Component.text("Level " + level, NamedTextColor.GRAY));

        final List<Location> labelLocations = resolveLabelLocations(bounds, regions, world);
        final ResourceNodeProp prop = new ResourceNodeProp(factory, definition, archetype, region, level, zone, label, labelLocations, labelService);

        // Capture the archetype's world state once at load (ore snapshot / tree placement) - it works off the region and
        // definition, not the label entity, so it runs before the prop materializes and persists across chunk cycles.
        archetype.onActivate(prop);

        // Chunk-managed: the label entities and respawn tick (re)materialize with the node's chunk; the first label is
        // re-spawned by this factory on every materialization. Harvest routing (manager) is keyed by the stable zone, so
        // it is registered once here and works regardless of the labels' materialization state.
        spawn(prop, labelLocations.getFirst(), loc -> loc.getWorld().spawn(loc, TextDisplay.class), registry);
        manager.register(prop);
    }

    /**
     * Resolves where a node's labels go: every Mapper point/perspective marker tagged {@code label} that lies inside
     * the node's region (full 3D containment, the same test zone membership uses, so nodes can share x/z columns).
     * Falls back to a single auto-computed label above the region centre when no markers are present.
     */
    private @NotNull List<Location> resolveLabelLocations(@NotNull CuboidRegion region,
                                                          @NotNull Collection<Region> regions, @NotNull World world) {
        final List<Location> markers = new ArrayList<>();
        for (Region other : regions) {
            if (!(other instanceof PointRegion point)) {
                continue;
            }
            if (!other.getName().equals("label")) {
                continue;
            }
            final Location location = point.getLocation();
            if (location == null) {
                continue;
            }
            // Re-home onto the node's world so the region's world-aware containment check matches.
            final Location worldLocation = new Location(world, location.getX(), location.getY(), location.getZ());
            if (region.contains(worldLocation)) {
                markers.add(worldLocation);
            }
        }
        return markers.isEmpty() ? List.of(labelLocation(region)) : markers;
    }

    private static @NotNull Location labelLocation(@NotNull CuboidRegion region) {
        final Location min = region.getMin();
        final Location max = region.getMax();
        final double x = (min.getX() + max.getX()) / 2.0 + 0.5;
        final double z = (min.getZ() + max.getZ()) / 2.0 + 0.5;
        final double y = max.getY() + 2.0;
        return new Location(min.getWorld(), x, y, z);
    }

    private static @NotNull Key nodeKey(@NotNull Region region) {
        return Key.key("clans", "resource_" + region.getId().toString().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void unload() {
        registeredZones.forEach(zoneManager::unregister);
        registeredZones.clear();
        manager.clear();
        labelService.clear();
    }
}
