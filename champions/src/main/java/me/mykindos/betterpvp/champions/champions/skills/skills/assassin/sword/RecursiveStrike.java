package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class RecursiveStrike extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, Integer> playerHits = new WeakHashMap<>();
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double distance;
    private int numHitsRequired;
    @Inject
    private CooldownManager cooldownManager;

    @Inject
    public RecursiveStrike(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Recursive Strike";
    }


    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Dash forwards <stat>" + distance + "</stat> blocks and spin around,",
                "dealing <val>" + getDamage(level) + "</val> damage to anything you pass through",
                "",
                "Landing <val>" + numHitsRequired + "</val> hits will reset the cooldown",
                "",
                "Cooldown: <val>" + getCooldown(level)};
    }

    public double getDamage(int level) {
        return baseDamage + (level - 1) * damageIncreasePerLevel;
    }

    @Override
    public void activate(Player player, int level) {
        Location originalLocation = player.getLocation();
        final Vector direction = player.getEyeLocation().getDirection();
        Location teleportLocation = originalLocation.clone();

        playerHits.put(player, 0);

        final int iterations = (int) Math.ceil(distance / 0.2);
        for (int i = 1; i <= iterations; i++) {
            final Vector increment = direction.clone().multiply(0.2 * i);
            final Location checkLocation = originalLocation.clone().add(increment);

            for (Entity entity : player.getWorld().getNearbyEntities(checkLocation, 0.5, 0.5, 0.5)) {
                if (entity instanceof Player && entity != player) {
                    LivingEntity target = (LivingEntity) entity;

                    CustomDamageEvent cde = new CustomDamageEvent(target, player, player, EntityDamageEvent.DamageCause.CUSTOM, 2.0, false);
                    UtilDamage.doCustomDamage(cde);
                    int currentHits = playerHits.getOrDefault(player, 0);
                    if (currentHits > 0) {
                        currentHits -= 1;
                        playerHits.put(player, currentHits);
                    }

                    break;
                }
            }

            BoundingBox relativeBoundingBox = UtilLocation.copyAABBToLocation(player.getBoundingBox(), checkLocation);

            final Location blockOnTop = checkLocation.clone().add(0, 1.0, 0);
            if (wouldCollide(blockOnTop.getBlock(), relativeBoundingBox)) {
                break;
            }

            Location newTeleportLocation = checkLocation;
            if (wouldCollide(checkLocation.getBlock(), relativeBoundingBox)) {
                if (!blockOnTop.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()) {
                    break;
                }

                final Vector horizontalIncrement = increment.clone().setY(0);
                final Location frontLocation = originalLocation.clone().add(horizontalIncrement);
                relativeBoundingBox = UtilLocation.copyAABBToLocation(player.getBoundingBox(), frontLocation);
                if (wouldCollide(frontLocation.getBlock(), relativeBoundingBox)) {
                    continue;
                }

                newTeleportLocation = frontLocation;
            }

            final Location headBlock = checkLocation.clone().add(0.0, relativeBoundingBox.getHeight(), 0.0);
            if (wouldCollide(headBlock.getBlock(), relativeBoundingBox)) {
                break;
            }

            if (!player.hasLineOfSight(checkLocation) && !player.hasLineOfSight(headBlock)) {
                break;
            }

            teleportLocation = newTeleportLocation;
        }

        float newYaw = (player.getLocation().getYaw() + 180) % 360;
        teleportLocation.setYaw(newYaw);
        teleportLocation.setPitch(-player.getLocation().getPitch());

        player.leaveVehicle();
        teleportLocation = UtilLocation.shiftOutOfBlocks(teleportLocation, player.getBoundingBox());

        Particle.GUST.builder().location(player.getLocation()).count(1).receivers(30).extra(0).spawn();
        Particle.GUST.builder().location(teleportLocation).count(1).receivers(30).extra(0).spawn();

        player.teleportAsync(teleportLocation);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getDamagee() instanceof Player)) return;
        if (event.isCancelled()) return;

        int numHits = playerHits.getOrDefault(player, 0);
        numHits++;
        playerHits.put(player, numHits);
        int level = getLevel(player);

        if (numHits >= numHitsRequired) {
            cooldownManager.removeCooldown(player, getName(), true);
            cooldownManager.use(player, getName(), 0, true);

            playerHits.put(player, 0);
        }
    }


    private boolean wouldCollide(Block block, BoundingBox boundingBox) {
        return !block.isPassable() && UtilBlock.doesBoundingBoxCollide(boundingBox, block);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 3.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        distance = getConfig("distance", 4.0, Double.class);
        numHitsRequired = getConfig("numHitsRequired", 3, Integer.class);
    }
}
