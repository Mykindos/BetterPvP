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
 * Configuration properties for a cannon's behavior and interactions.
 * These properties are generic and can be used by any plugin that spawns cannons.
 */
@Data
@Builder
public class CannonProperties {

    private static final Gson gson = new Gson();

    /**
     * Whether the cannon is invincible (cannot take damage or die)
     */
    @Builder.Default
    private boolean invincible = false;

    /**
     * Whether to show the health bar above the cannon
     */
    @Builder.Default
    private boolean showHealthBar = true;

    /**
     * Whether the cannon can be removed or destroyed
     */
    @Builder.Default
    private boolean removable = true;

    /**
     * Whether players can aim/rotate the cannon
     */
    @Builder.Default
    private boolean allowRotation = true;

    /**
     * Whether the cannon uses a fuse timer (false = instant shoot)
     */
    @Builder.Default
    private boolean allowFuse = true;

    /**
     * How far should the cannon shoot
     */
    @Builder.Default
    private double power = 1.3;

    /**
     * The size of the cannon
     */
    @Builder.Default
    private double size = 1;

    /**
     * Whether the cannon can be moved
     */
    @Builder.Default
    private boolean movable = true;

    /**
     * Whether the cannon can break blocks
     */
    @Builder.Default
    private boolean breaksBlocks = true;

    /**
     * Whether the cannon is enabled (can be loaded/fired)
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Whether cannonballs explode on entity collision
     */
    @Builder.Default
    private boolean entityCollisionExplode = true;

    /**
     * Whether cannonballs explode on block collision
     */
    @Builder.Default
    private boolean blockCollisionExplode = true;

    /**
     * How long cannonballs stay alive before exploding (in seconds)
     */
    @Builder.Default
    private double cannonballAliveSeconds = 2.0;

    /**
     * Maximum damage dealt by cannonballs
     */
    @Builder.Default
    private double cannonballDamage = 15.0;

    /**
     * Minimum damage dealt by cannonballs
     */
    @Builder.Default
    private double cannonballMinDamage = 4.0;

    /**
     * Maximum radius for cannonball damage
     */
    @Builder.Default
    private double cannonballDamageMaxRadius = 4.0;

    /**
     * Minimum radius for cannonball damage
     */
    @Builder.Default
    private double cannonballDamageMinRadius = 1.0;

    /**
     * View distance for the health bar display (in blocks)
     */
    @Builder.Default
    private double healthBarViewDistance = 40;

    /**
     * View distance for the instructions display (in blocks)
     */
    @Builder.Default
    private double instructionsViewDistance = 5;

    /**
     * Multiplier for explosion radius
     */
    @Builder.Default
    private double explosionRadiusMultiplier = 1.0;

    /**
     * Optional custom component to override default instructions
     */
    @Nullable
    private Component customInstructionsOverride;

    /**
     * Default properties for a normal cannon
     */
    public static CannonProperties normal() {
        return CannonProperties.builder().build();
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
        json.addProperty("allowFuse", allowFuse);
        json.addProperty("power", power);
        json.addProperty("size", size);
        json.addProperty("movable", movable);
        json.addProperty("breaksBlocks", breaksBlocks);
        json.addProperty("enabled", enabled);
        json.addProperty("entityCollisionExplode", entityCollisionExplode);
        json.addProperty("blockCollisionExplode", blockCollisionExplode);
        json.addProperty("cannonballAliveSeconds", cannonballAliveSeconds);
        json.addProperty("cannonballDamage", cannonballDamage);
        json.addProperty("cannonballMinDamage", cannonballMinDamage);
        json.addProperty("cannonballDamageMaxRadius", cannonballDamageMaxRadius);
        json.addProperty("cannonballDamageMinRadius", cannonballDamageMinRadius);
        json.addProperty("healthBarViewDistance", healthBarViewDistance);
        json.addProperty("instructionsViewDistance", instructionsViewDistance);
        json.addProperty("explosionRadiusMultiplier", explosionRadiusMultiplier);
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
        if (obj.has("allowFuse")) builder.allowFuse(obj.get("allowFuse").getAsBoolean());
        if (obj.has("power")) builder.power(obj.get("power").getAsDouble());
        if (obj.has("size")) builder.size(obj.get("size").getAsDouble());
        if (obj.has("movable")) builder.movable(obj.get("movable").getAsBoolean());
        if (obj.has("breaksBlocks")) builder.breaksBlocks(obj.get("breaksBlocks").getAsBoolean());
        if (obj.has("enabled")) builder.enabled(obj.get("enabled").getAsBoolean());
        if (obj.has("entityCollisionExplode")) builder.entityCollisionExplode(obj.get("entityCollisionExplode").getAsBoolean());
        if (obj.has("blockCollisionExplode")) builder.blockCollisionExplode(obj.get("blockCollisionExplode").getAsBoolean());
        if (obj.has("cannonballAliveSeconds")) builder.cannonballAliveSeconds(obj.get("cannonballAliveSeconds").getAsDouble());
        if (obj.has("cannonballDamage")) builder.cannonballDamage(obj.get("cannonballDamage").getAsDouble());
        if (obj.has("cannonballMinDamage")) builder.cannonballMinDamage(obj.get("cannonballMinDamage").getAsDouble());
        if (obj.has("cannonballDamageMaxRadius")) builder.cannonballDamageMaxRadius(obj.get("cannonballDamageMaxRadius").getAsDouble());
        if (obj.has("cannonballDamageMinRadius")) builder.cannonballDamageMinRadius(obj.get("cannonballDamageMinRadius").getAsDouble());
        if (obj.has("healthBarViewDistance")) builder.healthBarViewDistance(obj.get("healthBarViewDistance").getAsDouble());
        if (obj.has("instructionsViewDistance")) builder.instructionsViewDistance(obj.get("instructionsViewDistance").getAsDouble());
        if (obj.has("explosionRadiusMultiplier")) builder.explosionRadiusMultiplier(obj.get("explosionRadiusMultiplier").getAsDouble());
        if (obj.has("customInstructionsOverride")) {
            builder.customInstructionsOverride(GsonComponentSerializer.gson().deserialize(obj.get("customInstructionsOverride").getAsString()));
        }

        return builder.build();
    }
}
