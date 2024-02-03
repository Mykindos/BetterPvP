package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;

@Singleton
@BPvPListener
public class Void extends ActiveToggleSkill implements EnergySkill {

    public double baseDamageReduction;
    public double damageReductionIncreasePerLevel;
    public int baseEnergyReduction;
    public int energyReductionDecreasePerLevel;
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
                "<effect>Slownesss " + UtilFormat.getRomanNumeral(slownessStrength + 1) + "</effect>, and take no Knockback",
                "",
                "Reduces incoming damage by <stat>" + getDamageReduction(level) + "</stat>,",
                "but burns <stat>" + getEnergyReduction(level) + "</stat> of your energy",
                "",
                "Energy / Second: <val>" + getEnergy(level)
        };
    }

    public double getDamageReduction(int level) {
        return baseDamageReduction + level * damageReductionIncreasePerLevel;
    }

    public double getEnergyReduction(int level) {
        return baseEnergyReduction - level * energyReductionDecreasePerLevel;
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
        if (championsManager.getEnergy().use(player, "Void", 5, false)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "Void: <green>On");
        } else {
            cancel(player);
        }
    }

    @Override
    public void cancel(Player player, String reason) {
        super.cancel(player, reason);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SLOW);
        championsManager.getEffects().removeEffect(player, EffectType.NO_JUMP);
    }

    private void audio(Player player) {

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2F, 0.5F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2F, 0.5F);

    }

    private boolean doVoid(Player player) {

        int level = getLevel(player);
        if (level <= 0 || !championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)) {
            return false;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 21, slownessStrength));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 21, 1, false, false, false));
        championsManager.getEffects().addEffect(player, EffectType.NO_JUMP, 21);

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

        event.setDamage(event.getDamage() - getDamageReduction(level));
        championsManager.getEnergy().degenerateEnergy(damagee, getEnergyReduction(level) / 100);

        event.setKnockback(false);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.removePotionEffect(PotionEffectType.SLOW);
            championsManager.getEffects().removeEffect(player, EffectType.NO_JUMP);
            UtilMessage.simpleMessage(player, getClassType().getName(), "Void: <red>Off");
        } else {
            active.add(player.getUniqueId());

        }
    }


    public void loadSkillConfig() {
        baseDamageReduction = getConfig("baseDamageReduction", 2.0, Double.class);
        damageReductionIncreasePerLevel = getConfig("damageReductionIncreasePerLevel", 0.2, Double.class);

        baseEnergyReduction = getConfig("baseEnergyReduction", 20, Integer.class);
        energyReductionDecreasePerLevel = getConfig("energyReductionDecreasePerLevel", 1, Integer.class);

        slownessStrength = getConfig("slownessStrength", 2, Integer.class);
    }
}
