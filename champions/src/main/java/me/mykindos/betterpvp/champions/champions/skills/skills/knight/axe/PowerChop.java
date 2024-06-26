package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class PowerChop extends PrepareSkill implements CooldownSkill, DamageSkill, OffensiveSkill {

    private double timeToHit;

    private double minBonusDamage;

    private double baseBonusDamage;

    private double bonusDamageIncreasePerLevel;

    private final WeakHashMap<Player, Long> charge = new WeakHashMap<>();

    @Inject
    public PowerChop(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Power Chop";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to prepare",
                "",

                "Your next axe attack will",
                "deal " + getValueString(this::getBonusDamage, level) + " bonus damage.",
                "",
                "The attack must be made within",
                getValueString(this::getTimeToHit, level) + " seconds of being used",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getBonusDamage(int level) {
        return (Math.max(minBonusDamage, baseBonusDamage + ((level-1) * bonusDamageIncreasePerLevel)));
    }

    public double getTimeToHit(int level) {
        return timeToHit;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!charge.containsKey(player)) return;
        if (!isHolding(player)) return;

        int level = getLevel(player);
        if (level > 0) {
            event.setDamage(event.getDamage() + getBonusDamage(level));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0F, 1.0F);
            UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s %d</alt>.", event.getDamagee().getName(), getName(), level);
            event.addReason(getName());
            charge.remove(player);
        }
    }

    @UpdateEvent(delay = 100)
    public void removeFailures() {
        charge.entrySet().removeIf(entry -> {
            if (UtilTime.elapsed(entry.getValue(), (long) timeToHit * 1000)) {
                Player player = entry.getKey();
                UtilMessage.simpleMessage(player, getClassType().getName(), "You failed to use <alt>%s %d</alt>.", getName(), getLevel(player));
                player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                return true;
            }

            return false;
        });
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        charge.put(player, System.currentTimeMillis());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, 2.0f, 1.8f);
    }

    @Override
    public void loadSkillConfig() {
        timeToHit = getConfig("timeToHit", 1.0, Double.class);
        baseBonusDamage = getConfig("baseBonusDamage", 1.0, Double.class);
        bonusDamageIncreasePerLevel = getConfig("bonusDamageIncreasePerLevel", 0.5, Double.class);
        minBonusDamage = getConfig("minBonusDamage", 0.5, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
