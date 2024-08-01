package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WallKick extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill {

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();
    private final WeakHashMap<Player, Boolean> didEntityWallKick = new WeakHashMap<>();
    private double wallKickStrength;
    private double fallDamageLimit;
    private double damage;
    private double damageIncreasePerLevel;
    private static final long COLLISION_DELAY = 250L;

    @Inject
    public WallKick(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wall Kick";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "When your back is against a wall or entity,",
                "right click to kick off and leap forwards",
                "",
                "Colliding with enemies will deal " + getValueString(this::getDamage, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    @Override
    public void activate(Player player, int level) {
        doWallKick(player);
    }

    public void doWallKick(Player player) {
        Vector vec = player.getLocation().getDirection();
        vec.setY(0);
        VelocityData velocityData = new VelocityData(vec, wallKickStrength, false, 0.0D, 0.3D, 1.0D, true);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);

        player.setFallDistance(0);
        player.getWorld().spawnEntity(player.getLocation(), EntityType.LLAMA_SPIT);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 2.0F, 1.2F);

        UtilServer.runTaskLater(champions, () -> {
            championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL, getName(), (int) fallDamageLimit,
                    50L, true, true, UtilBlock::isGrounded);
        }, 3L);

        active.put(player, System.currentTimeMillis());
    }

    @Override
    public boolean canUse(Player player){
        if(wallKick(player)){
            return true;
        }
        UtilMessage.message(player, getClassType().getName(), "Your back is not to a wall or entity.");
        return false;
    }

    public boolean wallKick(Player player) {
        Vector vec = player.getLocation().getDirection();

        boolean[] directionFlags = getDirectionFlags(vec);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if ((x != 0) || (z != 0)) {
                    if (isWallKickable(directionFlags, x, z, player)) {
                        Block forward = getForwardBlock(vec, player);
                        if (UtilBlock.airFoliage(forward)) {
                            didEntityWallKick.put(player, false);
                            return true;
                        }
                    }
                }
            }
        }

        for (LivingEntity entity : UtilEntity.getNearbyEnemies(player, player.getLocation(), 2)) {
            if (entity != player) {
                didEntityWallKick.put(player, true);
                return true;
            }
        }
        return false;
    }

    private boolean[] getDirectionFlags(Vector vec) {
        boolean xPos = vec.getX() >= 0.0D;
        boolean zPos = vec.getZ() >= 0.0D;
        return new boolean[]{xPos, zPos};
    }

    private boolean isWallKickable(boolean[] directionFlags, int x, int z, Player player) {
        boolean xPos = directionFlags[0];
        boolean zPos = directionFlags[1];
        if (((!xPos) || (x <= 0)) && ((!zPos) || (z <= 0)) && ((xPos) || (x >= 0)) && ((zPos) || (z >= 0))) {
            Block back = player.getLocation().getBlock().getRelative(x, 0, z);
            if (!UtilBlock.airFoliage(back)) {
                return back.getLocation().getY() == Math.floor(player.getLocation().getY());
            }
        }
        return false;
    }

    private Block getForwardBlock(Vector vec, Player player) {
        Block forward;
        if (Math.abs(vec.getX()) > Math.abs(vec.getZ())) {
            if (vec.getX() >= 0) {
                forward = player.getLocation().getBlock().getRelative(1, 1, 0);
            } else {
                forward = player.getLocation().getBlock().getRelative(-1, 1, 0);
            }
        } else if (vec.getZ() >= 0) {
            forward = player.getLocation().getBlock().getRelative(0, 1, 1);
        } else {
            forward = player.getLocation().getBlock().getRelative(0, 1, -1);
        }
        return forward;
    }

    @UpdateEvent
    public void checkCollision() {
        Iterator<Map.Entry<Player, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, Long> next = it.next();
            Player player = next.getKey();
            if (player.isDead()) {
                it.remove();
                continue;
            }

            final Location midpoint = UtilPlayer.getMidpoint(player).clone();

            final Optional<LivingEntity> hit = UtilEntity.interpolateCollision(midpoint,
                            midpoint.clone().add(player.getVelocity().normalize().multiply(0.5)),
                            (float) 0.6,
                            ent -> UtilEntity.IS_ENEMY.test(player, ent))
                    .map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast);

            if (hit.isPresent()) {
                doWallKickCollision(player, hit.get(), next.getValue());
                continue;
            }

            if (UtilBlock.isGrounded(player) && UtilTime.elapsed(next.getValue(), 750L)) {
                it.remove();
            }
        }
    }

    public void doWallKickCollision(Player player, LivingEntity target, long activationTime) {
        didEntityWallKick.getOrDefault(player, false);
        if (System.currentTimeMillis() - activationTime < COLLISION_DELAY && didEntityWallKick.get(player)) {
            return;
        }

        int level = player.getLevel();

        UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>" + target.getName() + "</alt2> with <alt>Leap<alt>.");
        CustomDamageEvent cde = new CustomDamageEvent(target, player, null, DamageCause.CUSTOM, getDamage(level), false, "Leap");
        cde.setDamageDelay(0);
        UtilDamage.doCustomDamage(cde);

        UtilMessage.simpleMessage(target, getClassType().getName(), "<alt2>" + player.getName() + "</alt2> landed on you with <alt>Leap</alt>.");

        for (LivingEntity entity : UtilEntity.getNearbyEnemies(player, target.getLocation(), 1)) {
            if (entity != player && entity != target) {
                UtilDamage.doCustomDamage(new CustomDamageEvent(entity, player, null, DamageCause.CUSTOM, getDamage(level), false, "Leap Landing"));
            }
        }

        active.remove(player);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    private double getDamage(int level) {
        return damage + ((level -1 ) * damageIncreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        wallKickStrength = getConfig("wallKickStrength", 1.0, Double.class);
        fallDamageLimit = getConfig("fallDamageLimit", 15.0, Double.class);
        damage = getConfig("damage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0,Double.class);
    }
}
