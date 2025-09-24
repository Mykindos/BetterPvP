package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.StateSkill;
import me.mykindos.betterpvp.core.combat.damage.ModifierOperation;
import me.mykindos.betterpvp.core.combat.damage.ModifierType;
import me.mykindos.betterpvp.core.combat.damage.ModifierValue;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Agility extends StateSkill implements Listener, BuffSkill, MovementSkill, DefensiveSkill {

    private final WeakHashMap<Player, Integer> missedSwings = new WeakHashMap<>();

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double baseDamageReduction;
    private double damageReductionIncreasePerLevel;
    private double baseMissedSwings;
    private int speedStrength;
    private double missedSwingsIncreasePerLevel;

    @Inject
    public Agility(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sprint with great agility, gaining",
                "<effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> for " + getValueString(this::getDuration, level) + " seconds and ",
                getValueString(this::getDamageReduction, level, 1, "%", 0) + " reduced damage while active",
                "",
                "Agility ends if you interact",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getDamageReduction(int level) {
        return baseDamageReduction + ((level - 1) * damageReductionIncreasePerLevel);
    }

    public double getMaxMissedSwings(int level) {
        return baseMissedSwings + ((level - 1) * missedSwingsIncreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        super.activate(player, level);

        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, getName(), speedStrength, (long) (getDuration(level) * 1000));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 0.5F);
    }

    @EventHandler
    public void endOnInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        final int level = getLevel(player);
        if (level <= 0) return;

        final @NotNull UUID uuid = player.getUniqueId();
        if (!activeState.containsKey(uuid)) return;

        missedSwings.put(player, missedSwings.getOrDefault(player, 0) + 1);
        if (missedSwings.get(player) >= getMaxMissedSwings(level)) {
            doWhenStateEnds(uuid);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;

        // on player taking damage
        if (activeState.containsKey(damagee.getUniqueId())) {
            int level = getLevel(damagee);

            // Add a percentage-based damage reduction modifier
            double reductionPercent = getDamageReduction(level);
            event.getDamageModifiers().addModifier(ModifierType.DAMAGE, reductionPercent, getName(), ModifierValue.PERCENTAGE, ModifierOperation.DECREASE);
        }

        if (!(event.getDamager() instanceof Player damager)) return;
        if (!activeState.containsKey(damager.getUniqueId())) return;

        // honestly not sure what tf i am looking at
        // Why is it called missedSwings??? Why do we need a map for this????????
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            missedSwings.put(damager, 0);
        }
    }

    @Override
    protected void doOnSuccessfulUpdate(@NotNull Player player) {
        Location loc = player.getLocation();

        Random random = UtilMath.RANDOM;
        double x = loc.getX() + (random.nextDouble() - 0.5) * 0.5;
        double y = loc.getY() + (1 + (random.nextDouble() - 0.5) * 0.9);
        double z = loc.getZ() + (random.nextDouble() - 0.5) * 0.5;
        Location particleLoc = new Location(loc.getWorld(), x, y, z);
        new ParticleBuilder(Particle.EFFECT)
                .location(particleLoc)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    protected void doWhenStateEnds(@NotNull UUID uuid) {
        super.doWhenStateEnds(uuid);

        final @Nullable Player player = Bukkit.getPlayer(uuid);
        missedSwings.remove(player);  // if possible, try to remove
        if (player == null) return;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 0.01F);
        championsManager.getEffects().removeEffect(player, EffectTypes.SPEED, getName());
    }

    @Override
    protected @NotNull String getActionBarLabel() {
        return "Agile Speed";
    }

    @Override
    protected double getStateDuration(int level) {
        return getDuration(level);
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
    public String getName() {
        return "Agility";
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        baseDamageReduction = getConfig("baseDamageReduction", 60.0, Double.class);
        damageReductionIncreasePerLevel = getConfig("damageReductionIncreasePerLevel", 0.0, Double.class);
        speedStrength = getConfig("speedStrength", 3, Integer.class);
        baseMissedSwings = getConfig("baseMissedSwings", 1.0, Double.class);
        missedSwingsIncreasePerLevel = getConfig("missedSwingsIncreasePerLevel", 0.0, Double.class);
    }
}
