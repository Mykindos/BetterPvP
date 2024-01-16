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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WolfsFury extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();
    private final WeakHashMap<Player, Integer> missedSwings = new WeakHashMap<>();
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int strengthStrength;
    private int maxMissedSwings;

    @Inject
    public WolfsFury(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wolfs Fury";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Summon the power of the wolf, gaining",
                "<effect>Strength " + UtilFormat.getRomanNumeral(strengthStrength + 1) + "</effect> for <val>" + getDuration(level) + "</val> seconds, and giving",
                "no knockback on your attacks",
                "",
                "If you miss <stat>" + maxMissedSwings + "</stat> consecutive attacks,",
                "Wolfs Fury ends",
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
        missedSwings.put(damager, 0);

    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        active.entrySet().removeIf(entry -> {
            if (entry.getValue() - System.currentTimeMillis() <= 0) {
                missedSwings.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void onMiss(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        int level = getLevel(player);

        if(level > 0) {
            if (active.containsKey(player)) {
                missedSwings.put(player, missedSwings.getOrDefault(player, 0) + 1);
                if (missedSwings.get(player) >= maxMissedSwings) {
                    active.remove(player);
                    missedSwings.remove(player);
                    UtilMessage.message(player, getClassType().getName(), "<alt>" + getName() + "</alt> ended early");
                }
            }
        }
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 2f, 1.2f);
        active.put(player, (long) (System.currentTimeMillis() + (getDuration(level) * 1000L)));
        championsManager.getEffects().addEffect(player, EffectType.STRENGTH, 1, (long) (getDuration(level) * 1000L));
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        maxMissedSwings = getConfig("maxMissedSwings", 2, Integer.class);
        strengthStrength = getConfig("strengthStrength", 3, Integer.class);
    }
}
