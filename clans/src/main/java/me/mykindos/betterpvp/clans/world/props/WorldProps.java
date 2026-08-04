package me.mykindos.betterpvp.clans.world.props;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import dev.brauw.mapper.Mapper;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.tag.PatternTag;
import dev.brauw.mapper.tag.RegionScope;
import dev.brauw.mapper.tag.TagRegistry;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.content.WorldSelector;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.ScenePlacement;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.behavior.AmbientParticleBehavior;
import me.mykindos.betterpvp.core.scene.behavior.AmbientSoundBehavior;
import me.mykindos.betterpvp.core.scene.behavior.BobBehavior;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagAnchor;
import me.mykindos.betterpvp.core.scene.behavior.ProximityGate;
import me.mykindos.betterpvp.core.scene.behavior.ScriptEffectBehavior;
import me.mykindos.betterpvp.core.scene.behavior.SpinBehavior;
import me.mykindos.betterpvp.core.scene.behavior.TagBehavior;
import me.mykindos.betterpvp.core.scene.interaction.SceneInteractionRegistry;
import me.mykindos.betterpvp.core.scene.prop.InteractiveProp;
import me.mykindos.betterpvp.core.scene.prop.SimpleProp;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.ModelTint;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The things standing around a place: lanterns, crates, braziers, shrines, and the occasional one you can click.
 * <p>
 * A prop is one {@code prop} marker and its tags. Nothing about it needs a class - the model it wears, the sound it
 * makes, the particles it throws and what right-clicking it does are all authored on the marker - which is what makes
 * dressing a new location a job for the map editor rather than a pull request.
 * <p>
 * Where a prop <em>does</em> need code it stays composed rather than subclassed: a {@code type:} tag names a
 * {@link PropArchetype} for behaviour, and an {@code interact:} tag names a
 * {@link me.mykindos.betterpvp.core.scene.interaction.SceneInteraction} for clicks. Neither replaces the prop; both
 * attach to it, alongside whatever else the tags asked for.
 * <p>
 * Like residents, this content belongs to every world, so props appear wherever their markers are drawn - including in
 * every instance cloned from an island template.
 *
 * <h3>Data point</h3>
 * {@code prop} (perspective - facing matters). Everything is optional:
 * <ul>
 *   <li><b>Identity</b> - {@code id:} names this placement so code that answers its clicks can tell it from its twin
 *       on another island. {@code type:} names a {@link PropArchetype}.</li>
 *   <li><b>Look</b> - {@code model:} the ModelEngine blueprint, {@code skin:} a blueprint remapped over it,
 *       {@code idle:} the animation it rests in, {@code size:} how large it renders, {@code color:} what it is tinted
 *       (whole model and/or per bone — see {@link ModelTint}), {@code hitbox:} how big it is to click. A prop with no
 *       {@code model:} is invisible, which is exactly what a clickable hotspot on a hand-built structure wants to
 *       be.</li>
 *   <li><b>Label</b> - {@code name:} and {@code subtitle:} float above it; {@code bone:} pins them to a model bone so
 *       they follow it through animations.</li>
 *   <li><b>Clicks</b> - {@code interact:} names what opens, {@code interact_animation:} what it plays when clicked.</li>
 *   <li><b>Ambience</b> - {@code sound:} plus {@code sound_interval:}/{@code sound_volume:}/{@code sound_pitch:}, and
 *       {@code particle:} plus {@code particle_interval:}/{@code particle_count:}/{@code particle_spread:}/
 *       {@code particle_height:}. Both stop being computed entirely once nobody is within {@code radius:}.</li>
 *   <li><b>Motion</b> - {@code spin:} degrees per second, {@code bob:} how far it floats and {@code bob_period:} how
 *       long a full rise-and-fall takes.</li>
 * </ul>
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
public class WorldProps implements WorldContent {

    // Package-private: PropValidator checks the same data-point and must agree with this class about its name.
    static final String PROP_POINT = "prop";

    /** Beyond this, ambient sound and particles are not computed at all. */
    private static final double DEFAULT_RADIUS = 32;

    private static final int DEFAULT_SOUND_INTERVAL = 100;
    private static final int DEFAULT_PARTICLE_INTERVAL = 10;
    private static final double DEFAULT_BOB_PERIOD = 4;

    private final SceneObjectFactory objectFactory;
    private final SceneInteractionRegistry sceneInteractions;
    private final PropArchetypeRegistry archetypes;

    private boolean tagsRegistered;

