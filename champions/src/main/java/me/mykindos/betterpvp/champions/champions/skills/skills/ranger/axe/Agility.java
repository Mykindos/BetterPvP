package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Agility extends Skill implements InteractSkill, CooldownSkill, Listener, BuffSkill, MovementSkill, DefensiveSkill {

    private final HashMap<UUID, Long> active = new HashMap<>();
    private final WeakHashMap<Player, Integer> missedSwingsMap = new WeakHashMap<>();
    @Getter
    private double duration;

    @Getter
    private double damageReduction;
    private double missedSwings;
    private int speedStrength;

    @Inject
    public Agility(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Agility";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sprint with great agility, gaining",
                "<effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> for <val>" + getDuration() + "</val> seconds and ",
                "<val>" + UtilFormat.formatNumber(getDamageReduction() * 100, 0) + "%</val> reduced damage while active",
                "",
                "Agility ends if you interact",
                "",
                "Cooldown: <val>" + getCooldown()
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

    @EventHandler
    public void endOnInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        if (hasSkill(player)) {
            if (active.containsKey(player.getUniqueId())) {
                missedSwingsMap.put(player, missedSwingsMap.getOrDefault(player, 0) + 1);
                if (missedSwingsMap.get(player) >= getMaxMissedSwings()) {
                    deactivate(player);
                    active.remove(player.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (active.containsKey(damagee.getUniqueId())) {
            event.setDamage(event.getDamage() * (1 - getDamageReduction()));
        }

        if (!(event.getDamager() instanceof Player damager)) return;
        if (!active.containsKey(damager.getUniqueId())) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            missedSwingsMap.put(damager, 0);
        }
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<Map.Entry<UUID, Long>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
            } else {
                spawnSkillParticles(player);
                if (entry.getValue() - System.currentTimeMillis() <= 0) {
                    deactivate(player);
                    iterator.remove();
                }
            }
        }
    }

    private void spawnSkillParticles(Player player) {
        Location loc = player.getLocation();

        Random random = UtilMath.RANDOM;
        double x = loc.getX() + (random.nextDouble() - 0.5) * 0.5;
        double y = loc.getY() + (1 + (random.nextDouble() - 0.5) * 0.9);
        double z = loc.getZ() + (random.nextDouble() - 0.5) * 0.5;
        Location particleLoc = new Location(loc.getWorld(), x, y, z);
        new ParticleBuilder(Particle.EFFECT)
                .location(particleLoc)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }


    @Override
    public void activate(Player player) {
        if (!active.containsKey(player.getUniqueId())) {
            championsManager.getEffects().addEffect(player, EffectTypes.SPEED, getName(), speedStrength, (long) (getDuration() * 1000));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 0.5F);
            active.put(player.getUniqueId(), (long) (System.currentTimeMillis() + (getDuration() * 1000L)));
        }
    }

    public void deactivate(Player player) {
        UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s</green> has ended.", getName()));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 0.01F);
        championsManager.getEffects().removeEffect(player, EffectTypes.SPEED, getName());
        missedSwingsMap.remove(player);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 3.0, Double.class);
        damageReduction = getConfig("damageReduction", 0.60, Double.class);
        speedStrength = getConfig("speedStrength", 3, Integer.class);
        missedSwings = getConfig("missedSwings", 1.0, Double.class);
    }
}