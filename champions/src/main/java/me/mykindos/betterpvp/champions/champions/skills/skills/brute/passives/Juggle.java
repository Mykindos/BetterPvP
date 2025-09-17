package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.JuggleData;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
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
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;


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

    private final WeakHashMap<Player, JuggleData> data = new WeakHashMap<>();

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

    /**
     * This is pretty much the cooldown for the skill.
     */
    public double getTimeBetweenCharges(int level) {
        return timeBetweenCharges - ((level - 1) * timeBetweenChargesDecreasePerLevel);
    }

    /**
     * Checks if the player is airborne, meaning they are not grounded or in water.
     * This is used to determine if the player can use the Juggle skill.
     *
     * @param player The player to check.
     * @return true if the player is airborne, false otherwise.
     */
    private boolean isThePlayerUsingJuggleAirborne(Player player) {
        return UtilBlock.isGrounded(player, 1) || UtilBlock.isInWater(player);
    }

    /**
     * Listens for the PreCustomDamageEvent to handle the case when a player hits an ally.
     * If the player passes all checks, we cancel the event and call {@link #onHit} to activate Juggle.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHitAlly(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();

        if (!cde.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) return;
        if (!(cde.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level <= 0) return;

        if (isThePlayerUsingJuggleAirborne(player)) return;

        LivingEntity potentialAlly = cde.getDamagee();
        if (UtilEntity.isEntityFriendly(player, potentialAlly)) {

            // If the player doesn't have charges, we don't want to cancel the event
            @Nullable JuggleData juggleData = data.get(player);
            if (juggleData == null || juggleData.getCharges() <= 0) return;

            cde.addReason(getName());
            event.setCancelled(true);
            onHit(player, potentialAlly, true);
        }
    }

    /**
     * Listens for the CustomDamageEvent to handle the case when a player hits an enemy.
     * If the player passes all checks, we cancel the event and call {@link #onHit} to activate Juggle.
     * <p>
     * This method is set to HIGH priority to ensure it takes precedence over other skills; specifically,
     * we want to ignore Stampede's knockback here.
     */
    @EventHandler (priority = EventPriority.HIGH)
    public void onHitEnemy(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.isCancelled()) return;

        int level = getLevel(player);
        if (level <= 0) return;

        if (isThePlayerUsingJuggleAirborne(player)) return;

        final @Nullable JuggleData juggleData = data.get(player);
        if (juggleData == null || juggleData.getCharges() <= 0) {
            return;  // No charges available, do not proceed
        }

        event.setKnockback(false);
        event.addReason(getName());

        onHit(player, event.getDamagee(), false);
    }

    /**
     * Handles the logic for when a player hits an entity, either an ally or an enemy.
     * This method handles all the logic for this skill like applying the velocity, playing the activation sound,
     * and spawning the particle effects.
     *
     * @param player The player who hit the target.
     * @param target The entity that was hit.
     * @param isFriendly True if the target is an ally, false if it's an enemy.
     */
    public void onHit(Player player, LivingEntity target, boolean isFriendly) {

        final @Nullable JuggleData juggleData = data.get(player);
        if (juggleData == null) return;

        int level = getLevel(player);
        int curCharges = juggleData.getCharges();
        if (curCharges >= getMaxCharges(level)) {
            championsManager.getCooldowns().use(player, getName(), getTimeBetweenCharges(level), false, true, true);
        }

        final int newCharges = curCharges - 1;
        juggleData.setCharges(newCharges);
        notifyCharges(player, newCharges);

        // Movement
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

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        if (!data.containsKey(player)) {
            data.put(player, new JuggleData());
        }
    }

    /**
     * Untrack the player when they leave or when the skill is unequipped.
     * Updates the player's charges.
     */
    @UpdateEvent(delay = 250)
    public void addCharge() {
        final Iterator<Map.Entry<Player, JuggleData>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, JuggleData> entry = iterator.next();
            final Player player = entry.getKey();
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            final JuggleData juggleData = entry.getValue();

            final int charges = juggleData.getCharges();
            final int maxCharges = getMaxCharges(level);

            if (charges >= maxCharges) continue;

            if (!championsManager.getCooldowns().use(player, getName(), getTimeBetweenCharges(level), false, true, false)) {
                continue;  // skip if not enough time has passed
            }

            juggleData.addCharge();
            notifyCharges(player, juggleData.getCharges());
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
