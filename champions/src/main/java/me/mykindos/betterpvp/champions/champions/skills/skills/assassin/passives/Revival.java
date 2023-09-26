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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

@Singleton
@BPvPListener
public class Revival extends Skill implements ToggleSkill, CooldownSkill, Listener {
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
                "you will be revived, giving you <effect>Invulnerability</effect> for <stat>2</stat> seconds",
                "and giving you <effect>Regeneration III </effect> and <effect>Strength II</effect> for <val>"+(4 + (level-1))+"</val> seconds",
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

            damagee.setHealth(1);
            damagee.getWorld().playSound(damagee.getLocation(),Sound.ITEM_TOTEM_USE, 2.0F,0.8F);

            damagee.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) (effectDuration + (damagee.getLevel()-1)) * 20, 1));
            damagee.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (effectDuration + (damagee.getLevel()-1)) * 20, 2));

            damagee.setMetadata("RevivalDamageReduction", new FixedMetadataValue(champions, true));

            Bukkit.getScheduler().runTaskLater(champions, () -> {
                if (damagee.hasMetadata("RevivalDamageReduction")) {
                    damagee.removeMetadata("RevivalDamageReduction", champions);
                }
            }, (long) effectDuration * 20);

            active.remove(damagee.getUniqueId());
            Bukkit.getScheduler().runTaskLater(champions, () -> {
                if (damagee.hasMetadata("RevivalDamageReduction")) {
                    damagee.removeMetadata("RevivalDamageReduction", champions);
                }
            }, 40L);
        }

        if (damagee.hasMetadata("RevivalDamageReduction")) {
            event.setDamage(0);
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
        baseDuration = getConfig("baseDuration", 0.5, Double.class);
        effectDuration = getConfig("effectDuration", 4.0, Double.class);
    }
}