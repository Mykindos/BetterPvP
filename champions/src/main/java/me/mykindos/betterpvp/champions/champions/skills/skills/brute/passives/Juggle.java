package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Color;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;


@Singleton
@BPvPListener
public class Juggle extends Skill implements PassiveSkill, OffensiveSkill, CrowdControlSkill, Listener {

    private double timeBetweenCharges;
    private double timeBetweenChargesDecreasePerLevel;
    private int baseCharges;
    private int chargesIncreasePerLevel;

    private double velocity;
    private double yMax;
    private double yAdd;
    private double ySet;

    private final HashMap<UUID, Integer> charges = new HashMap<>();

    @Inject
    public Juggle(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Juggle";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "While you're airborne, striking enemies",
                "briefly sends them flying upward.",
                "",
                "Store up to " + getValueString(this::getMaxCharges, level) + " charges",
                "",
                "Gain a charge every: " + getValueString(this::getTimeBetweenCharges, level) + " seconds"
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    public int getMaxCharges(int level) {
        return baseCharges + ((level - 1) * chargesIncreasePerLevel);
    }

    public double getTimeBetweenCharges(int level) {
        return timeBetweenCharges - ((level - 1) * timeBetweenChargesDecreasePerLevel);
    }



    @EventHandler (priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.isCancelled()) return;
        int level = getLevel(player);
        if (level <= 0) return;

        // Check if the player is airborne
        if (UtilBlock.isGrounded(player, 1) || UtilBlock.isInWater(player)) return;

        UUID playerUUID = player.getUniqueId();
        int charge = charges.getOrDefault(playerUUID, 0);
        if (charge <= 0) return;

        // Either remove the charge or decrement it
        int remainingCharges = Math.max(0, charge - 1);
        charges.put(playerUUID, remainingCharges);

        // Skill logic starts here!
        event.setKnockback(false);
        event.addReason(getName());

        LivingEntity target = event.getDamagee();

        // Higher priority allows for this to override Stampede's knockback
        Vector upward = new Vector(0, 1, 0);
        VelocityData enemyVelocityData = new VelocityData(upward, velocity, false, ySet, yAdd, yMax, true);
        UtilVelocity.velocity(target, player, enemyVelocityData, VelocityType.CUSTOM);

        // from skill listener - L380
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s<gray>.", getName());

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_DEFLECT, 1.0F, 1.0F);

        Location base = target.getLocation().add(0, 0.8, 0);
        var dust = new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.2f);

        for (double y = 0; y <= 1.5; y += 0.05) { // lower max height and smaller step for density
            double r = 0.2 + y * 0.2; // tighter spiral
            for (int i = 0; i < 3; i++) {
                double angle = y * 4 + i * Math.PI * 2 / 3; // faster rotation
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                base.getWorld().spawnParticle(Particle.DUST, base.clone().add(x, y, z), 0, dust);
            }
        }
    }

    @UpdateEvent(delay = 250)
    public void addCharge() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            UUID playerUUID = player.getUniqueId();
            if (level <= 0) {
                charges.remove(playerUUID);
                continue;
            }
            if (!charges.containsKey(playerUUID)) {
                charges.put(playerUUID, 0);
                continue;
            }

            if (!championsManager.getCooldowns().use(player, getName(), getTimeBetweenCharges(level), false)) return;

            int charge = charges.get(playerUUID);
            if (charge < getMaxCharges(level)) {
                charge = Math.min(getMaxCharges(level), charge + 1);
                notifyCharges(player, charge);
                charges.put(playerUUID, charge);
            }
        }
    }

    private void notifyCharges(Player player, int charges) {
        UtilMessage.simpleMessage(player, getClassType().getName(), getName() + " Charges: <alt2>" + charges);
    }

    @Override
    public void loadSkillConfig() {

        timeBetweenCharges = getConfig("timeBetweenCharges", 7.0, Double.class);
        timeBetweenChargesDecreasePerLevel = getConfig("timeBetweenChargesDecreasePerLevel", 1.0, Double.class);
        baseCharges = getConfig("baseCharges", 2, Integer.class);
        chargesIncreasePerLevel = getConfig("chargesIncreasePerLevel", 0, Integer.class);
        velocity = getConfig("velocity", 0.8, Double.class);
        yAdd = getConfig("yAdd", 0.3, Double.class);
        yMax = getConfig("yMax", 0.5, Double.class);
        ySet = getConfig("ySet", 0.0D, Double.class);
    }
}
