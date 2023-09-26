package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

@Singleton
@BPvPListener
public class Revival extends Skill implements ToggleSkill, CooldownSkill, Listener {
    public double percentHealthRecovered;
    public double baseDuration;
    public double effectDuration;

    private final Set<UUID> active = new HashSet<>();
    private final Map<UUID, Integer> particleTasks = new HashMap<>();

    @Inject
    public Revival(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Revival";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "If you die within the next <val>"+ (baseDuration + ((level-1) * 0.5)) +"</val> seconds,",
                "you will be revived, setting your health to <stat>" + percentHealthRecovered + "%",
                "of your maximum HP, and receiving <effect>Regeneration I",
                "and <effect>Strength I</effect> for <stat>5<stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public void toggle(Player player, int level) {
        if (!active.contains(player.getUniqueId())) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
            active.add(player.getUniqueId());

            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(champions, () -> {
                player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.GOLD_BLOCK);
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
            }, (long) ((baseDuration + ((level-1) * 0.5)) * 20));
        }

    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;

        if (active.contains(damagee.getUniqueId()) && damagee.getHealth() <= event.getDamage()) {
            event.setCancelled(true);

            damagee.setHealth(damagee.getMaxHealth() * (percentHealthRecovered / 100));
            damagee.getWorld().playSound(damagee.getLocation(),Sound.ITEM_TOTEM_USE, 2.0F,0.8F);

            damagee.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) effectDuration * 20, 0));
            damagee.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) effectDuration * 20, 0));

            active.remove(damagee.getUniqueId());
            if (particleTasks.containsKey(damagee.getUniqueId())) {
                Bukkit.getScheduler().cancelTask(particleTasks.get(damagee.getUniqueId()));
                particleTasks.remove(damagee.getUniqueId());
            }
        }
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
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2.5);
    }
    public void loadSkillConfig(){
        percentHealthRecovered = getConfig("percentHealthRecovered", 25.0, Double.class);
        baseDuration = getConfig("baseDuration", 0.5, Double.class);
        effectDuration = getConfig("effectDuration", 5.0, Double.class);
    }
}