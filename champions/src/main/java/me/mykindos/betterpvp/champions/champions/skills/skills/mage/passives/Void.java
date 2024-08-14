package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.HashMap;

@Singleton
@BPvPListener
public class Void extends ActiveToggleSkill implements EnergySkill, DefensiveSkill, BuffSkill {

    public double baseDamageReduction;
    public double damageReductionIncreasePerLevel;
    public double baseEnergyReduction;
    public double energyReductionDecreasePerLevel;
    public int slownessStrength;

    @Inject
    public Void(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Void";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "While in void form, you receive",
                "<effect>Slownesss " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect>, and take no knockback",
                "",
                "Every point of damage you take will be",
                "reduced by " + getValueString(this::getDamageReduction, level) + " and drain " + getValueString(this::getEnergyReduction, level) + " energy",
                "",
                "Uses " + getValueString(this::getEnergyStartCost, level) + " energy on activation",
                "Energy / Second: " + getValueString(this::getEnergy, level)
        };
    }

    public double getDamageReduction(int level) {
        return baseDamageReduction + ((level - 1) * damageReductionIncreasePerLevel);
    }

    public double getEnergyReduction(int level) {
        return baseEnergyReduction - ((level - 1) * energyReductionDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public boolean process(Player player) {
        HashMap<String, Long> updateCooldowns = updaterCooldowns.get(player.getUniqueId());

        if (updateCooldowns.getOrDefault("audio", 0L) < System.currentTimeMillis()) {
            audio(player);
            updateCooldowns.put("audio", System.currentTimeMillis() + 1000);
        }

        return doVoid(player);
    }

    @Override
    public void toggleActive(Player player) {
        if (championsManager.getEnergy().use(player, getName(), getEnergyStartCost(getLevel(player)), false)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "Void: <green>On");
        } else {
            cancel(player);
        }
    }

    @Override
    public void cancel(Player player, String reason) {
        super.cancel(player, reason);
        championsManager.getEffects().removeEffect(player, EffectTypes.INVISIBILITY, getName());
        championsManager.getEffects().removeEffect(player, EffectTypes.SLOWNESS, getName());
        championsManager.getEffects().removeEffect(player, EffectTypes.NO_JUMP, getName());
    }

    private void audio(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2F, 0.5F);
    }

    private boolean doVoid(Player player) {

        int level = getLevel(player);
        if (level <= 0 || !championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)) {
            return false;
        }

        championsManager.getEffects().addEffect(player, EffectTypes.SLOWNESS, getName(), slownessStrength, 50, true);
        championsManager.getEffects().addEffect(player, EffectTypes.INVISIBILITY, getName(), 1, 50, true);
        championsManager.getEffects().addEffect(player, EffectTypes.NO_JUMP, getName(), 1, 50, true);

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee) || !active.contains(damagee.getUniqueId())) {
            return;
        }

        int level = getLevel(damagee);
        if (level <= 0) {
            return;
        }

        double energyReduced = event.getDamage() * getEnergyReduction(level);
        event.setDamage(event.getDamage() - getDamageReduction(level));
        championsManager.getEnergy().degenerateEnergy(damagee, energyReduced / 100);

        event.setKnockback(false);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public void loadSkillConfig() {
        baseDamageReduction = getConfig("baseDamageReduction", 2.0, Double.class);
        damageReductionIncreasePerLevel = getConfig("damageReductionIncreasePerLevel", 0.5, Double.class);

        baseEnergyReduction = getConfig("baseEnergyReduction", 2.0, Double.class);
        energyReductionDecreasePerLevel = getConfig("energyReductionDecreasePerLevel", 0.5, Double.class);

        slownessStrength = getConfig("slownessStrength", 3, Integer.class);
    }
}
