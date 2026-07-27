package me.mykindos.betterpvp.core.item.impl.cannon.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-instance configuration of a single cannon: how tough it is, how it may be handled, and how hard it throws.
 * <p>
 * Everything describing the <em>shot</em> - damage curve, lifetime, impact behaviour - belongs to
 * {@link me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmo} instead, so one cannon can fire several kinds of
 * round. What remains here are the handful of vetoes a cannon may impose on whatever it is loaded with (see the
 * override block below), which exist because those limits are a property of the emplacement, not of the ammunition:
 * a dungeon prop cannon must not hurt its own guards regardless of what someone chambers in it.
 * <p>
 * Traits shared by every cannon of a kind (model, timings, firing mode) live on {@link CannonArchetype}; these
 * properties are the per-instance values, persisted alongside the cannon in {@link CannonStore}.
 */
@Data
@Builder
public class CannonProperties {

    private static final Gson gson = new Gson();

    /** Whether the cannon is invincible (cannot take damage or die) */
    @Builder.Default
    private boolean invincible = false;

    /** Whether to show the health bar above the cannon */
    @Builder.Default
    private boolean showHealthBar = true;

    /** Whether the cannon can be removed or destroyed */
    @Builder.Default
    private boolean removable = true;

    /** Whether players can aim/rotate the cannon */
    @Builder.Default
    private boolean allowRotation = true;

    /** How far should the cannon shoot */
    @Builder.Default
    private double power = 1.3;

    /** The size of the cannon */
    @Builder.Default
    private double size = 1;

    /** Whether the cannon can be moved */
    @Builder.Default
    private boolean movable = true;

    /** Whether shots fired from this cannon may break blocks */
    @Builder.Default
    private boolean breaksBlocks = true;

    /** Whether the cannon is enabled (can be loaded/fired) */
    @Builder.Default
    private boolean enabled = true;

    /** View distance for the health bar display (in blocks) */
    @Builder.Default
    private double healthBarViewDistance = 40;

    /** View distance for the instructions display (in blocks) */
    @Builder.Default
    private double instructionsViewDistance = 5;

    /**
     * Whether the instruction tag carries its state line - the fuse bar, the cooldown timer, {@code READY}. Turn it
     * off on an emplacement whose state is nobody's business, leaving only the "what do I press" lines.
     */
    @Builder.Default
    private boolean showProgressLine = true;

    /**
     * Whether the cannon runs a sequence per operator instead of one for the emplacement.
     * <p>
     * Everything such a cannon emits between the click that starts a sequence and the shot leaving it - fuse flame and
     * crackle, boarding and firing sounds, the countdown, a rider's mannequin sitting in the barrel - goes only to the
     * player it belongs to, so a cannon in use reads as untouched to everyone standing around it. Bystanders pick a
     * ride up at the point it becomes theirs to watch: a mannequin already in the air, without its trail.
     * <p>
     * Because nobody's sequence is written onto the cannon, nobody has to wait for it either: any number of players can
     * be mid-shot in the same emplacement at once, each on their own fuse. There is no shared cooldown between them -
     * a private cannon is not a thing to take turns with. Its floating tags stay in their idle state throughout, since
     * one display cannot show several people different countdowns; the countdown is on each rider's action bar instead.
     */
    @Builder.Default
    private boolean privateOperation = false;

    /** Multiplier for explosion radius */
    @Builder.Default
    private double explosionRadiusMultiplier = 1.0;

    /** Optional custom component to override default instructions */
    @Nullable
    private Component customInstructionsOverride;

    // --- Ammo vetoes: null means "whatever the loaded ammo says". Set only when the emplacement must constrain its
    // rounds regardless of type (a decorative or dungeon cannon that must not harm anything, for instance).

    /** Scales all damage from this cannon's shots. {@code 0} makes it purely cosmetic. */
    @Builder.Default
    private double damageMultiplier = 1.0;

    /** Overrides whether shots detonate on living entities. */
    @Nullable
    private Boolean entityCollisionExplode;

    /** Overrides whether shots detonate on solid blocks. */
    @Nullable
    private Boolean blockCollisionExplode;

    /**
     * Overrides the archetype's fuse length for this one cannon. {@code 0} fires as soon as it is triggered.
     */
    @Nullable
    private Double fuseSecondsOverride;

    /** Default properties for a normal cannon */
    public static CannonProperties normal() {
        return CannonProperties.builder().build();
    }

