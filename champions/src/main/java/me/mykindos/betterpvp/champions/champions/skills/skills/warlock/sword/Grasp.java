package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.customtypes.CustomArmourStand;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Grasp extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, CrowdControlSkill, DamageSkill {

    private final WeakHashMap<Player, ArrayList<LivingEntity>> cooldownJump = new WeakHashMap<>();
    private final HashMap<ArmorStand, Long> stands = new HashMap<>();

    private double baseDistance;

    private double distanceIncreasePerLevel;

    private double baseDamage;

    private double damageIncreasePerLevel;

    @Inject
    public Grasp(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Grasp";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Create a wall of skulls that closes in on",
                "you from " + getValueString(this::getDistance, level) + " blocks away, dragging along",
                "all enemies and dealing " + getValueString(this::getDamage, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)

        };
    }

    public double getDistance(int level) {
        return baseDistance + ((level - 1) * distanceIncreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }


    private void createArmourStand(Player player, Location loc, int level) {
        CustomArmourStand as = new CustomArmourStand(((CraftWorld) loc.getWorld()).getHandle());
        ArmorStand test = (ArmorStand) as.spawn(loc);
        test.setVisible(false);
        // ArmorStand test = (ArmorStand) p.getWorld().spawnEntity(tempLoc, EntityType.ARMOR_STAND);
        test.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
        test.setGravity(false);

        test.setSmall(true);
        test.setHeadPose(new EulerAngle(UtilMath.randomInt(360), UtilMath.randomInt(360), UtilMath.randomInt(360)));

        stands.put(test, System.currentTimeMillis() + 200);

        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, loc, 1)) {
            if (target.getLocation().distance(player.getLocation()) < 3) continue;
            Location targetLocation = player.getLocation();
            targetLocation.add(targetLocation.getDirection().normalize().multiply(2));

            if (!cooldownJump.get(player).contains(target)) {

                UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, getName()));
                cooldownJump.get(player).add(target);
                VelocityData velocityData = new VelocityData(UtilVelocity.getTrajectory(target.getLocation(), targetLocation), 1.0, false, 0, 0.5, 1, true);
                UtilVelocity.velocity(target, player, velocityData);
            }
        }

    }


    @UpdateEvent
    public void onUpdate() {
        Iterator<Map.Entry<ArmorStand, Long>> it = stands.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ArmorStand, Long> next = it.next();
            if (next.getValue() - System.currentTimeMillis() <= 0) {
                next.getKey().remove();
                it.remove();
            }
        }
    }


    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public void activate(Player player, int level) {
        Block block = player.getTargetBlock(null, (int) getDistance(level));
        Location startPos = player.getLocation();

        final Vector v = player.getLocation().toVector().subtract(block.getLocation().toVector()).normalize().multiply(0.2);
        v.setY(0);

        final Location loc = block.getLocation().add(v);
        cooldownJump.put(player, new ArrayList<>());

        final BukkitTask runnable = new BukkitRunnable() {

            @Override
            public void run() {

                boolean skip = false;
                if ((loc.getBlock().getType() != Material.AIR)
                        && UtilBlock.solid(loc.getBlock())) {

                    loc.add(0.0D, 1.0D, 0.0D);
                    if ((loc.getBlock().getType() != Material.AIR)
                            && UtilBlock.solid(loc.getBlock())) {
                        skip = true;
                    }

                }


                Location compare = loc.clone();
                compare.setY(startPos.getY());
                if (compare.distance(startPos) < 1) {
                    cancel();
                    return;
                }


                if ((loc.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType() == Material.AIR)) {
                    loc.add(0.0D, -1.0D, 0.0D);
                }


                for (int i = 0; i < 10; i++) {

                    loc.add(v);
                    if (!skip) {
                        Location tempLoc = new Location(player.getWorld(), loc.getX() + UtilMath.randDouble(-2D, 2.0D), loc.getY() + UtilMath.randDouble(0.0D, 0.5D) - 0.50,
                                loc.getZ() + UtilMath.randDouble(-2.0D, 2.0D));

                        createArmourStand(player, tempLoc.clone(), level);
                        createArmourStand(player, tempLoc.clone().add(0, 1, 0), level);
                        createArmourStand(player, tempLoc.clone().add(0, 2, 0), level);

                        if (i % 2 == 0) {
                            player.getWorld().playSound(tempLoc, Sound.ENTITY_VEX_DEATH, 0.3f, 0.3f);
                        }
                    }
                }


            }

        }.runTaskTimer(champions, 0, 2);


        new BukkitRunnable() {

            @Override
            public void run() {
                runnable.cancel();
                cooldownJump.get(player).clear();

            }

        }.runTaskLater(champions, 40);


    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        Block block = player.getTargetBlock(null, (int) getDistance(level));
        if (block.getLocation().distance(player.getLocation()) < 3) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> this close.");
            return false;
        }

        return true;
    }

    @Override
    public void loadSkillConfig() {
        baseDistance = getConfig("baseDistance", 10.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 5.0, Double.class);

        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
    }
}
