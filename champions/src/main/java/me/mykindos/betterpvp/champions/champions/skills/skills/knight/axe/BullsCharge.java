package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import java.util.HashMap;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class BullsCharge extends Skill implements Listener, InteractSkill, CooldownSkill {

    private final HashMap<UUID, Long> running = new HashMap<>();

    private double slowDuration;

    @Inject
    public BullsCharge(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bulls Charge";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Enter a rage, gaining <effect>Speed II</effect>",
                "and giving <effect>Slowness III</effect> to anything you hit for <stat>" + slowDuration + "</stat> seconds",
                "",
                "While charging, you take no knockback",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public void activate(Player player, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2));
        UtilSound.playSound(player.getWorld(), player, Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, 49);
        running.put(player.getUniqueId(), System.currentTimeMillis() + 4000L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;

        if (event.getDamagee() instanceof Player player) {
            if (running.containsKey(player.getUniqueId())) {
                event.setKnockback(false);
            }
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        if (event.getDamager() instanceof Player damager) {

            final LivingEntity damagee = event.getDamagee();

            if (running.containsKey(damager.getUniqueId())) {
                if (System.currentTimeMillis() >= running.get(damager.getUniqueId())) {
                    running.remove(damager.getUniqueId());
                    return;
                }

                event.setKnockback(false);

                damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) slowDuration * 20, 2));
                damager.removePotionEffect(PotionEffectType.SPEED);

                damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0.0F);
                damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5F, 0.5F);

                if (event.getDamagee() instanceof Player damaged) {
                    UtilMessage.simpleMessage(damaged, getClassType().getName(), "<yellow>" + damager.getName() + "</yellow> hit you with <green>" + getName() + "</green>.");
                    UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <yellow>" + damaged.getName() + "</yellow> with <green>" + getName() + "</green>.");
          
                    running.remove(damager.getUniqueId());
                    return;
                }

                running.remove(damager.getUniqueId());
            }
        }
    }

    @UpdateEvent(delay = 1000)
    public void onUpdate() {
        running.entrySet().removeIf(entry -> entry.getValue() - System.currentTimeMillis() <= 0);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1);
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    public void loadSkillConfig() {
        slowDuration = getConfig("slowDuration", 3.0, Double.class);
    }

}