    /** @return the entity-collision policy for a shot of ammo whose own default is {@code ammoDefault} */
    public boolean resolveEntityCollision(boolean ammoDefault) {
        return entityCollisionExplode == null ? ammoDefault : entityCollisionExplode;
    }

    /** @return the block-collision policy for a shot of ammo whose own default is {@code ammoDefault} */
    public boolean resolveBlockCollision(boolean ammoDefault) {
        return blockCollisionExplode == null ? ammoDefault : blockCollisionExplode;
    }

    /**
     * Serializes this CannonProperties to a JSON string
     */
    public @NotNull String toJson() {
        final JsonObject json = new JsonObject();
        json.addProperty("invincible", invincible);
        json.addProperty("showHealthBar", showHealthBar);
        json.addProperty("removable", removable);
        json.addProperty("allowRotation", allowRotation);
        json.addProperty("power", power);
        json.addProperty("size", size);
        json.addProperty("movable", movable);
        json.addProperty("breaksBlocks", breaksBlocks);
        json.addProperty("enabled", enabled);
        json.addProperty("healthBarViewDistance", healthBarViewDistance);
        json.addProperty("instructionsViewDistance", instructionsViewDistance);
        json.addProperty("showProgressLine", showProgressLine);
        json.addProperty("privateOperation", privateOperation);
        json.addProperty("explosionRadiusMultiplier", explosionRadiusMultiplier);
        json.addProperty("damageMultiplier", damageMultiplier);
        if (entityCollisionExplode != null) {
            json.addProperty("entityCollisionExplode", entityCollisionExplode);
        }
        if (blockCollisionExplode != null) {
            json.addProperty("blockCollisionExplode", blockCollisionExplode);
        }
        if (fuseSecondsOverride != null) {
            json.addProperty("fuseSecondsOverride", fuseSecondsOverride);
        }
        if (customInstructionsOverride != null) {
            json.addProperty("customInstructionsOverride", GsonComponentSerializer.gson().serialize(customInstructionsOverride));
        }
        return gson.toJson(json);
    }

    /**
     * Deserializes a CannonProperties from a JSON string
     */
    public static @NotNull CannonProperties fromJson(@NotNull String json) {
        final JsonObject obj = gson.fromJson(json, JsonObject.class);
        final CannonPropertiesBuilder builder = CannonProperties.builder();

        if (obj.has("invincible")) builder.invincible(obj.get("invincible").getAsBoolean());
        if (obj.has("showHealthBar")) builder.showHealthBar(obj.get("showHealthBar").getAsBoolean());
        if (obj.has("removable")) builder.removable(obj.get("removable").getAsBoolean());
        if (obj.has("allowRotation")) builder.allowRotation(obj.get("allowRotation").getAsBoolean());
        if (obj.has("power")) builder.power(obj.get("power").getAsDouble());
        if (obj.has("size")) builder.size(obj.get("size").getAsDouble());
        if (obj.has("movable")) builder.movable(obj.get("movable").getAsBoolean());
        if (obj.has("breaksBlocks")) builder.breaksBlocks(obj.get("breaksBlocks").getAsBoolean());
        if (obj.has("enabled")) builder.enabled(obj.get("enabled").getAsBoolean());
        if (obj.has("healthBarViewDistance")) builder.healthBarViewDistance(obj.get("healthBarViewDistance").getAsDouble());
        if (obj.has("instructionsViewDistance")) builder.instructionsViewDistance(obj.get("instructionsViewDistance").getAsDouble());
        if (obj.has("showProgressLine")) builder.showProgressLine(obj.get("showProgressLine").getAsBoolean());
        if (obj.has("privateOperation")) builder.privateOperation(obj.get("privateOperation").getAsBoolean());
        if (obj.has("explosionRadiusMultiplier")) builder.explosionRadiusMultiplier(obj.get("explosionRadiusMultiplier").getAsDouble());
        if (obj.has("damageMultiplier")) builder.damageMultiplier(obj.get("damageMultiplier").getAsDouble());
        if (obj.has("entityCollisionExplode")) builder.entityCollisionExplode(obj.get("entityCollisionExplode").getAsBoolean());
        if (obj.has("blockCollisionExplode")) builder.blockCollisionExplode(obj.get("blockCollisionExplode").getAsBoolean());
        if (obj.has("fuseSecondsOverride")) builder.fuseSecondsOverride(obj.get("fuseSecondsOverride").getAsDouble());
        if (obj.has("customInstructionsOverride")) {
            builder.customInstructionsOverride(GsonComponentSerializer.gson().deserialize(obj.get("customInstructionsOverride").getAsString()));
        }

        return builder.build();
    }
}
