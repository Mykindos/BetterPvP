package me.mykindos.betterpvp.core.npc.model;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * NMS Player instance for NPCs.
 */
public class HumanNMS extends Player {

    protected HumanNMS(String name, Location location) {
        super(((CraftWorld) location.getWorld()).getHandle(), new GameProfile(UUID.randomUUID(), name));
    }

    @Override
    public @Nullable GameType gameMode() {
        return GameType.DEFAULT_MODE;
    }
}
