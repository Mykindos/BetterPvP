package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.ComboAttackData;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@CustomLog
public class ComboAttack extends Skill implements PassiveSkill, Listener, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Player, ComboAttackData> repeat = new WeakHashMap<>();

    private double baseDamageIncrement;
    private double damageIncrementPerLevel;
    private double baseMaxDamage;
    private double maxDamageIncrementPerLevel;
    private double duration;
    private double durationIncreasePerLevel;


    @Inject
    public ComboAttack(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Combo Attack";
    }


    @Override
    public Component[] getDescription(int level) {
        Component increment = getValueComponent(this::getDamageIncrement, level);
        Component max = getValueComponent(this::getMaxDamageIncrement, level);
        Component duration = getValueComponent(this::getDuration, level);
        return Translations.componentLines(
                "champions.skill.assassin.combo-attack.description",
                increment,
                max,
                duration
        );
    }

    public double getMaxDamageIncrement(int level) {
        return baseMaxDamage + (level - 1) * maxDamageIncrementPerLevel;
    }

    private double getDamageIncrement(int level) {
        return baseDamageIncrement + (level - 1) * damageIncrementPerLevel;
    }

    private double getDuration(int level) {
        return duration + (level - 1) * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!championsManager.getRoles().hasRole(damager, Role.ASSASSIN)) return;

        int level = getLevel(damager);
        if (level > 0) {

            ComboAttackData comboAttackData = repeat.computeIfAbsent(damager, v -> new ComboAttackData(event.getDamagee().getUniqueId(), 0, System.currentTimeMillis()));

            if (comboAttackData.getLastTarget() != event.getDamagee().getUniqueId()) {
                repeat.remove(damager);
                return;
            }

            double cur = comboAttackData.getDamageIncrement();
            event.addModifier(new SkillDamageModifier.Flat(this, cur));

            comboAttackData.setDamageIncrement(Math.min(cur + getDamageIncrement(level), getMaxDamageIncrement(level)));
            comboAttackData.setLastTarget(event.getDamagee().getUniqueId());
            comboAttackData.setLast(System.currentTimeMillis());

            damager.getWorld().playSound(damager.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, (float) (0.7f + (0.3f * comboAttackData.getDamageIncrement())));

        }
    }


    /**
     * All sword, axe, and bow skills cancel. Otherwise, if it is an interact or toggle skill it should also cancel
     * @param skill the skill
     * @return {@code true} if the skill should cancel a combo attack, {@code false} otherwise
     */
    private boolean shouldCancelCombo(IChampionsSkill skill) {
        if (skill.getType() == SkillType.SWORD || skill.getType() == SkillType.AXE || skill.getType() == SkillType.BOW) {
            return true;
        }
        return skill instanceof ToggleSkill || skill instanceof InteractSkill;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillUse(PlayerUseSkillEvent event) {
        final Player player = event.getPlayer();
        final int level = getLevel(player);
        if (level <= 0) return;
        if (!repeat.containsKey(player)) return;

        if (!shouldCancelCombo(event.getSkill())) return;

        final ComboAttackData data = repeat.remove(player);
        if (data == null) return;
        endInfo(player, level, data.getDamageIncrement());

    }

    @UpdateEvent(delay = 50)
    public void onUpdate() {
        Iterator<Player> iterator = repeat.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            int level = getLevel(player);
            if (UtilTime.elapsed(repeat.get(player).getLast(), (long) (getDuration(level) * 1000))) {
                endInfo(player, level, repeat.get(player).getDamageIncrement());
                iterator.remove();
            }
        }
    }

    public void endInfo(Player player, int level, double combo) {
        UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.combo-attack.ended", getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN), Component.text(String.format("%.1f", combo), NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 5.0f);
    }


    @Override
    public void loadSkillConfig() {
        baseDamageIncrement = getConfig("baseDamageIncrement", 1.0, Double.class);
        damageIncrementPerLevel = getConfig("damageIncrementPerLevel", 0.0, Double.class);
        baseMaxDamage = getConfig("baseMaxDamage", 2.0, Double.class);
        maxDamageIncrementPerLevel = getConfig("maxDamageIncrementPerLevel", 1.0, Double.class);
        duration = getConfig("duration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
    }
}
