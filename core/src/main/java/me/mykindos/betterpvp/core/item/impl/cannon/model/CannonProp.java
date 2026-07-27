package me.mykindos.betterpvp.core.item.impl.cannon.model;

import com.destroystokyo.paper.ParticleBuilder;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmo;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.behavior.TagAnchor;
import me.mykindos.betterpvp.core.scene.behavior.TagBehavior;
import me.mykindos.betterpvp.core.scene.prop.ModeledProp;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cannon in the world.
 * <p>
 * The iron golem is a disposable body that the scene framework destroys and rebuilds across chunk cycles, so all of
 * the cannon's state - loaded round, health, rotation - lives on this object and is persisted by {@link CannonStore}.
 * {@link #onInit()} therefore runs on <b>every</b> materialization and rebuilds the cannon's whole presentation from
 * those fields: model, attributes, health, tags and the {@link CannonCycle} that drives it. The floating displays are
 * {@link TagBehavior}s, so the framework removes them on dematerialization.
 */
@Getter
@CustomLog
public class CannonProp extends ModeledProp implements SoundProvider {

    public static final long COOLDOWN_LERP_OUT = 5_000L;

    private final @NotNull CannonService service;
    private final @NotNull UUID cannonId;
    private final @Nullable UUID placedBy;
    private final @NotNull CannonArchetype archetype;
    private final @NotNull CannonProperties properties;

    /** The chambered round, or {@code null} when empty. Persisted by ammo id. */
    @Setter private @Nullable CannonAmmo ammo;

    /** Authoritative health, mirrored onto the golem each materialization and read back after damage. */
    @Setter private double health;

    private @Nullable CannonCycle cycle;
    private @Nullable TagBehavior healthTag;
    private @Nullable TagBehavior instructionTag;

    /** Where this cannon can fire a rider to. Empty for cannons that do not launch passengers. */
    private final List<CannonDestination> destinations = new ArrayList<>();

    /**
     * Arbitrary durable metadata attached by other modules (clan ownership, event bindings). Persisted with the
     * cannon, since the backing entity's PDC is discarded whenever the body is rebuilt.
     */
    private final Map<String, String> tags = new ConcurrentHashMap<>();

    /**
     * Declared by a scene rather than placed by a player, so it is rebuilt from the map on every load and must never
     * be written to {@link CannonStore} - a stale record would outlive an edit to the scene.
     */
    private boolean transientCannon;

    /**
     * True while the framework is tearing this cannon's body down.
     * <p>
     * {@code SceneObject.dematerialize()} despawns the golem with {@link org.bukkit.entity.Entity#remove()}, whose
     * removal reason is {@code DISCARDED} - which {@code EntityRemovalReason} classes as a destroy, the same bucket as
     * being killed. Without this flag a routine chunk unload is indistinguishable from the cannon being destroyed, and
     * would delete its record.
     */
    private boolean dematerializing;

    public CannonProp(@NotNull SceneObjectFactory factory,
                      @NotNull CannonService service,
                      @NotNull UUID cannonId,
                      @Nullable UUID placedBy,
                      @NotNull CannonArchetype archetype,
                      @NotNull CannonProperties properties,
                      double health) {
        super(factory);
        this.service = service;
        this.cannonId = cannonId;
        this.placedBy = placedBy;
        this.archetype = archetype;
        this.properties = properties;
        this.health = health;
    }

    /** Marks this cannon as scene-owned. See {@link #transientCannon}. */
    public void markTransient() {
        this.transientCannon = true;
    }

    /** Replaces the set of places this cannon can fire a rider to. */
    public void setDestinations(@NotNull Collection<CannonDestination> replacements) {
        destinations.clear();
        destinations.addAll(replacements);
    }

    /** Attaches durable metadata and writes it through to the store. */
    public void setTag(@NotNull String key, @NotNull String value) {
        tags.put(key, value);
        service.persist(this);
    }

    public @Nullable String getTag(@NotNull String key) {
        return tags.get(key);
    }

    @Override
    protected void onInit() {
        super.onInit(); // wraps the golem in ModelEngine

        final ModeledEntity modeled = getModeledEntity();
        if (modeled != null && modeled.getModel(archetype.getModelId()).isEmpty()) {
            final ActiveModel model = ModelEngineAPI.createActiveModel(archetype.getModelId());
            model.setScale(properties.getSize());
            model.setHitboxScale(properties.getSize() + 0.4);
            model.setShadowVisible(true);
            modeled.addModel(model, true);
            modeled.setBaseEntityVisible(false);
            modeled.setModelRotationLocked(false);
            // The backing entity is non-persistent and rebuilt from the store, so letting ModelEngine save the model
            // would resurrect orphaned bodies the cannon record no longer knows about.
            modeled.setSaved(false);
        }

        refreshFromConfig();
        this.dematerializing = false;

        this.cycle = new CannonCycle(this);
        addBehavior(cycle);

        if (properties.isShowHealthBar()) {
            this.healthTag = new TagBehavior(this, tagAnchor(0.5), new Vector(0, 0, 0), display -> {
                styleDisplay(display, properties.getHealthBarViewDistance());
                display.setTextOpacity((byte) 160);
            });
            addBehavior(healthTag);
        }

        if (properties.isEnabled() || properties.getCustomInstructionsOverride() != null) {
            this.instructionTag = new TagBehavior(this, tagAnchor(2.5), new Vector(0, 0, 0),
                    display -> styleDisplay(display, properties.getInstructionsViewDistance()));
            addBehavior(instructionTag);
        }
    }

    @Override
    protected void onDematerialize() {
        // Runs before the body is despawned, so the flag is already set by the time the removal event fires.
        this.dematerializing = true;
        // Captures whatever changed while the body existed - notably rotation, which aiming mutates every tick and so
        // is deliberately not written through on each change.
        service.persist(this);
        this.cycle = null;
        this.healthTag = null;
        this.instructionTag = null;
        super.onDematerialize();
    }

    /** Anchors a tag {@code heightOffset} blocks above the model's hitbox, tracking the body every tick. */
    private TagAnchor tagAnchor(double heightOffset) {
        return () -> {
            if (!isInitialized()) {
                return null;
            }
            final ActiveModel model = getActiveModel();
            final double top = model == null ? getEntity().getHeight() : model.getHitboxScale().y();
            return getEntity().getLocation().add(0, top + heightOffset, 0);
        };
    }

    private static void styleDisplay(@NotNull TextDisplay display, double viewBlocks) {
        display.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
        display.setBrightness(new Display.Brightness(15, 15));
        display.setShadowed(true);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(2f),
                new AxisAngle4f()
        ));
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setBillboard(Display.Billboard.CENTER);
        display.setPersistent(false);
        UtilEntity.setViewRangeBlocks(display, (float) viewBlocks);
    }

    /**
     * Applies the golem's combat and movement traits, including its configured max health. Runs on every
     * materialization, since the framework hands the prop a brand-new entity each time. Must not mark the entity
     * persistent: the body is owned by the scene framework, and a saved golem would be orphaned from its record.
     */
    public void refreshFromConfig() {
        if (!(getEntity() instanceof IronGolem golem)) {
            return;
        }
        golem.setAggressive(false);
        golem.setRemoveWhenFarAway(false);
        golem.customName(Component.text("Cannon"));
        golem.setCustomNameVisible(false);
        golem.setAware(false);
        golem.setVisualFire(TriState.FALSE);
        golem.setCollidable(false);
        golem.setGravity(properties.isMovable());
        Bukkit.getMobGoals().removeAllGoals(golem);

        if (!properties.isMovable()) {
            Objects.requireNonNull(golem.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).setBaseValue(Integer.MAX_VALUE);
            Objects.requireNonNull(golem.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE)).setBaseValue(Integer.MAX_VALUE);
            Objects.requireNonNull(golem.getAttribute(Attribute.GRAVITY)).setBaseValue(0);
        }
        Objects.requireNonNull(golem.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0D);
        Objects.requireNonNull(golem.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(service.getCannonHealth());
        golem.setHealth(Math.min(Math.max(health, 1), service.getCannonHealth()));
    }

    public @Nullable ActiveModel getActiveModel() {
        final ModeledEntity modeled = getModeledEntity();
        return modeled == null ? null : modeled.getModel(archetype.getModelId()).orElse(null);
    }

    /** Where a shot leaves the barrel, or the cannon's own location if the model has no muzzle bone. */
    public @NotNull Location getMuzzle() {
        final ActiveModel model = getActiveModel();
        if (model == null) {
            return getLocation();
        }
        final Optional<ModelBone> bone = model.getBone("tnt_start");
        return bone.map(modelBone -> modelBone.getLocation().clone()).orElseGet(this::getLocation);
    }

    /**
     * How long this cannon's fuse burns, honouring any per-instance override of its archetype's timing.
     * <p>
     * Lives here rather than on {@link CannonCycle} because a privately operated cannon has no single cycle to ask -
     * each rider burns their own fuse off this same number.
     */
    public long fuseMillis() {
        final Double override = properties.getFuseSecondsOverride();
        return (long) ((override == null ? archetype.getFuseSeconds() : override) * 1000L);
    }

    /**
     * Draws one tick of a burning fuse at the model's fuse bone.
     *
     * @param viewer the only player to render it for, or {@code null} to render it for everyone in range
     */
    public void emitFuse(@Nullable Player viewer) {
        final ActiveModel model = getActiveModel();
        if (model == null) {
            return;
        }
        final Optional<ModelBone> bone = model.getBone("fuse");
        if (bone.isEmpty()) {
            return;
        }

        final Location location = bone.get().getLocation();
        final SoundEffect crackle = new SoundEffect("littleroom_cannon", "littleroom.cannon.fuse", 1f, 1.3f);
        final ParticleBuilder flame = Particle.SMALL_FLAME.builder()
                .location(location)
                .count(0) // For directional particles, count must be 0
                .offset(0, 0.15, 0)
                .extra(0.2);

        if (viewer == null) {
            crackle.play(location);
            flame.receivers(60).spawn();
        } else {
            crackle.play(viewer, location);
            flame.receivers(viewer).spawn();
        }
    }

    public @NotNull Location getLocation() {
        return isInitialized() ? getEntity().getLocation() : Objects.requireNonNull(getAnchor()).clone();
    }

    public @NotNull CannonState getCycleState() {
        return cycle == null ? CannonState.IDLE : cycle.getState();
    }

    public boolean isLoaded() {
        return getCycleState().isCharged();
    }

    public void rotate(final @NotNull Vector vector) {
        if (!isInitialized()) {
            return;
        }
        final Location location = getEntity().getLocation();
        location.setDirection(vector);
        getEntity().setRotation(location.getYaw(), location.getPitch());
    }

    /** Reads the live golem's health back onto the prop so it survives the next chunk cycle and a restart. */
    public void syncHealthFromEntity() {
        if (isInitialized() && getEntity() instanceof LivingEntity living) {
            this.health = living.getHealth();
            service.persist(this);
        }
    }

    @Override
    public @Nullable Sound apply(@NotNull DamageEvent event) {
        return SoundProvider.DEFAULT.apply(event);
    }

    @Override
    public boolean fromEntity() {
        return false;
    }

    /**
     * The cannon's floating instructions: an aim hint, the firing mode's own next-input line, and - unless
     * {@link CannonProperties#isShowProgressLine()} says otherwise - a state-dependent progress line.
     */
    public @NotNull TextComponent instructions() {
        if (properties.getCustomInstructionsOverride() != null) {
            return (TextComponent) properties.getCustomInstructionsOverride();
        }

        final TextComponent.Builder component = Component.text();
        if (properties.isAllowRotation() && !getCycleState().isBusy()) {
            component.append(Component.text("Hold ", NamedTextColor.AQUA)
                            .append(Component.text("Right Click", NamedTextColor.WHITE, TextDecoration.BOLD))
                            .append(Component.text(" to ", NamedTextColor.AQUA))
                            .append(Component.text("Aim", NamedTextColor.WHITE, TextDecoration.BOLD)))
                    .appendNewline();
        }

        component.append(archetype.getFiringMode().actionLine(this));
        if (cycle != null && properties.isShowProgressLine()) {
            component.appendNewline().appendNewline();
            component.append(cycle.progressLine());
        }
        return component.build();
    }
}
