package me.mykindos.betterpvp.core.scene.npc;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * NMS Player instance for NPCs. Optionally carries a skin via the profile's
 * {@code textures} property (value + signature), which the client renders.
 * <p>
 * This entity is never added to the world; the NPC is presented to clients entirely through
 * packets (see {@link HumanNPC#show()}). Constructing it only allocates the backing handle and
 * reserves an entity id — call {@link #place()} to position it.
 */
@Getter
public class HumanNMS extends Player {

    private final Location location;
    @Nullable private final String skinValue;
    @Nullable private final String skinSignature;

    public HumanNMS(String name, Location location) {
        this(name, location, null, null);
    }

    public HumanNMS(String name, Location location, @Nullable String skinValue, @Nullable String skinSignature) {
        super(((CraftWorld) location.getWorld()).getHandle(), profile(name, skinValue, skinSignature));
        this.location = location;
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
    }

    /**
     * Positions the NMS entity at its target location <b>without</b> adding it to the world.
     * The NPC is rendered entirely through packets (see {@link HumanNPC#show()}); keeping it out
     * of the world avoids the {@code CraftHumanEntity}/{@code CraftPlayer} cast crashes that
     * plugins iterating live entities (e.g. Nexo) hit on a non-{@code ServerPlayer} player.
     * Positioning still matters so {@code getBukkitEntity().getLocation()} reports the real spot.
     */
    public void place() {
        setCustomNameVisible(false);
        setPos(location.getX(), location.getY(), location.getZ());
        setRot(location.getYaw(), location.getPitch());
        setYHeadRot(location.getYaw());
    }

    private static GameProfile profile(String name, @Nullable String skinValue, @Nullable String skinSignature) {
        final Multimap<String, Property> textures = LinkedHashMultimap.create();
        if (skinValue != null && !skinValue.isBlank()) {
            textures.put("textures", new Property("textures", skinValue, skinSignature));
        }
        return new GameProfile(UUID.randomUUID(), name, new PropertyMap(textures));
    }

    @Override
    public @Nullable GameType gameMode() {
        return GameType.DEFAULT_MODE;
    }
}
