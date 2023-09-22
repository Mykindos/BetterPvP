package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.WeakHashMap;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class Blink extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, Location> loc = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> blinkTime = new WeakHashMap<>();

    private int maxTravelDistance;

    @Inject
    public Blink(Champions champions, ChampionsManager championsManager, Champions champions1) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Blink";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a axe to activate.",
                "",
                "Instantly teleport forwards <val>15</val> Blocks.",
                "Cannot be used while Slowed.",
                "",
                "Using again within 5 seconds De-Blinks,",
                "returning you to your original location.",
                "Cannot be used while Slowed.",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @EventHandler
    public void onCustomDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.SUFFOCATION) return;
        if (event.getDamager() == null) return;
        if (!(event.getDamagee() instanceof Player player)) return;

        if (blinkTime.containsKey(player)) {
            if (!UtilTime.elapsed(blinkTime.get(player), 500)) {
                deblink(player, true);
            }

        }
    }

    @UpdateEvent(delay = 100)
    public void onDetectGlass() {
        for (Player player : blinkTime.keySet()) {
            if (UtilTime.elapsed(blinkTime.get(player), 250)) continue;
            if (isInInvalidBlock(player)) {
                deblink(player, true);
            }

        }
    }

    private boolean isInInvalidBlock(Player player) {
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                Location loc = new Location(player.getWorld(), Math.floor(player.getLocation().getX() + x),
                        player.getLocation().getY(), Math.floor(player.getLocation().getZ() + z));

                if (loc.getBlock().getType().name().contains("GLASS") || loc.getBlock().getType().name().contains("DOOR")) {
                    return true;

                }
            }
        }

        return false;
    }

    public void deblink(Player player, boolean force) {
        UtilServer.runTaskLater(champions, () -> {
            if (!championsManager.getCooldowns().isCooling(player, "Deblink") || force) {

                if (!force) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>Deblink " + getLevel(player) + "</alt>.");
                } else {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "The target location was invalid, Blink cooldown has been reduced.");
                    championsManager.getCooldowns().removeCooldown(player, "Blink", true);
                    championsManager.getCooldowns().add(player, "Blink", 2, true);
                }

                Block lastSmoke = player.getLocation().getBlock();

                double curRange = 0.0D;
                Location target = this.loc.remove(player);

                boolean done = false;
                while (!done) {
                    Vector vec = UtilVelocity.getTrajectory(player.getLocation(),
                            new Location(player.getWorld(), target.getX(), target.getY(), target.getZ()));

                    Location newTarget = player.getLocation().add(vec.multiply(curRange));


                    curRange += 0.2D;


                    lastSmoke.getWorld().playEffect(lastSmoke.getLocation(), Effect.SMOKE, 4);
                    lastSmoke = newTarget.getBlock();

                    if (UtilMath.offset(newTarget, target) < 0.4D) {
                        done = true;
                    }
                    if (curRange > 24.0D) {
                        done = true;
                    }
                }

                player.teleport(target);
                player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0, 15);
            }
        }, 1);

    }

    @Override
    public boolean canUse(Player player) {


        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You cannot use " + getName() + " while Slowed.");
            return false;
        }

        if (championsManager.getEffects().hasEffect(player, EffectType.STUN)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You cannot use <alt>%s</alt> while stunned.", getName());
            return false;
        }

        if ((loc.containsKey(player)) && (blinkTime.containsKey(player))
                && (!UtilTime.elapsed(blinkTime.get(player), 4000L))) {
            deblink(player, false);
            return false;
        }


        return true;
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1));
    }


    @Override
    public void activate(Player player, int level) {

        // Run this later as teleporting during the InteractEvent was causing it to trigger twice
        UtilServer.runTaskLater(champions, () -> {
            Vector direction = player.getLocation().getDirection();
            Location targetLocation = player.getLocation().add(0, 1, 0);

            double maxDistance = maxTravelDistance;

            for (double currentDistance = 0; currentDistance < maxDistance; currentDistance += 1) {
                Location testLocation = targetLocation.clone().add(direction.clone());
                Block testBlock = testLocation.getBlock();
                if (!UtilBlock.isWall(testBlock)) {
                    targetLocation = testLocation;
                    player.getWorld().playEffect(targetLocation, Effect.SMOKE, 4);

                    if (!UtilPlayer.getNearbyPlayers(player, targetLocation, 0.5D, EntityProperty.ENEMY).isEmpty()) {
                        break;
                    }
                } else {

                    break;
                }
            }

            blinkTime.put(player, System.currentTimeMillis());
            loc.put(player, player.getLocation());

            Location finalLocation = targetLocation.add(direction.clone().multiply(-1));
            player.leaveVehicle();
            player.teleport(finalLocation);

            championsManager.getCooldowns().add(player, "Deblink", 0.25, false);
            player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
        }, 1);


    }

    @Override
    public void loadSkillConfig(){
        maxTravelDistance = getConfig("maxTravelDistance", 16, Integer.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
