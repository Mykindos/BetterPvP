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
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

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
                "Right click with an Axe to activate",
                "",
                "Instantly teleport forwards <stat>15</stat> Blocks",
                "",
                "Using again within <stat>5</stat> seconds De-Blinks,",
                "returning you to your original location",
                "",
                "Cannot be used while <effect>Slowed</effect>",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public String getDefaultClassString() {
        return "assassin";
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
            if (!championsManager.getCooldowns().hasCooldown(player, "Deblink") || force) {

                if (!force) {
                    UtilMessage.simpleMessage(player, "Champions", "You used <alt>Deblink " + getLevel(player) + "</alt>.");
                } else {
                    UtilMessage.simpleMessage(player, "Champions", "The target location was invalid, Blink cooldown has been reduced.");
                    championsManager.getCooldowns().removeCooldown(player, "Blink", true);
                    championsManager.getCooldowns().use(player, "Blink", 2, true);
                }

                Location target = this.loc.remove(player);
                float currentYaw = player.getLocation().getYaw();
                float currentPitch = player.getLocation().getPitch();

                drawBlinkLine(player.getLocation(), target);

                target.setYaw(currentYaw);
                target.setPitch(currentPitch);

                player.teleport(target);
                player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
            }
        }, 1);
    }


    @Override
    public boolean canUse(Player player) {


        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            UtilMessage.simpleMessage(player, "Champions", "You cannot use " + getName() + " while Slowed.");
            return false;
        }

        if (championsManager.getEffects().hasEffect(player, EffectType.STUN)) {
            UtilMessage.simpleMessage(player, "Champions", "You cannot use <alt>%s</alt> while stunned.", getName());
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

                    if (!UtilPlayer.getNearbyPlayers(player, targetLocation, 0.5D, EntityProperty.ENEMY).isEmpty()) {
                        break;
                    }
                } else {
                    break;
                }
            }

            drawBlinkLine(player.getLocation(), targetLocation);

            blinkTime.put(player, System.currentTimeMillis());
            loc.put(player, player.getLocation());

            Location finalLocation = targetLocation.add(direction.clone().multiply(-1));
            player.leaveVehicle();
            player.teleport(finalLocation);

            championsManager.getCooldowns().use(player, "Deblink", 0.25, false);
            player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
        }, 1);
    }


    private void drawBlinkLine(Location from, Location to) {
        World world = from.getWorld();
        double distance = from.distance(to);
        Vector vector = to.toVector().subtract(from.toVector()).normalize().multiply(0.1);
        Location location = from.clone();

        for (double length = 0; length < distance; length += 0.1) {
            world.spawnParticle(Particle.SMOKE_LARGE, location, 0, 0, 0, 0, 0);
            location.add(vector);
        }
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
