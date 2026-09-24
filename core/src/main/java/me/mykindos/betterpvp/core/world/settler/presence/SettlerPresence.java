package me.mykindos.betterpvp.core.world.settler.presence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.behavior.AmbientParticleBehavior;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagAnchor;
import me.mykindos.betterpvp.core.scene.behavior.ScriptEffectBehavior;
import me.mykindos.betterpvp.core.scene.behavior.TagBehavior;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.world.content.SceneSpawn;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAssignedEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerJoinedEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerLeftEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Puts every settler of a loaded site in its world and keeps them current: a body appears when a settler joins and
 * goes when it leaves, and heads off to its new workplace when its assignment changes.
 * <p>
 * The module that owns a kind of site binds {@link #content()} to that site's worlds.
 */
@BPvPListener
@Singleton
@CustomLog
public class SettlerPresence implements Listener {

    private final SettlerService service;
    private final ProfessionRegistry professions;
    private final SiteInstances instances;
    private final SettlerFactory factory;
    private final Map<String, Loaded> worlds = new HashMap<>();
    private final Set<String> missingModels = new HashSet<>();

    @Inject
    public SettlerPresence(@NotNull SettlerService service, @NotNull ProfessionRegistry professions,
                           @NotNull SiteInstances instances, @NotNull SettlerFactory factory) {
        this.service = service;
        this.professions = professions;
        this.instances = instances;
        this.factory = factory;
    }

    /** Content showing the settlers of whatever site a world belongs to. */
    public @NotNull WorldContent content() {
        return new WorldContent() {
            @Override
            public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
                instances.byWorld(world.getName()).map(SiteInstance::getKey).ifPresent(key -> {
                    final Loaded loaded = new Loaded(key, regions, scope);
                    worlds.put(world.getName(), loaded);
                    scope.onRelease(() -> worlds.remove(world.getName(), loaded));
                    sync(world, loaded);
                });
            }
        };
    }

    /** Catches rosters that were read after their world opened, and anything that changed without an event. */
    @UpdateEvent(delay = 5000)
    public void tick() {
        for (Map.Entry<String, Loaded> entry : new ArrayList<>(worlds.entrySet())) {
            final World world = Bukkit.getWorld(entry.getKey());
            if (world != null) {
                sync(world, entry.getValue());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoined(@NotNull SettlerJoinedEvent event) {
        forSite(event.getSite(), this::sync);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeft(@NotNull SettlerLeftEvent event) {
        forSite(event.getSite(), (world, loaded) -> {
            final SettlerNPC body = loaded.bodies.remove(event.getSettler().getId());
            if (body != null) {
                body.remove();
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAssigned(@NotNull SettlerAssignedEvent event) {
        forSite(event.getSite(), (world, loaded) -> {
            final SettlerNPC body = loaded.bodies.get(event.getSettler().getId());
            if (body != null) {
                body.replan();
            }
        });
    }

    /** Sends every settler of {@code site} standing in {@code world} to gather near {@code spot} for a moment. */
    public void gather(@NotNull SiteKey site, @NotNull World world, @NotNull Location spot) {
        final Loaded loaded = worlds.get(world.getName());
        if (loaded != null && loaded.key.equals(site)) {
            loaded.bodies.values().forEach(body -> body.gather(spot));
        }
    }

    private void sync(@NotNull World world, @NotNull Loaded loaded) {
        final SettlerSite site = service.site(loaded.key).orElse(null);
        final Roster roster = service.roster(loaded.key).orElse(null);
        if (site == null || roster == null || loaded.scope.isReleased()) {
            return;
        }

        final Set<UUID> present = new HashSet<>();
        for (Settler settler : roster.getSettlers()) {
            present.add(settler.getId());
            if (!loaded.bodies.containsKey(settler.getId())) {
                loaded.bodies.put(settler.getId(), spawn(world, loaded, site, settler));
            }
        }
        loaded.bodies.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().remove();
            return true;
        });
    }

    private @NotNull SettlerNPC spawn(@NotNull World world, @NotNull Loaded loaded, @NotNull SettlerSite site,
                                      @NotNull Settler settler) {
        final Location home = site.home(loaded.key, world, loaded.regions).orElseGet(world::getSpawnLocation);
        final SettlerNPC npc = new SettlerNPC(factory, settler.getId());
        npc.addDecorator(object -> dress(npc, world, loaded, site, home));
        npc.setInteractionHandler(player -> current(loaded.key, npc.getSettlerId()).ifPresent(found -> {
            npc.talkTo(player);
            site.interact(player, loaded.key, found);
        }));
        loaded.scope.add(new SceneSpawn(npc, home, factory::backingEntity));
        return npc;
    }

    /** Everything that needs the entity, so it is put back each time the settler materializes. */
    private void dress(@NotNull SettlerNPC npc, @NotNull World world, @NotNull Loaded loaded, @NotNull SettlerSite site,
                       @NotNull Location home) {
        final Settler settler = current(loaded.key, npc.getSettlerId()).orElse(null);
        if (settler == null) {
            return;
        }
        final SettlerLook look = site.look(loaded.key, settler);
        final Component name = Component.text(settler.getName(), settler.getRarity().getColor());
        final Component role = role(settler);

        final ActiveModel model = model(npc, look);
        if (model != null) {
            BoneTagAnchor.addNameplate(npc, model, "head", name, role);
            npc.addBehavior(new ScriptEffectBehavior(npc, model));
        } else {
            TagBehavior.addNameplate(npc, name, role);
        }
        if (settler.getRarity() == SettlerRarity.LEGENDARY) {
            npc.addBehavior(new AmbientParticleBehavior(npc, Particle.END_ROD, 1, 0.3, new Vector(0, 2.2, 0), 40));
        }

        final SettlerRoutine routine = new SettlerRoutine(npc, look, home, () -> current(loaded.key, npc.getSettlerId())
                .filter(found -> found.getState() == SettlerState.WORKING && found.getAssignment() != null)
                .flatMap(found -> site.workplace(loaded.key, world, loaded.regions, found.getAssignment())));
        npc.setRoutine(routine);
        npc.addBehavior(routine);
    }

    /** Puts the look's model on the body, or nothing when its model is not installed yet. */
    private @Nullable ActiveModel model(@NotNull SettlerNPC npc, @NotNull SettlerLook look) {
        final ModeledEntity modeled = npc.getModeledEntity();
        if (modeled == null) {
            return null;
        }
        if (ModelEngineAPI.getBlueprint(look.getModel()) == null) {
            if (missingModels.add(look.getModel())) {
                log.warn("Settler model '{}' is not installed, so settlers using it show no model", look.getModel()).submit();
            }
            return null;
        }

        final ActiveModel model = ModelEngineAPI.createActiveModel(look.getModel());
        model.setScale(look.getSize());
        model.setHitboxScale(1.5);
        model.getAnimationHandler().setDefaultProperty(
                new AnimationHandler.DefaultProperty(ModelState.IDLE, look.getIdleAnimation(), 0, 0, 1));
        modeled.addModel(model, true);
        if (look.getSkin() != null && ModelEngineAPI.getBlueprint(look.getSkin()) != null) {
            ModelEngineHelper.remapModel(model, ModelEngineAPI.getBlueprint(look.getSkin()));
        }
        return model;
    }

    private @NotNull Component role(@NotNull Settler settler) {
        if (settler.getProfession() == null) {
            return Component.empty();
        }
        return professions.find(settler.getProfession())
                .map(profession -> Translations.component(profession.getKey()).color(NamedTextColor.YELLOW))
                .orElseGet(Component::empty);
    }

    private @NotNull Optional<Settler> current(@NotNull SiteKey key, @NotNull UUID settlerId) {
        return service.roster(key).flatMap(roster -> roster.find(settlerId));
    }

    private void forSite(@NotNull SiteKey site, @NotNull WorldAction action) {
        for (SiteInstance instance : instances.forKey(site)) {
            final Loaded loaded = worlds.get(instance.getWorldName());
            final World world = Bukkit.getWorld(instance.getWorldName());
            if (loaded != null && world != null) {
                action.run(world, loaded);
            }
        }
    }

    private interface WorldAction {
        void run(@NotNull World world, @NotNull Loaded loaded);
    }

    private static final class Loaded {

        private final SiteKey key;
        private final RegionIndex regions;
        private final WorldContentScope scope;
        private final Map<UUID, SettlerNPC> bodies = new HashMap<>();

        private Loaded(@NotNull SiteKey key, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
            this.key = key;
            this.regions = regions;
            this.scope = scope;
        }
    }
}
