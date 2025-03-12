package me.mykindos.betterpvp.game.framework.model.team;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.game.framework.listener.TeamSelectorListener;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.lang.ref.WeakReference;

/**
 * Represents a selector for teams in the waiting lobby.
 */
@Getter
public final class TeamSelector {

    private final TeamProperties teamProperties;
    private boolean spawned = false;
    private Entity entity;

    public TeamSelector(TeamProperties teamProperties) {
        this.teamProperties = teamProperties;
    }

    public Entity spawn(final Location location) {
        Preconditions.checkState(!spawned, "TeamSelector already spawned");
        this.spawned = true;

        this.entity = location.getWorld().spawn(location, Sheep.class, sheep -> {
            sheep.setAI(false);
            Bukkit.getMobGoals().removeAllGoals(sheep);
            sheep.setAggressive(false);
            sheep.setInvulnerable(true);
            sheep.setAware(false);
            sheep.setGravity(false);
            sheep.setSilent(true);
            sheep.setNoPhysics(true);
            sheep.setCanPickupItems(false);
            sheep.lockFreezeTicks(true);
            sheep.setVisualFire(false);
            sheep.setAdult();
            sheep.setPersistent(false);
            sheep.setCustomNameVisible(true);
            sheep.customName(Component.text(teamProperties.name(), teamProperties.color()));

            // Set colored sheep
            sheep.setColor(teamProperties.vanillaColor());
        });

        return entity;
    }
}