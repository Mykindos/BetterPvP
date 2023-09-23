package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.RecallData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class Usurper extends Skill implements ToggleSkill, CooldownSkill, Listener {

    private final Set<UUID> active = new HashSet<>();

    private double baseDuration;

    @Inject
    public Usurper(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Usurper";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Must be above half health to use",
                "",
                "Reduce yourself to 3 hearts and take double knockback,",
                "But receive <val>"+ (70 + ((level - 1) * 5)) +"</val> reduced damage and speed III for 2 <val>" + (baseDuration + level * 0.5) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public boolean canUse(Player player) {
        if (player.getHealth()<10) {
            UtilMessage.message(player, getClassType().getName(), "You do not have enough health to sacrifice.");
            return false;
        }
        return true;
    }

    @Override
    public void toggle(Player player, int level) {
        if (!active.contains(player.getUniqueId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) ((baseDuration + level) * 20), 2));
            player.setHealth(6);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 1.0F, 1.0F);
            Particle.EXPLOSION_NORMAL.builder().location(player.getLocation()).receivers(30).spawn();
            active.add(player.getUniqueId());


        }
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        if (active.contains(damagee.getUniqueId())) {
            event.setDamage(event.getDamage() * (0.30 - ((damagee.getLevel() - 1) * 5)));
            UtilMessage.message(damager, getClassType().getName(), damagee.getName() + " is using " + getName());
            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5F, 2.0F);
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            active.remove(damager.getUniqueId());
        }

    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        active.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2.5);
    }

}
