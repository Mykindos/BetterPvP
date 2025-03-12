package me.mykindos.betterpvp.champions.champions.npc;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a selector for {@link Role}. This can be clicked by the player to equip a role, and given {@code isEditor} will allow the player to edit their builds.
 */
@Getter
public final class KitSelector {

    private final Role role;
    private final boolean isEditor;
    private boolean spawned = false;

    public KitSelector(Role role, boolean isEditor) {
        this.role = role;
        this.isEditor = isEditor;
    }

    public void spawn(final Location location) {
        Preconditions.checkState(!spawned, "KitSelector already spawned");
        this.spawned = true;

        final Entity entity = location.getWorld().spawn(location, Zombie.class, zombie -> {
            zombie.setAI(false);
            Bukkit.getMobGoals().removeAllGoals(zombie);
            zombie.setShouldBurnInDay(false);
            zombie.setAggressive(false);
            zombie.setInvulnerable(true);
            zombie.setAware(false);
            zombie.setGravity(false);
            zombie.setSilent(true);
            zombie.setConversionTime(-Integer.MAX_VALUE);
            zombie.setNoPhysics(true);
            zombie.setCanPickupItems(false);
            zombie.setCanBreakDoors(false);
            zombie.lockFreezeTicks(true);
            zombie.setVisualFire(false);
            zombie.setAdult();
            zombie.setPersistent(false);
            zombie.setCustomNameVisible(true);
            zombie.customName(Component.text(role.getName(), role.getColor()));

            zombie.getEquipment().setHelmet(new ItemStack(role.getHelmet()));
            zombie.getEquipment().setChestplate(new ItemStack(role.getChestplate()));
            zombie.getEquipment().setLeggings(new ItemStack(role.getLeggings()));
            zombie.getEquipment().setBoots(new ItemStack(role.getBoots()));
        });

        // register
        JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(KitSelectorListener.class).selectors.put(entity, this);
    }

}