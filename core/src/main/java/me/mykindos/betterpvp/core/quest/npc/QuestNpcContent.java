package me.mykindos.betterpvp.core.quest.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.SceneEntity;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectFactoryManager;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.behavior.TagBehavior;
import me.mykindos.betterpvp.core.scene.npc.HumanNMS;
import me.mykindos.betterpvp.core.scene.npc.HumanNPC;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentBinding;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.content.WorldSelector;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Spawns quest-giver NPCs from Mapper data-points in every world. A {@link PointRegion} (or its subclass
 * {@code PerspectiveRegion}) named with a quest NPC id is spawned as that NPC: either via a registered factory + type
 * ({@code source=factory}, like {@code /npc spawn}) or as a skinned {@code HumanNPC} ({@code source=human}). Appearance
 * comes from the panel-authored {@link QuestNpcRegistry}, placement comes from Mapper.
 */
@Singleton
@CustomLog
@PluginAdapter("Mapper")
public class QuestNpcContent implements WorldContent {

    private final SceneObjectRegistry registry;
    private final SceneObjectFactoryManager factoryManager;
    private final QuestNpcFactory factory;
    private final QuestGiverService questGiverService;
    private final QuestNpcRegistry questNpcRegistry;

    /** Whether the NPC definitions have been read since the last reload, so they are read once and not per world. */
    private boolean definitionsRead;

    @Inject
    public QuestNpcContent(Core core, SceneObjectRegistry registry, SceneObjectFactoryManager factoryManager,
                           QuestNpcFactory factory, QuestGiverService questGiverService,
                           QuestNpcRegistry questNpcRegistry, WorldContentService contentService) {
        this.registry = registry;
        this.factoryManager = factoryManager;
        this.factory = factory;
        this.questGiverService = questGiverService;
        this.questNpcRegistry = questNpcRegistry;
        factoryManager.addObject("quest", factory);
        contentService.register(core, new WorldContentBinding(WorldSelector.any(), () -> List.of(this))
                .withOnReload(() -> definitionsRead = false));
    }

    @Override
    public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
        if (!definitionsRead) {
            questNpcRegistry.reload();
            definitionsRead = true;
        }

        for (Region region : regions.all()) {
            if (!(region instanceof PointRegion point)) {
                continue;
            }
            final QuestNpcDefinition def = questNpcRegistry.get(region.getName()).orElse(null);
            if (def == null) {
                continue;
            }

            point.setWorld(world);
            final SceneObject obj = spawnNpc(point.getLocation(), def);
            if (obj == null) {
                continue;
            }
            scope.adopt(obj);

            // Bound by identity so "Talk to NPC" objectives (and root-trigger auto-start) can match it. The NPC carries
            // no content of its own.
            questGiverService.bind(obj.getId(), new QuestGiverBinding(def.getId()));
            if (obj instanceof SceneEntity) {
                // A decorator rather than a one-off behaviour, so the nameplate is re-applied every time the NPC
                // re-materializes after a chunk cycle instead of being lost with the despawned display.
                final String displayName = def.getDisplayName();
                final Component role = Component.text("Quest NPC", NamedTextColor.YELLOW);
                obj.addDecorator(o -> TagBehavior.addNameplate((SceneEntity) o, displayName, role));
            }
        }
    }

    private @Nullable SceneObject spawnNpc(@NotNull Location location, @NotNull QuestNpcDefinition def) {
        if (def.isHuman()) {
            final HumanNMS handle = new HumanNMS(def.getDisplayName(), location, def.getSkinValue(), def.getSkinSignature());
            final HumanNPC npc = new HumanNPC(handle, factory); // HumanNPC inits in its constructor
            handle.place(); // position the (world-less) NMS entity; rendering is packet-only
            registry.register(npc);
            return npc;
        }

        final SceneObjectFactory chosen = factoryManager.getObject(def.getFactory() == null ? "" : def.getFactory().toLowerCase()).orElse(null);
        if (chosen == null || def.getType() == null || !Arrays.asList(chosen.getTypes()).contains(def.getType())) {
            log.warn("Quest NPC '{}' references unknown factory/type {}:{}", def.getId(), def.getFactory(), def.getType()).submit();
            return null;
        }
        return chosen.spawnDefault(location, def.getType()); // registers itself
    }
}
