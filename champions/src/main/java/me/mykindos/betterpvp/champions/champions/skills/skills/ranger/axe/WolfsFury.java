package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
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
import org.bukkit.event.EventPriority;
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
    private final WeakHashMap<Player, Integer> missedSwingsMap = new WeakHashMap<>();

    @Getter
    private int strengthLevel;
    @Getter
    private double duration;
    private int missedSwings;

    @Inject
    public WolfsFury(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wolfs Fury";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Summon the power of the wolf, gaining",
                "<effect>Strength " + UtilFormat.getRomanNumeral(getStrengthLevel()) + "</effect> for <val>" + getDuration() + "</val> seconds and giving",
                "no knockback on your attacks",
                "",
                "If you miss <val>" + getMaxMissedSwings() + "</val> consecutive swings",
                "Wolfs Fury ends",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.STRENGTH.getDescription(getStrengthLevel()),
        };
    }

    public double getMaxMissedSwings() {
        return missedSwings;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @EventHandler()
    public void onDamage(CustomDamageEvent e) {
        if (e.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!active.containsKey(damager)) return;

        if (hasSkill(damager)) {
            e.setKnockback(false);
            e.addReason(getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamage(PreCustomDamageEvent event) {
        final CustomDamageEvent cde = event.getCustomDamageEvent();
        if (cde.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (!active.containsKey(damager)) return;

        missedSwingsMap.put(damager, 0);
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
            missedSwingsMap.remove(player);
            deactivate(player);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onMiss(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!hasSkill(player)) {
            return;
        }

        if (!active.containsKey(player)) {
            return;
        }

        missedSwingsMap.put(player, missedSwingsMap.getOrDefault(player, 0) + 1);
        if (missedSwingsMap.get(player) >= getMaxMissedSwings()) {
            expire(player, true);
            active.remove(player);
        }
    }

    @Override
    public void activate(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 2f, 1.2f);
        active.put(player, (long) (System.currentTimeMillis() + (getDuration() * 1000L)));
        championsManager.getEffects().addEffect(player, EffectTypes.STRENGTH, getName(), getStrengthLevel(), (long) (getDuration() * 1000L));
    }

    public void deactivate(Player player) {
        UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s</green> has ended.", getName()));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 2f, 1);
        championsManager.getEffects().removeEffect(player, EffectTypes.STRENGTH, getName());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 5.0, Double.class);
        missedSwings = getConfig("missedSwings", 2, Integer.class);
        strengthLevel = getConfig("strengthLevel", 2, Integer.class);
    }
}