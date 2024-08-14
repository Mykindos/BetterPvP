package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Singleton
@BPvPListener
public class Riposte extends ChannelSkill implements CooldownSkill, InteractSkill, OffensiveSkill, DamageSkill, HealthSkill, DefensiveSkill {

    private final HashMap<UUID, Long> handRaisedTime = new HashMap<>();
    private final HashMap<LivingEntity, Long> stanceBroken = new HashMap<>();

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double cooldownDecrease;
    private int vulnerabilityStrength;
    private double stanceBrokenDuration;
    private double stanceBrokenDurationIncreasePerLevel;

    @Inject
    public Riposte(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Riposte";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hold right click with a Sword to activate",
                "",
                "If an enemy hits you within " + getValueString(this::getDuration, level) + " second of blocking",
                "you will parry their attack, breaking their stance",
                "and giving them <effect>Vulnerability I</effect> for " + getValueString(this::getStanceBrokenDuration, level) + " seconds",
                "",
                "Hitting players with broken stances will reduce",
                "the cooldown by " + getValueString(this::getCooldownReduction, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.VULNERABILITY.getDescription(vulnerabilityStrength)
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getCooldownReduction(int level) {
        return cooldownDecrease;
    }

    public double getStanceBrokenDuration(int level) {
        return stanceBrokenDuration + ((level - 1) * stanceBrokenDurationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRiposte(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (event.getDamager() == null) return;

        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        if (!gamer.isHoldingRightClick()) return;
        LivingEntity ent = event.getDamager();

        int level = getLevel(player);
        if (level > 0) {
            event.setKnockback(false);
            event.setDamage(0);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 2.0f, 1.3f);

            UtilMessage.simpleMessage(player, getClassType().getName(), "You Riposted <green>%s<gray>.", ent);
            UtilMessage.simpleMessage(ent, getClassType().getName(), "<yellow>%s<gray> broke your stance.", player.getName());
            stanceBroken.put(ent, System.currentTimeMillis() + (long) (getStanceBrokenDuration(level) * 1000));
            championsManager.getEffects().addEffect(ent, EffectTypes.VULNERABILITY, vulnerabilityStrength, (long) (getStanceBrokenDuration(level) * 1000));

            active.remove(player.getUniqueId());
            handRaisedTime.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onAttack(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        int level = getLevel(attacker);
        if (level <= 0) return;

        if (stanceBroken.containsKey(event.getDamagee())) {
            this.championsManager.getCooldowns().reduceCooldown(attacker, getName(), getCooldownReduction(level));

        }
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 1.3f);

        Particle.LARGE_SMOKE.builder().location(player.getLocation().add(0, 0.25, 0)).receivers(20).extra(0).spawn();
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = active.iterator();

        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
                if (gamer.isHoldingRightClick() && !handRaisedTime.containsKey(player.getUniqueId())) {
                    handRaisedTime.put(player.getUniqueId(), System.currentTimeMillis());
                    continue;
                }

                if (!gamer.isHoldingRightClick() && handRaisedTime.containsKey(player.getUniqueId())) {
                    failRiposte(player);
                    it.remove();
                    continue;
                }

                int level = getLevel(player);

                if (gamer.isHoldingRightClick() && UtilTime.elapsed(handRaisedTime.getOrDefault(player.getUniqueId(), 0L), (long) getDuration(level) * 1000L)) {
                    failRiposte(player);
                    it.remove();
                }
            } else {
                it.remove();
            }
        }

        Iterator<Map.Entry<LivingEntity, Long>> sbIt = stanceBroken.entrySet().iterator();
        while (sbIt.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = sbIt.next();
            if (System.currentTimeMillis() >= entry.getValue()) {
                sbIt.remove();
            }
        }
    }

    private void failRiposte(Player player) {
        handRaisedTime.remove(player.getUniqueId());
        UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s %d</green>", getName(), getLevel(player));
        player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
        cooldownDecrease = getConfig("cooldownDecrease", 2.0, Double.class);
        vulnerabilityStrength = getConfig("vulnerabilityStrength", 1, Integer.class);
        stanceBrokenDuration = getConfig("stanceBrokenDuration", 2.0, Double.class);
        stanceBrokenDurationIncreasePerLevel = getConfig("stanceBrokenDurationIncreasePerLevel", 1.0, Double.class);
    }
}
