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
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.MapperSceneLoader;
import me.mykindos.betterpvp.core.scene.loader.ModuleReloadLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.scene.loader.ServerStartLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.WorldLoadStrategy;
import me.mykindos.betterpvp.core.scene.npc.HumanNMS;
import me.mykindos.betterpvp.core.scene.npc.HumanNPC;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Spawns quest-giver NPCs from Mapper data-points across <b>all</b> worlds. A
 * {@link PointRegion} (or its subclass {@code PerspectiveRegion}) named with a
 * quest NPC id is spawned as that NPC: either via a registered factory + type
 * ({@code source=factory}, like {@code /npc spawn}) or as a skinned
 * {@code HumanNPC} ({@code source=human}). Appearance comes from the
 * panel-authored {@link QuestNpcRegistry}; placement comes from Mapper.
 */
@PluginAdapter("Core")
@Singleton
@CustomLog
public class QuestNpcSceneLoader extends MapperSceneLoader {

    private final SceneObjectRegistry registry;
    private final SceneObjectFactoryManager factoryManager;
    private final QuestNpcFactory factory;
    private final QuestGiverService questGiverService;
    private final QuestNpcRegistry questNpcRegistry;

    @Inject
    public QuestNpcSceneLoader(Core core, SceneObjectRegistry registry, SceneObjectFactoryManager factoryManager,
                               QuestNpcFactory factory, QuestGiverService questGiverService, QuestNpcRegistry questNpcRegistry,
                               SceneLoaderManager sceneLoaderManager) {
        this.registry = registry;
        this.factoryManager = factoryManager;
        this.factory = factory;
        this.questGiverService = questGiverService;
        this.questNpcRegistry = questNpcRegistry;
        sceneLoaderManager.register(this, core);
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        // World-agnostic: re-scan whenever any world loads, so quest NPCs in on-demand worlds appear once that world
        // is available rather than being missed by the one-shot server-start pass.
        return List.of(new ServerStartLoadStrategy(), new WorldLoadStrategy(), new ModuleReloadLoadStrategy());
    }

    /** All Mapper data-points across every loaded world (world-agnostic). */
    @Override
    protected Collection<Region> getRegions() {
        final List<Region> all = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            try {
                for (Region region : MapperHelper.getRegions(world)) {
                    if (region instanceof PointRegion point) {
                        point.setWorld(world);
                    }
                    all.add(region);
                }
            } catch (Exception ex) {
                // World has no Mapper data-points (or Mapper absent) — skip quietly.
            }
        }
        return all;
    }

    @Override
    protected void load() {
        factoryManager.removeObject("quest");
        factoryManager.addObject("quest", factory);
        questNpcRegistry.reload();

        int spawned = 0;
        for (Region region : getRegions()) {
            if (!(region instanceof PointRegion point)) continue;
            final QuestNpcDefinition def = questNpcRegistry.get(region.getName()).orElse(null);
            if (def == null) continue; // a data-point with no matching NPC definition

            final SceneObject obj = spawnNpc(point.getLocation(), def);
            if (obj == null) continue;

            // Bind every quest NPC by identity so "Talk to NPC" objectives (and root-trigger
            // auto-start) can match it. The NPC carries no content of its own.
            questGiverService.bind(obj.getId(), new QuestGiverBinding(def.getId()));
            if (obj instanceof SceneEntity) {
                // Add as a decorator (not a one-off behaviour) so the nameplate is re-applied every time the NPC
                // re-materializes after a chunk cycle, instead of being lost with the despawned display.
                final String displayName = def.getDisplayName();
                final Component role = Component.text("Quest NPC", NamedTextColor.YELLOW);
                obj.addDecorator(o -> TagBehavior.addNameplate((SceneEntity) o, displayName, role));
            }
            spawned++;
        }
        log.info("Spawned {} quest-giver NPC(s) from Mapper", spawned).submit();
    }

    /** @return the spawned scene object, or {@code null} if it could not be spawned. */
    private SceneObject spawnNpc(Location location, QuestNpcDefinition def) {
        if (def.isHuman()) {
            final HumanNMS handle = new HumanNMS(def.getDisplayName(), location, def.getSkinValue(), def.getSkinSignature());
            final HumanNPC npc = new HumanNPC(handle, factory); // HumanNPC inits in its constructor
            handle.place(); // position the (world-less) NMS entity; rendering is packet-only
            registry.register(npc);
            track(npc);
            return npc;
        }

        final SceneObjectFactory chosen = factoryManager.getObject(def.getFactory() == null ? "" : def.getFactory().toLowerCase()).orElse(null);
        if (chosen == null || def.getType() == null || !Arrays.asList(chosen.getTypes()).contains(def.getType())) {
            log.warn("Quest NPC '{}' references unknown factory/type {}:{}", def.getId(), def.getFactory(), def.getType()).submit();
            return null;
        }
        final SceneObject obj = chosen.spawnDefault(location, def.getType()); // registers itself
        track(obj);
        return obj;
    }
}
