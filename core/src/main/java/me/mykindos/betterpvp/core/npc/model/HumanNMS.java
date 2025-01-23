package me.mykindos.betterpvp.core.npc.model;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;

import java.util.UUID;

/**
 * NMS Player instance for NPCs.
 */
public class HumanNMS extends Player {

    protected HumanNMS(String name, Location location) {
        super(((CraftWorld) location.getWorld()).getHandle(),
                BlockPos.containing(location.getX(), location.getY(), location.getZ()),
                0f,
                new GameProfile(UUID.randomUUID(), name));
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
