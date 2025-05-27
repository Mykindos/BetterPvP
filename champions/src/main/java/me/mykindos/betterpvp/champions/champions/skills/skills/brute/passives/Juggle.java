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
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scheduler.BPVPTask;
import me.mykindos.betterpvp.core.scheduler.TaskScheduler;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
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
public class Juggle extends Skill implements PassiveSkill, OffensiveSkill, TeamSkill, CrowdControlSkill, Listener {

    private double timeBetweenCharges;
    private double timeBetweenChargesDecreasePerLevel;
    private int baseCharges;
    private int chargesIncreasePerLevel;

    private double enemyVelocity;
    private double enemyYMax;
    private double enemyYAdd;
    private double enemyYSet;

    private double allyVelocity;
    private double allyYMax;
    private double allyYAdd;
    private double allyYSet;

    private double fallDamageLimit;

    private final HashMap<UUID, Integer> charges = new HashMap<>();

    private final TaskScheduler taskScheduler;

    @Inject
    public Juggle(Champions champions, ChampionsManager championsManager, TaskScheduler taskScheduler) {
        super(champions, championsManager);
        this.taskScheduler = taskScheduler;
    }

    @Override
    public String getName() {
        return "Juggle";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "While you're airborne, strike an ally",
                "or enemy to briefly send them upward.",
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHitAlly(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();

        if (!(cde.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level <= 0) return;

        // Check if the player is airborne
        if (UtilBlock.isGrounded(player, 1) || UtilBlock.isInWater(player)) return;

        LivingEntity potentialAlly = cde.getDamagee();

        if (UtilEntity.isEntityFriendly(player, potentialAlly)) {

            // If the player doesn't have charges, we don't want to cancel the event
            int charge = charges.getOrDefault(player.getUniqueId(), 0);
            if (charge <= 0) return;

            cde.addReason(getName());
            event.setCancelled(true);
            onHit(player, potentialAlly, true);
        }
    }


    // Higher priority allows for this to override Stampede's knockback
    @EventHandler (priority = EventPriority.HIGH)
    public void onHitEnemy(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.isCancelled()) return;

        int level = getLevel(player);
        if (level <= 0) return;

        // Check if the player is airborne
        if (UtilBlock.isGrounded(player, 1) || UtilBlock.isInWater(player)) return;

        event.setKnockback(false);
        event.addReason(getName());

        onHit(player, event.getDamagee(), false);
    }

    public void onHit(Player player, LivingEntity target, boolean isFriendly) {
        UUID playerUUID = player.getUniqueId();
        int charge = charges.getOrDefault(playerUUID, 0);
        if (charge <= 0) return;

        // Either remove the charge or decrement it
        int remainingCharges = Math.max(0, charge - 1);
        charges.put(playerUUID, remainingCharges);

        Vector upward = new Vector(0, 1, 0);

        VelocityData velocityData;
        if (isFriendly) {
            velocityData = new VelocityData(upward, allyVelocity, false, allyYSet, allyYAdd, allyYMax, true);

            taskScheduler.addTask(new BPVPTask(target.getUniqueId(), uuid -> !UtilBlock.isGrounded(uuid), uuid -> {
                Player allyToApplyNoFallTo = Bukkit.getPlayer(uuid);
                if (allyToApplyNoFallTo != null) {
                    championsManager.getEffects().addEffect(allyToApplyNoFallTo, player, EffectTypes.NO_FALL, getName(), (int) fallDamageLimit,
                            250L, true, true, UtilBlock::isGrounded);
                }
            }, 1000));
        } else {
            velocityData = new VelocityData(upward, enemyVelocity, false, enemyYSet, enemyYAdd, enemyYMax, true);
        }

        UtilVelocity.velocity(target, player, velocityData, VelocityType.CUSTOM);

        // No feedback messages but send sound for everyone
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_DEFLECT, 2.0F, 0.8F);


        // A dark purple dust particle effect
        var dust = new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.2f);

        Location base = target.getLocation().add(0, 0.8, 0);

        // Spawn particles in a spiral pattern around the target (more of a cone though)
        for (double y = 0; y <= 1.5; y += 0.05) {
            double radius = 0.2 + y * 0.2; // tighter spiral
            for (int i = 0; i < 3; i++) {
                double angle = y * 4 + i * Math.PI * 2 / 3;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
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

        enemyVelocity = getConfig("enemyVelocity", 0.8, Double.class);
        enemyYAdd = getConfig("enemyYAdd", 0.3, Double.class);
        enemyYMax = getConfig("enemyYMax", 0.5, Double.class);
        enemyYSet = getConfig("enemyYSet", 0.0D, Double.class);

        allyVelocity = getConfig("allyVelocity", 1.2, Double.class);
        allyYAdd = getConfig("allyYAdd", 0.6, Double.class);
        allyYMax = getConfig("allyYMax", 0.8, Double.class);
        allyYSet = getConfig("allyYSet", 0.0D, Double.class);

        fallDamageLimit = getConfig("fallDamageLimit", 5.0, Double.class);
    }
}
