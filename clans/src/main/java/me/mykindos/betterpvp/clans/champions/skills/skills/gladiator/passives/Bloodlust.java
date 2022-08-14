package me.mykindos.betterpvp.clans.champions.skills.skills.gladiator.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Bloodlust extends Skill implements PassiveSkill {

    private final WeakHashMap<Player, Long> time = new WeakHashMap<>();
    private final WeakHashMap<Player, Integer> str = new WeakHashMap<>();

    private double duration;

    @Inject
    public Bloodlust(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Bloodlust";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "When an enemy dies within 15 blocks,",
                "you go into a Bloodlust, receiving",
                "Speed 1 and Strength 1 for " + ChatColor.GREEN + (duration + level) + ChatColor.GRAY + " seconds.",
                "",
                "Bloodlust can stack up to 3 times,",
                "boosting the level of Speed and Strength."};
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        for (Player target : UtilPlayer.getNearbyEnemies(event.getEntity(), event.getEntity().getLocation(), 15)) {
            int level = getLevel(target);
            if (level > 0) {
                int tempStr = 0;
                if (str.containsKey(target)) {
                    tempStr = str.get(target) + 1;
                }
                tempStr = Math.min(tempStr, 3);
                str.put(target, tempStr);
                time.put(target, (long) (System.currentTimeMillis() + duration * 1000));
                if (target.hasPotionEffect(PotionEffectType.SPEED)) {
                    target.removePotionEffect(PotionEffectType.SPEED);
                }
                championsManager.getEffects().addEffect(target, EffectType.STRENGTH, tempStr, (long) ((duration + level) * 1000L));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) ((duration + level) * 20), tempStr));
                UtilMessage.message(target, getClassType().getName(), "You entered bloodlust at level: " + ChatColor.YELLOW + tempStr + ChatColor.GRAY + ".");
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 2.0F, 0.6F);
            }

        }
    }

    @UpdateEvent(delay = 500)
    public void update() {
        for (Player cur : Bukkit.getOnlinePlayers()) {
            expire(cur);
        }
    }

    public void expire(Player player) {
        if (!time.containsKey(player)) return;

        if (System.currentTimeMillis() > time.get(player)) {
            int tempStr = str.get(player);
            str.remove(player);
            UtilMessage.message(player, getClassType().getName(), "Your bloodlust has ended at level: " + ChatColor.YELLOW + tempStr + ChatColor.GRAY + ".");
            time.remove(player);
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig(){
        duration = getConfig("duration", 5.0, Double.class);
    }


}
