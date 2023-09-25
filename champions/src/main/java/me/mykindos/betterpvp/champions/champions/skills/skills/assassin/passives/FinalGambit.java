package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;
import java.util.HashMap;

@Singleton
@BPvPListener
public class FinalGambit extends Skill implements ToggleSkill, CooldownSkill, Listener {

    private final Set<UUID> active = new HashSet<>();
    private final Map<UUID, Integer> particleTasks = new HashMap<>();

    private double baseDuration;

    @Inject
    public FinalGambit(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Final Gambit";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop Sword / Axe to Activate",
                "",
                "Sacrifice half of your health in exchange",
                "for <stat>90%</stat> damage reduction and <effect>Speed III",
                "that lasts for <val>" + (baseDuration + (level-1) * 0.5) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public void toggle(Player player, int level) {
        if (!active.contains(player.getUniqueId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) ((baseDuration + (level * 0.5)) * 20), 2));
            player.setHealth(player.getHealth()/2.0);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
            active.add(player.getUniqueId());

            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(champions, () -> {
                player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            }, 0L, 1L);

            particleTasks.put(player.getUniqueId(), taskId);

            Bukkit.getScheduler().runTaskLater(champions, () -> {
                active.remove(player.getUniqueId());

                if (particleTasks.containsKey(player.getUniqueId())) {
                    Bukkit.getScheduler().cancelTask(particleTasks.get(player.getUniqueId()));
                    particleTasks.remove(player.getUniqueId());
                }
            }, (long) ((baseDuration + (level * 0.5)) * 20));

            Bukkit.getScheduler().runTaskLater(champions, () -> {
                active.remove(player.getUniqueId());
            }, (long) ((baseDuration + (level * 0.5)) * 20));
        }
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        if (active.contains(damagee.getUniqueId())) {
            event.setDamage(0.1);
            UtilMessage.message(damager, getClassType().getName(), damagee.getName() + " is using " + getName());
            damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.5F, 2.0F);
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
