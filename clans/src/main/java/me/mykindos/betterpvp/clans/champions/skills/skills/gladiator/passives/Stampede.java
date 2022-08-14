package me.mykindos.betterpvp.clans.champions.skills.skills.gladiator.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Stampede extends Skill implements PassiveSkill {

    private final WeakHashMap<Player, Long> sprintTime = new WeakHashMap<>();
    private final WeakHashMap<Player, Integer> sprintStr = new WeakHashMap<>();

    private double durationPerStack;

    @Inject
    public Stampede(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Stampede";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You slowly build up speed as you",
                "sprint. You gain a level of Speed",
                "for every " + ChatColor.GREEN + (7 - level) + ChatColor.GRAY + " seconds, up to a max",
                "of Speed II.",
                "",
                "Attacking during stampede deals",
                "2 bonus damage per speed level."};
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @UpdateEvent(delay = 250)
    public void updateSpeed() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!sprintTime.containsKey(player)) continue;
            int level = getLevel(player);
            if (level > 0) {
                if (!player.isSprinting()) {
                    sprintTime.remove(player);
                    sprintStr.remove(player);
                    player.removePotionEffect(PotionEffectType.SPEED);
                } else {
                    long time = sprintTime.get(player);
                    int str = sprintStr.get(player);
                    if (str > 0) {
                        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                            player.removePotionEffect(PotionEffectType.SPEED);
                        }

                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, str - 1));
                    }
                    if (UtilTime.elapsed(time, (long) ((durationPerStack - level) * 1000L))) {
                        sprintTime.put(player, System.currentTimeMillis());
                        if (str < 2) {
                            sprintStr.put(player, str + 1);

                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 2.0F, 0.2F * str + 1.0F);
                        }
                    }
                }
            } else if (player.isSprinting()) {
                if (!sprintTime.containsKey(player)) {
                    this.sprintTime.put(player, System.currentTimeMillis());
                    this.sprintStr.put(player, 0);
                }
            }
        }


    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        int str = sprintStr.getOrDefault(damager, 0);
        if (str <= 0) return;

        sprintTime.remove(damager);
        sprintStr.remove(damager);
        damager.removePotionEffect(PotionEffectType.SPEED);

        event.setKnockback(false);
        UtilVelocity.velocity(event.getDamagee(), UtilVelocity.getTrajectory2d(damager, event.getDamagee()), 2.0D, true, 0.0D, 0.4D, 1.0D, true);
        event.setDamage(event.getDamage() + str);
    }

    @Override
    public void loadSkillConfig() {
        durationPerStack = getConfig("durationPerStack", 8.0, Double.class);
    }


}
