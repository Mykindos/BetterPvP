package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class Leap extends Skill implements InteractSkill, CooldownSkill, Listener {

    private double leapStrength;
    private double wallKickStrength;
    private double wallKickInternalCooldown;
    private double fallDamageLimit;

    private final HashMap<UUID, Long> canTakeFall = new HashMap<>();

    @Inject
    public Leap(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Leap";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate.",
                "",
                "Take a great leap forward",
                "",
                "Activate while your back is to a wall to perform",
                "a wall-kick, which will not affect the cooldown",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public void activate(Player player, int level) {
        if (!wallKick(player)) {
            doLeap(player, false);
        }
    }

    public void doLeap(Player player, boolean wallkick) {
        if (!wallkick) {
            VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), leapStrength, false, 0.0D, 0.2D, 1.0D, true);
            UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);
        } else {
            Vector vec = player.getLocation().getDirection();
            vec.setY(0);
            VelocityData velocityData = new VelocityData(vec, wallKickStrength, false, 0.0D, 0.8D, 2.0D, true);
            UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);
            UtilMessage.message(player, getClassType().getName(), "You used <alt>Wall Kick</alt>.");
        }

        player.getWorld().spawnEntity(player.getLocation(), EntityType.LLAMA_SPIT);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 2.0F, 1.2F);
        canTakeFall.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void reduceFallDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (event.getDamagee() instanceof Player player) {
            UUID playerId = player.getUniqueId();

            if (canTakeFall.containsKey(playerId)) {
                if (event.getDamage() <= fallDamageLimit) {
                    event.setCancelled(true);
                } else {
                    event.setDamage(event.getDamage() - fallDamageLimit);
                }
                canTakeFall.remove(playerId);
            }
        }
    }

    @UpdateEvent
    public void onUpdate() {
        canTakeFall.entrySet().removeIf(entry -> UtilTime.elapsed(entry.getValue(), 2000));
    }

    public boolean wallKick(Player player) {
        if (championsManager.getCooldowns().use(player, "Wall Kick", wallKickInternalCooldown, false)) {
            Vector vec = player.getLocation().getDirection();
            boolean[] directionFlags = getDirectionFlags(vec);

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if ((x != 0) || (z != 0)) {
                        if (isWallKickable(directionFlags, x, z, player)) {
                            Block forward = getForwardBlock(vec, player);
                            if (UtilBlock.airFoliage(forward)) {
                                doLeap(player, true);
                                return true;
                            }
                        }
                    }
                }
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


    @Override
    public boolean canUse(Player player) {

        return !wallKick(player);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean canUseSlowed() {
        return false;
    }

    @Override
    public void loadSkillConfig() {
        leapStrength = getConfig("leapStrength", 1.3, Double.class);
        wallKickStrength = getConfig("wallKickStrength", 0.9, Double.class);
        wallKickInternalCooldown = getConfig("wallKickInternalCooldown", 1.0, Double.class);
        fallDamageLimit = getConfig("fallDamageLimit", 6.0, Double.class);

    }
}
