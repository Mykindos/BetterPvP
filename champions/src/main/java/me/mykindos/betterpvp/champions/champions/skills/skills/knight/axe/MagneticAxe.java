package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.AxeProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Singleton
@BPvPListener
public class MagneticAxe extends Skill implements InteractSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private final Map<Player, List<AxeProjectile>> data = new HashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double duration;
    private double hitboxSize;
    private double speed;

    @Inject
    public MagneticAxe(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Magnetic Axe";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Throw your axe, dealing " + getValueString(this::getDamage, level) + " damage",
                "",
                "After colliding with anything, it",
                "will be magnetized back to you",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getSpeed() {
        return speed;
    }

    private double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    private double getDuration(int level) {
        return duration;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d) * cooldownDecreasePerLevel;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    @Override
    public void activate(Player player, int level) {
        if (!isHolding(player)) return;

        ItemStack axeItem = player.getInventory().getItemInMainHand();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 1.0F);
        player.getInventory().setItemInMainHand(null);

        Vector perpendicularAxis = player.getLocation().getDirection().crossProduct(new Vector(0, 1, 0)).normalize();
        Location rightHandPosition = player.getLocation().add(0, 1, 0).add(perpendicularAxis.multiply(0.3));

        AxeProjectile projectile = new AxeProjectile(
                player,
                hitboxSize,
                rightHandPosition,
                (long) (getDuration(level) * 1000L),
                axeItem,
                getDamage(level),
                getSpeed(),
                this);

        Vector direction = player.getLocation().getDirection().normalize();
        direction.add(new Vector(0, Math.sin(Math.toRadians(8)), 0));
        direction.normalize().multiply(speed);
        projectile.redirect(direction);

        data.computeIfAbsent(player, key -> new ArrayList<>()).add(projectile);
    }

    @UpdateEvent
    public void checkCollide() {
        Iterator<Map.Entry<Player, List<AxeProjectile>>> iterator = data.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Player, List<AxeProjectile>> entry = iterator.next();
            Player player = entry.getKey();
            List<AxeProjectile> list = entry.getValue();

            // Despawn if player invalid
            if (player == null || !player.isOnline() || !player.isValid() || list.isEmpty()) {
                iterator.remove();
                list.forEach(AxeProjectile::remove);
                continue;
            }

            Iterator<AxeProjectile> dataIterator = list.iterator();
            while (dataIterator.hasNext()) {
                AxeProjectile projectile = dataIterator.next();

                if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                    projectile.remove();
                    dataIterator.remove();
                    continue;
                }

                // Return to player if hit
                if (projectile.isImpacted() && projectile.getLocation().distanceSquared(player.getEyeLocation()) < 1.0) {
                    dataIterator.remove();
                    projectile.remove();
                    projectile.setMarkForRemoval(true);
                    returnAxeToPlayer(player, projectile);
                    continue;
                }

                projectile.tick();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        returnAllAxesToPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();

        List<AxeProjectile> axeList = data.remove(player);
        if (axeList != null) {
            for (AxeProjectile axeProjectile : axeList) {
                ItemStack originalAxe = axeProjectile.getItemStack();
                CraftPlayer craftPlayer = (CraftPlayer) player;
                craftPlayer.getHandle().drop(net.minecraft.world.item.ItemStack.fromBukkitCopy(originalAxe), true, true, true, item -> {
                            item.setVelocity(new Vector(0,0,0));
                            item.teleport(deathLocation);
                        });
                /*if (droppedItem != null) {
                    droppedItem.
                }*/
                axeProjectile.remove();
                axeProjectile.setMarkForRemoval(true);
            }
        }
    }

    private void returnAllAxesToPlayer(Player player) {
        List<AxeProjectile> axeList = data.remove(player);
        if (axeList != null) {
            for (AxeProjectile axeProjectile : axeList) {
                returnAxeToPlayer(player, axeProjectile);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        returnAllAxesToPlayer(player);
    }

    private void returnAxeToPlayer(Player player, AxeProjectile axeProjectile) {
        ItemStack originalAxe = axeProjectile.getItemStack();

        if (player.getInventory().addItem(originalAxe).isEmpty()) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
        } else {
            CraftPlayer craftPlayer = (CraftPlayer) player;
            craftPlayer.getHandle().drop(net.minecraft.world.item.ItemStack.fromBukkitCopy(originalAxe), true, true, true, item -> {
                item.setVelocity(new Vector(0,0,0));
            });
        }

        axeProjectile.remove();
        axeProjectile.setMarkForRemoval(true);
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        duration = getConfig("duration", 10.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.4, Double.class);
        speed = getConfig("speed", 30.0, Double.class);
    }
}
