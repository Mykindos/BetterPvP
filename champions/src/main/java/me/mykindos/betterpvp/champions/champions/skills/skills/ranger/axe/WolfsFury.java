package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
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
import org.bukkit.inventory.EquipmentSlot;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WolfsFury extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, BuffSkill {
    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();
    private final WeakHashMap<Player, Integer> missedSwings = new WeakHashMap<>();
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int strengthLevel;

    private int baseMissedSwings;
    private double missedSwingsIncreasePerLevel;

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
                "<effect>Strength " + UtilFormat.getRomanNumeral(strengthLevel) + "</effect> for " + getValueString(this::getDuration, level) + " seconds and giving",
                "no knockback on your attacks",
                "",
                "If you miss " + getValueString(this::getMaxMissedSwings, level) + " consecutive attacks",
                "Wolfs Fury ends",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.STRENGTH.getDescription(strengthLevel),
        };
    }

    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    public int getMaxMissedSwings(int level) {
        return (int) Math.floor(baseMissedSwings + ((level - 1) * missedSwingsIncreasePerLevel));
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
        if (e.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!active.containsKey(damager)) return;

        int level = getLevel(damager);
        if (level > 0) {
            e.setKnockback(false);
            e.addReason(getName());
        }
        missedSwings.put(damager, 0);

    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        active.entrySet().removeIf(entry -> expire(entry.getKey(), false));
    }

    private boolean expire(Player player, boolean force) {
        if (player == null) {
            return true;
        }

        if ((active.get(player) - System.currentTimeMillis() <= 0) || force) {
            missedSwings.remove(player);
            deactivate(player);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onMiss(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isLeftClick()) return;

        Player player = event.getPlayer();

        int level = getLevel(player);

        if (level > 0) {
            if (active.containsKey(player)) {
                missedSwings.put(player, missedSwings.getOrDefault(player, 0) + 1);
                if (missedSwings.get(player) >= getMaxMissedSwings(level)) {
                    expire(player, true);
                    active.remove(player);
                }
            }
        }
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 2f, 1.2f);
        active.put(player, (long) (System.currentTimeMillis() + (getDuration(level) * 1000L)));
        championsManager.getEffects().addEffect(player, EffectTypes.STRENGTH, getName(), strengthLevel, (long) (getDuration(level) * 1000L));
    }

    public void deactivate(Player player) {
        UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s %s</green> has ended.", getName(), getLevel(player)));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 2f, 1);

        championsManager.getEffects().removeEffect(player, EffectTypes.STRENGTH, getName());

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        baseMissedSwings = getConfig("baseMissedSwings", 2, Integer.class);
        missedSwingsIncreasePerLevel = getConfig("missedSwingsIncreasePerLevel", 0.5, Double.class); // 1 extra swing per 2 levels
        strengthLevel = getConfig("strengthLevel", 2, Integer.class);
    }
}