    @Inject
    public WorldProps(@NotNull ClansSceneObjectFactory objectFactory,
                      @NotNull SceneInteractionRegistry sceneInteractions,
                      @NotNull PropArchetypeRegistry archetypes,
                      @NotNull WorldContentService contentService) {
        this.objectFactory = objectFactory;
        this.sceneInteractions = sceneInteractions;
        this.archetypes = archetypes;

        contentService.register(new WorldContentBinding(WorldSelector.any(), () -> List.of(this)));
    }

    @Override
    public @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull RegionIndex regions) {
        registerTags();

        final List<PerspectiveRegion> markers = regions.find(PROP_POINT, PerspectiveRegion.class);
        if (markers.isEmpty()) {
            return List.of();
        }

        log.info("Props in '{}': {} marker(s)", world.getName(), markers.size()).submit();

        final List<SceneSpawn> spawns = new ArrayList<>(markers.size());
        for (PerspectiveRegion marker : markers) {
            spawns.add(prop(marker));
        }
        return spawns;
    }

    /**
     * Assembles one prop. Everything that needs the entity present is done in a decorator, so it is re-applied every
     * time the prop re-materializes after its chunk cycles.
     */
    private SceneSpawn prop(@NotNull PerspectiveRegion marker) {
        final RegionTags tags = RegionTags.of(marker);
        final Location home = marker.getLocation();

        final String modelId = tags.getString("model", "");
        final String skinId = tags.getString("skin", "");
        final String idleAnimation = tags.getString("idle", "");
        final String interaction = tags.getString("interact", "");
        final String interactAnimation = tags.getString("interact_animation", "");
        final double hitbox = tags.getDouble("hitbox", 1.0);
        final double size = tags.getDouble("size", 1.0);
        final ModelTint tint = ModelTint.parse(tags.getString("color", "")).orElse(null);

        // Only a prop that answers clicks becomes an Actor: that also suppresses block placement and item use for
        // anyone looking at it, which would be wrong for scenery you happen to be building next to.
        final SimpleProp prop = interaction.isBlank() ? new SimpleProp(objectFactory) : new InteractiveProp(objectFactory);
        final ScenePlacement placement = new ScenePlacement(prop, tags.getString("id", ""), tags, home);
        final PropArchetype archetype = archetype(tags, home);

        prop.addDecorator(object -> {
            final SimpleProp self = (SimpleProp) object;
            final ActiveModel model = buildModel(self, modelId, skinId, idleAnimation, hitbox, size, tint);

            nameplate(self, model, tags);
            ambience(self, tags);
            motion(self, tags);

            if (archetype != null) {
                archetype.decorate(self, placement);
            }
            if (self instanceof InteractiveProp clickable) {
                clickable.setInteractionHandler(player -> {
                    if (model != null && !interactAnimation.isBlank()) {
                        ModelEngineHelper.playAnimation(model, interactAnimation, 0.2, 0.1, 1.0, false);
                    }
                    sceneInteractions.run(interaction, player, placement);
                });
            }
        });

        return new SceneSpawn(prop, home, objectFactory::backingEntity);
    }

    @Nullable
    private PropArchetype archetype(@NotNull RegionTags tags, @NotNull Location home) {
        final String key = tags.getString("type", "");
        if (key.isBlank()) {
            return null;
        }

        final PropArchetype archetype = archetypes.get(key);
        if (archetype == null) {
            log.warn("Prop at {} declares type '{}', which nothing has registered", home, key).submit();
        }
        return archetype;
    }

    /**
     * Dresses the prop in its model, if it has one. A prop without a {@code model:} keeps the invisible backing entity
     * it was spawned with, which is how a clickable hotspot is placed on a structure that is already built out of
     * blocks.
     *
     * @return the model, or {@code null} if this prop has none
     */
    @Nullable
    private ActiveModel buildModel(@NotNull SimpleProp prop, @NotNull String modelId, @NotNull String skinId,
                                   @NotNull String idleAnimation, double hitbox, double size, @Nullable ModelTint tint) {
        if (modelId.isBlank()) {
            return null;
        }

        final ModeledEntity modeled = prop.getModeledEntity();
        if (modeled == null) {
            log.warn("Prop could not bind a ModeledEntity for model '{}'", modelId).submit();
            return null;
        }

        final ActiveModel model = ModelEngineAPI.createActiveModel(modelId);
        model.setHitboxScale(hitbox);
        if (!idleAnimation.isBlank()) {
            model.getAnimationHandler().setDefaultProperty(
                    new AnimationHandler.DefaultProperty(ModelState.IDLE, idleAnimation, 0, 0, 1));
        }
        modeled.addModel(model, true);
        if (!skinId.isBlank()) {
            ModelEngineHelper.remapModel(model, ModelEngineAPI.getBlueprint(skinId));
        }
        // After the remap: a re-pointed bone renders from its own tint, so colouring first would leave a skinned prop
        // untinted.
        if (tint != null) {
            final Set<String> unknown = tint.apply(model);
            if (!unknown.isEmpty()) {
                log.warn("Prop model '{}' has no renderer bone called {} - that part keeps its own colour",
                        modelId, String.join(", ", unknown)).submit();
            }
        }

        // Particles and sounds the animations themselves ask for, so a forge can spark on the hammer-fall frame.
        prop.addBehavior(new ScriptEffectBehavior(prop, model));
        return model;
    }

    private void nameplate(@NotNull SimpleProp prop, @Nullable ActiveModel model, @NotNull RegionTags tags) {
        final String name = tags.getString("name", "");
        if (name.isBlank()) {
            return;
        }

        final Component subtitle = Component.text(tags.getString("subtitle", ""), NamedTextColor.GRAY);
        final String bone = tags.getString("bone", "");
        if (model != null && !bone.isBlank()) {
            BoneTagAnchor.addNameplate(prop, model, bone, name, subtitle);
        } else {
            TagBehavior.addNameplate(prop, name, subtitle);
        }
    }

    /**
     * Attaches the ambient sound and particle loops the marker asked for, each gated on somebody being close enough to
     * notice. The gate is what makes props affordable at scale: a harbour's worth of them, multiplied by every live
     * instance of that harbour, otherwise ticks continuously for empty rooms.
     */
    private void ambience(@NotNull SimpleProp prop, @NotNull RegionTags tags) {
        final double radius = tags.getDouble("radius", DEFAULT_RADIUS);

        final String sound = tags.getString("sound", "");
        if (!sound.isBlank()) {
            final AmbientSoundBehavior ambient = new AmbientSoundBehavior(prop, soundEffect(sound, tags),
                    tags.getInt("sound_interval", DEFAULT_SOUND_INTERVAL));
            prop.addBehavior(new ProximityGate(prop, radius, ambient));
        }

        final Particle particle = particle(tags.getString("particle", ""));
        if (particle != null) {
            final AmbientParticleBehavior ambient = new AmbientParticleBehavior(prop, particle,
                    tags.getInt("particle_count", 3),
                    tags.getDouble("particle_spread", 0.2),
                    new Vector(0, tags.getDouble("particle_height", 1.0), 0),
                    tags.getInt("particle_interval", DEFAULT_PARTICLE_INTERVAL));
            prop.addBehavior(new ProximityGate(prop, radius, ambient));
        }
    }

    private void motion(@NotNull SimpleProp prop, @NotNull RegionTags tags) {
        final double spin = tags.getDouble("spin", 0);
        if (spin != 0) {
            prop.addBehavior(new SpinBehavior(prop, spin));
        }

        final double bob = tags.getDouble("bob", 0);
        if (bob > 0) {
            prop.addBehavior(new BobBehavior(prop, bob, tags.getDouble("bob_period", DEFAULT_BOB_PERIOD)));
        }
    }

    /**
     * Reads a {@code sound:} tag. Written {@code namespace:key}, or bare for a vanilla sound - so
     * {@code sound:block.bell.use} and {@code sound:betterpvp:ship_creak} both work. Resolved by key rather than by
     * enum so resource-pack sounds are authored exactly like vanilla ones.
     */
    private SoundEffect soundEffect(@NotNull String sound, @NotNull RegionTags tags) {
        final float pitch = (float) tags.getDouble("sound_pitch", 1.0);
        final float volume = (float) tags.getDouble("sound_volume", 1.0);

        final int separator = sound.indexOf(':');
        if (separator > 0 && separator < sound.length() - 1) {
            return new SoundEffect(sound.substring(0, separator), sound.substring(separator + 1), pitch, volume);
        }
        return new SoundEffect("minecraft", sound, pitch, volume);
    }

    @Nullable
    private Particle particle(@NotNull String name) {
        if (name.isBlank()) {
            return null;
        }

        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            log.warn("Prop declares particle '{}', which is not a particle", name).submit();
            return null;
        }
    }

    /**
     * Tells Mapper's in-world editor which tags a prop takes.
     * <p>
     * Done once, on the first world that asks. Not in the constructor, because that now runs during plugin enable when
     * Mapper may not be ready to take them; not per load either, since the definitions are identical for every world
     * and this content is asked about all of them.
     */
    private void registerTags() {
        if (tagsRegistered) {
            return;
        }
        tagsRegistered = true;

        try {
            registerTagDefinitions();
            Mapper.get().getValidationRegistry().register(new PropValidator(archetypes));
        } catch (Throwable throwable) {
            // Editor convenience only - it must never stop props from spawning. Throwable, not Exception: an older
            // Mapper surfaces here as NoClassDefFoundError/NoSuchMethodError.
            log.warn("Could not register prop tags - check the Mapper plugin version", throwable).submit();
        }
    }

    private void registerTagDefinitions() {
        final RegionScope prop = RegionScope.names(PROP_POINT);
        final TagRegistry tags = Mapper.get().getTagRegistry();

        // One entry of a colour tag: a colour, optionally prefixed by the bone it applies to.
        final String colorEntry = "(?:[A-Za-z0-9_]+=)?(?:#?[0-9A-Fa-f]{6}|[a-z_]+)";

        tags.register(
                new PatternTag("id", "id:.+", "id:<text>", "Identifies this prop to whatever answers its clicks", true, prop),
                new PatternTag("type", "type:.+", "type:<archetype>", "Code behind this prop, if it needs any", true, prop),

                new PatternTag("model", "model:.+", "model:<blueprint>", "ModelEngine model; omit for an invisible prop", true, prop),
                new PatternTag("skin", "skin:.+", "skin:<blueprint>", "Blueprint remapped over the model", true, prop),
                new PatternTag("idle", "idle:.+", "idle:<animation>", "Animation played while resting", true, prop),
                new PatternTag("size", "size:[0-9.]+", "size:<number>", "How large the model is rendered", true, prop),
                new PatternTag("color", "color:" + colorEntry + "(?:," + colorEntry + ")*",
                        "color:<colour>[,<bone>=<colour>...]",
                        "Tints the model - hex or a colour name, whole-model and/or per bone", true, prop),
                new PatternTag("hitbox", "hitbox:[0-9.]+", "hitbox:<number>", "How big the prop is to click", true, prop),

                new PatternTag("name", "name:.+", "name:<text>", "Label floating above the prop", true, prop),
                new PatternTag("subtitle", "subtitle:.+", "subtitle:<text>", "Smaller line above the label", true, prop),
                new PatternTag("bone", "bone:.+", "bone:<bone>", "Model bone the label follows", true, prop),

                new PatternTag("interact", "interact:.+", "interact:<action>", "What right-clicking this prop opens", true, prop),
                new PatternTag("interact_animation", "interact_animation:.+", "interact_animation:<animation>",
                        "Animation played when clicked", true, prop),

                new PatternTag("radius", "radius:[0-9.]+", "radius:<blocks>",
                        "How close a player must be for ambient effects to run", true, prop),
                new PatternTag("sound", "sound:.+", "sound:<[namespace:]key>", "Ambient sound, replayed on a beat", true, prop),
                new PatternTag("sound_interval", "sound_interval:\\d+", "sound_interval:<ticks>", "Ticks between plays", true, prop),
                new PatternTag("sound_volume", "sound_volume:[0-9.]+", "sound_volume:<number>", "Ambient sound volume", true, prop),
                new PatternTag("sound_pitch", "sound_pitch:[0-9.]+", "sound_pitch:<number>", "Ambient sound pitch", true, prop),

                new PatternTag("particle", "particle:.+", "particle:<particle>", "Ambient particle, emitted on a beat", true, prop),
                new PatternTag("particle_interval", "particle_interval:\\d+", "particle_interval:<ticks>", "Ticks between puffs", true, prop),
                new PatternTag("particle_count", "particle_count:\\d+", "particle_count:<number>", "Particles per puff", true, prop),
                new PatternTag("particle_spread", "particle_spread:[0-9.]+", "particle_spread:<blocks>", "How far they scatter", true, prop),
                new PatternTag("particle_height", "particle_height:[0-9.]+", "particle_height:<blocks>", "Height they come from", true, prop),

                new PatternTag("spin", "spin:-?[0-9.]+", "spin:<degrees per second>", "Turns the prop on the spot", true, prop),
                new PatternTag("bob", "bob:[0-9.]+", "bob:<blocks>", "How far the prop floats up and down", true, prop),
                new PatternTag("bob_period", "bob_period:[0-9.]+", "bob_period:<seconds>", "How long one full bob takes", true, prop));
    }
}
