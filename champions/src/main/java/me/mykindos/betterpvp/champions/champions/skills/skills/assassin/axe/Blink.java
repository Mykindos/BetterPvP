package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.axe;

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
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Blink extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill {

    private final WeakHashMap<Player, Location> loc = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> blinkTime = new WeakHashMap<>();
    private int maxTravelDistance;
    private int distanceIncreasePerLevel;
    private int deblinkTime;
    private int deblinkTimeIncreasePerLevel;

    @Inject
    public Blink(Champions champions, ChampionsManager championsManager) {
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
                "Instantly teleport forwards " + getValueString(this::getMaxTravelDistance, level) + " Blocks",
                "",
                "Using again within " + getValueString(this::getDeblinkTime, level) + " seconds De-Blinks,",
                "returning you to your original location",
                "",
                "Cannot be used while <effect>Slowed</effect>",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public int getDeblinkTime(int level){
        return deblinkTime + (level - 1) * deblinkTimeIncreasePerLevel;
    }

    public int getMaxTravelDistance(int level){
        return maxTravelDistance + ((level-1) * distanceIncreasePerLevel);
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
            if (!championsManager.getCooldowns().hasCooldown(player, "Deblink") || force) {

                if (!force) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>Deblink " + getLevel(player) + "</alt>.");
                } else {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "The target location was invalid, Blink cooldown has been reduced.");
                    championsManager.getCooldowns().removeCooldown(player, "Blink", true);
                    championsManager.getCooldowns().use(player, "Blink", 2, true);
                }

                Location target = this.loc.remove(player);
                if(target == null) return;

                float currentYaw = player.getLocation().getYaw();
                float currentPitch = player.getLocation().getPitch();

                drawBlinkLine(player.getLocation(), target);

                target.setYaw(currentYaw);
                target.setPitch(currentPitch);

                player.teleport(target);
                player.setFallDistance(0);
                player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
            }
        }, 1);
    }


    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        if ((loc.containsKey(player)) && (blinkTime.containsKey(player))
                && (!UtilTime.elapsed(blinkTime.get(player), getDeblinkTime(level) * 1000L))) {
            deblink(player, false);
            return false;
        }
        return true;
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @Override
    public void activate(Player player, int level) {
        double maxDistance = getMaxTravelDistance(level);
        final Location origin = player.getLocation();
        UtilLocation.teleportForward(player, maxDistance, false, success -> {
            if (!Boolean.TRUE.equals(success)) {
                return;
            }

            final Location lineStart = origin.add(0.0, player.getHeight() / 2, 0.0);
            final Location lineEnd = player.getLocation().clone().add(0.0, player.getHeight() / 2, 0.0);
            drawBlinkLine(lineStart, lineEnd);

            blinkTime.put(player, System.currentTimeMillis());
            loc.put(player, origin);

            championsManager.getCooldowns().use(player, "Deblink", 0.25, false);
            player.getWorld().playEffect(origin, Effect.BLAZE_SHOOT, 0);
            player.getWorld().playEffect(lineEnd, Effect.BLAZE_SHOOT, 0);
        });
    }


    private void drawBlinkLine(Location from, Location to) {
        World world = from.getWorld();
        double distance = from.distance(to);
        Vector vector = to.toVector().subtract(from.toVector()).normalize().multiply(0.1);
        Location location = from.clone();

        for (double length = 0; length < distance; length += 0.1) {
            world.spawnParticle(Particle.LARGE_SMOKE, location, 0, 0, 0, 0, 0);
            location.add(vector);
        }
    }

    @Override
    public void loadSkillConfig(){
        maxTravelDistance = getConfig("maxTravelDistance", 9, Integer.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 3, Integer.class);
        deblinkTime = getConfig("deblinkTime", 4, Integer.class);
        deblinkTimeIncreasePerLevel = getConfig("deblinkTimeIncreasePerLevel", 0, Integer.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
