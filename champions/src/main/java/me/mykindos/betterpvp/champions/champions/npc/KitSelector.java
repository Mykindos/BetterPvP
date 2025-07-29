package me.mykindos.betterpvp.champions.champions.npc;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.core.components.champions.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a selector for {@link Role}. This can be clicked by the player to equip a role, and given {@code isEditor} will allow the player to edit their builds.
 */
@Getter
public final class KitSelector {

    private final Role role;
    @Setter
    private Function<Player, BuildMenu> buildMenuFunction = null;
    private final boolean isEquip;
    private final boolean isEditor;
    private boolean spawned = false;
    private LivingEntity entity;
    private Entity nametag;

    public KitSelector(Role role, boolean isEquip, boolean isEditor) {
        Preconditions.checkArgument(isEquip || isEditor, "Kit selector must be at least an editor and an equipper, or both!");
        this.role = role;
        this.isEquip = isEquip;
        this.isEditor = isEditor;
    }

    public void spawn(final Location location) {
        Preconditions.checkState(!spawned, "KitSelector already spawned");
        this.spawned = true;

        this.entity = location.getWorld().spawn(location, Zombie.class, zombie -> {
            zombie.setAI(false);
            Bukkit.getMobGoals().removeAllGoals(zombie);
            zombie.setShouldBurnInDay(false);
            zombie.setAggressive(false);
            zombie.setInvulnerable(true);
            zombie.setAware(false);
            zombie.setGravity(false);
            zombie.setSilent(true);
            zombie.setConversionTime(-Integer.MAX_VALUE);
            zombie.setRemoveWhenFarAway(false);
            zombie.setNoPhysics(true);
            zombie.setCanPickupItems(false);
            zombie.setCanBreakDoors(false);
            zombie.lockFreezeTicks(true);
            zombie.setVisualFire(false);
            zombie.setAdult();
            zombie.setPersistent(false);

            zombie.getEquipment().setHelmet(new ItemStack(role.getHelmet()));
            zombie.getEquipment().setChestplate(new ItemStack(role.getChestplate()));
            zombie.getEquipment().setLeggings(new ItemStack(role.getLeggings()));
            zombie.getEquipment().setBoots(new ItemStack(role.getBoots()));
            zombie.getEquipment().setItemInMainHand(null);
            zombie.getEquipment().setItemInOffHand(null);

            final Entity vehicle = zombie.getVehicle();
            if (vehicle != null && vehicle.isValid()) {
                zombie.leaveVehicle();
                vehicle.remove();
            }
        });

        this.nametag = location.getWorld().spawn(entity.getEyeLocation().add(0, 0.7, 0), TextDisplay.class, spawned -> {
            spawned.text(Component.text(role.getName(), role.getColor(), TextDecoration.BOLD));
            spawned.setPersistent(false);
            spawned.setShadowed(false);
            spawned.setSeeThrough(false);
            spawned.setBillboard(Display.Billboard.VERTICAL);
            spawned.setBackgroundColor(Color.fromARGB(0x0));
        });

        // register
        JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(KitSelectorListener.class).selectors.put(entity, this);
    }

    public void remove() {
        if (entity != null && entity.isValid()) {
            JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(KitSelectorListener.class).selectors.remove(entity);
            entity.remove();
            entity = null;
        }
        if (nametag != null && nametag.isValid()) {
            nametag.remove();
            nametag = null;
        }
    }

}