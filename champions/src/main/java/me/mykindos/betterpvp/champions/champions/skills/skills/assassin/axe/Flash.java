package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Flash extends Skill implements InteractSkill, Listener {

    private int maxCharges;
    private double timeBetweenCharges;
    private int maxTravelDistance;

    private final WeakHashMap<Player, Location> loc = new WeakHashMap<>();
    private final WeakHashMap<Player, Integer> charges = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> lastRecharge = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> blinkTime = new WeakHashMap<>();

    @Inject
    public Flash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Flash";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a axe to activate.",
                "",
                "Instantly teleport forwards 8 Blocks.",
                "Cannot be used while Slowed.",
                "",
                "Stores up to 4 charges.",
                "",
                "Cannot be used while Slowed.",
                "Recharge: 1 charge per <val>" + (timeBetweenCharges - level) + "</val> seconds."
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
    public void skillEquipEvent(SkillEquipEvent event) {
        if (event.getSkill().getName().equalsIgnoreCase(getName())) {
            if (!charges.containsKey(event.getPlayer())) {
                charges.put(event.getPlayer(), 0);
            }

            if (!lastRecharge.containsKey(event.getPlayer())) {
                lastRecharge.put(event.getPlayer(), System.currentTimeMillis());
            }
        }
    }

    @UpdateEvent(delay = 100)
    public void recharge() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            int level = getLevel(player);
            if (level <= 0) continue;

            if (!lastRecharge.containsKey(player)) {
                lastRecharge.put(player, System.currentTimeMillis());
            }

            if(!charges.containsKey(player)) {
                charges.put(player, 0);
            }

            int charge = charges.get(player);
            if (charge == maxCharges) {
                lastRecharge.put(player, System.currentTimeMillis());
                continue;
            }

            if (UtilTime.elapsed(lastRecharge.get(player), (long) ((timeBetweenCharges * 1000L) - (level * 1000L)))) {
                charges.put(player, Math.min(maxCharges, charge + 1));
                UtilMessage.simpleMessage(player, getClassType().getName(), "Flash Charges: <alt>" + (charge + 1));
                lastRecharge.put(player, System.currentTimeMillis());
            }
        }
    }


    @EventHandler
    public void onCustomDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.SUFFOCATION) return;
        if (event.getDamager() == null) return;
        if (!(event.getDamagee() instanceof Player player)) return;

        if (blinkTime.containsKey(player)) {
            if (!UtilTime.elapsed(blinkTime.get(player), 500)) {
                deblink(player);
            }

        }
    }

    @UpdateEvent(delay = 100)
    public void onDetectGlass() {
        for (Player player : blinkTime.keySet()) {
            if (UtilTime.elapsed(blinkTime.get(player), 250)) continue;
            if (isInInvalidBlock(player)) {
                deblink(player);
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

    public void deblink(Player player) {
        UtilServer.runTaskLater(champions, () -> {

            UtilMessage.message(player, getClassType().getName(), "The target location was invalid, You will be refunded a charge shortly.");
            UtilServer.runTaskLater(champions, () -> {
                charges.put(player, Math.min(maxCharges, charges.get(player) + 1));
                UtilMessage.simpleMessage(player, getClassType().getName(), "Flash Charges: <alt>" + charges.get(player));
                lastRecharge.put(player, System.currentTimeMillis());
            }, 20);


            Location target = this.loc.remove(player);

            player.teleport(target);
            player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0, 15);

        }, 1);

    }

    @Override
    public boolean canUse(Player player) {

        if (charges.containsKey(player)) {
            if (charges.get(player) == 0) {
                UtilMessage.simpleMessage(player, getClassType().getName(), "You don't have any <alt>" + getName() + "</alt> charges.");
                return false;
            }
        }

        return true;
    }


    @Override
    public void activate(Player player, int level) {
        if (charges.getOrDefault(player, 0) <= 0) return;

        charges.put(player, charges.getOrDefault(player, 1) - 1);
        UtilServer.runTaskLater(champions, () -> {
            Vector direction = player.getLocation().getDirection();
            Location targetLocation = player.getLocation().add(0, 1, 0);

            double maxDistance = maxTravelDistance;

            for (double currentDistance = 0; currentDistance < maxDistance; currentDistance += 1) {
                Location testLocation = targetLocation.clone().add(direction.clone());
                Block testBlock = testLocation.getBlock();
                if (!UtilBlock.isWall(testBlock)) {
                    targetLocation = testLocation;
                    Particle.FIREWORKS_SPARK.builder().location(targetLocation).count(2).receivers(100).extra(0).spawn();

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

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4F, 1.2F);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SILVERFISH_DEATH, 1.0F, 1.6F);
        }, 1);

    }

    @Override
    public void loadSkillConfig() {
        maxCharges = getConfig("maxCharges", 4, Integer.class);
        timeBetweenCharges = getConfig("timeBetweenCharges", 11.0, Double.class);
        maxTravelDistance = getConfig("maxTravelDistance", 16, Integer.class);
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
