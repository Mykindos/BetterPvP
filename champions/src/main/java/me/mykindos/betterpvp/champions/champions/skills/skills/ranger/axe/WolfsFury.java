package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
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
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Iterator;
import java.util.Map;
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
    private int strengthLevelIncreasePerLevel;
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
                "<effect>Strength " + UtilFormat.getRomanNumeral(getStrengthLevel(level)) + "</effect> for " + getValueString(this::getDuration, level) + " seconds and giving",
                "no knockback on your attacks",
                "",
                "If you miss " + getValueString(this::getMaxMissedSwings, level) + " consecutive swings",
                "Wolfs Fury ends",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.STRENGTH.getDescription(getStrengthLevel(level)),
        };
    }

    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    public double getMaxMissedSwings(int level) {
        return baseMissedSwings + ((level - 1) * missedSwingsIncreasePerLevel);
    }

    public int getStrengthLevel(int level){
        return strengthLevel + ((level - 1) * strengthLevelIncreasePerLevel);
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

    @UpdateEvent
    public void onUpdate() {
        Iterator<Map.Entry<Player, Long>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, Long> entry = iterator.next();
            Player player = entry.getKey();
            if (player == null) {
                iterator.remove();
            } else {
                spawnSkillParticles(player);
                if (entry.getValue() - System.currentTimeMillis() <= 0) {
                    expire(player, true);
                    iterator.remove();
                }
            }
        }
    }

    private void spawnSkillParticles(Player player) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 0.75F);
        new ParticleBuilder(Particle.DUST)
                .location(player.getLocation().add(0, 1, 0))
                .count(1)
                .offset(0.3, 0.6, 0.3)
                .extra(0)
                .receivers(60)
                .data(dustOptions)
                .spawn();
    }

    private boolean expire(Player player, boolean force) {
        if (player == null) {
            return true;
        }

        if ((active.get(player) - System.currentTimeMillis() <= 0) || force || player.isDead()) {
            missedSwings.remove(player);
            deactivate(player);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onMiss(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

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
        championsManager.getEffects().addEffect(player, EffectTypes.STRENGTH, getName(), getStrengthLevel(level), (long) (getDuration(level) * 1000L));
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
        baseDuration = getConfig("baseDuration", 5.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
        baseMissedSwings = getConfig("baseMissedSwings", 2, Integer.class);
        missedSwingsIncreasePerLevel = getConfig("missedSwingsIncreasePerLevel", 1.0, Double.class);
        strengthLevel = getConfig("strengthLevel", 2, Integer.class);
        strengthLevelIncreasePerLevel = getConfig("strengthLevelIncreasePerLevel", 0, Integer.class);
    }
}