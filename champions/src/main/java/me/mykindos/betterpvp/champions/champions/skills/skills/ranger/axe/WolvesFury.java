package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WolvesFury extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int strengthStrength;

    @Inject
    public WolvesFury(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wolves Fury";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Summon the power of the wolf, gaining",
                "<effect>Strength " + UtilFormat.getRomanNumeral(strengthStrength) + "</effect> for <val>" + getDuration(level) + "</val> seconds, and giving",
                "no knockback on your attacks",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + level * durationIncreasePerLevel;
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
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @EventHandler
    public void onDamage(CustomDamageEvent e) {
        if(e.getCause() != DamageCause.ENTITY_ATTACK) return;
        if(!(e.getDamager() instanceof Player damager)) return;
        if(!active.containsKey(damager)) return;

        int level = getLevel(damager);
        if(level > 0) {
            e.setKnockback(false);
            e.addReason(getName());
        }

    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        active.entrySet().removeIf(entry -> entry.getValue() - System.currentTimeMillis() <= 0);
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
        active.put(player, (long) (System.currentTimeMillis() + ((baseDuration + level) * 1000L)));
        championsManager.getEffects().addEffect(player, EffectType.STRENGTH, 1, (long) ((baseDuration + level) * 1000L));
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);

        strengthStrength = getConfig("strengthStrength", 1, Integer.class);
    }
}
