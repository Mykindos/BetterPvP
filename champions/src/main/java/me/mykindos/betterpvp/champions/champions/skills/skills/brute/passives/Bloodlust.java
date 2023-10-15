package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
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

    private int radius;

    private int maxStacks;

    @Inject
    public Bloodlust(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bloodlust";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "When an enemy dies within <stat>" + radius + "</stat> blocks,",
                "you go into a Bloodlust, receiving",
                "<effect>Speed I</effect> and <effect>Strength I</effect> for <val>" + (duration + level) + "</val> seconds.",
                "",
                "Bloodlust can stack up to <stat>" + maxStacks + "</stat> times,",
                "boosting the level of <effect>Speed</effect> and <effect>Strength</effect>"};
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        for (Player target : UtilPlayer.getNearbyEnemies(event.getEntity(), event.getEntity().getLocation(), radius)) {
            int level = getLevel(target);
            if (level > 0) {
                int tempStr = 0;
                if (str.containsKey(target)) {
                    tempStr = str.get(target) + 1;
                }
                tempStr = Math.min(tempStr, maxStacks);
                str.put(target, tempStr);
                time.put(target, (long) (System.currentTimeMillis() + duration * 1000));
                if (target.hasPotionEffect(PotionEffectType.SPEED)) {
                    target.removePotionEffect(PotionEffectType.SPEED);
                }
                championsManager.getEffects().addEffect(target, EffectType.STRENGTH, tempStr, (long) ((duration + level) * 1000L));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) ((duration + level) * 20), tempStr));
                UtilMessage.simpleMessage(target, getClassType().getName(), "You entered bloodlust at level: <alt2>" + tempStr + "</alt2>.");
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
            UtilMessage.simpleMessage(player, getClassType().getName(), "Your bloodlust has ended at level: <alt2>" + tempStr + "</alt2>.");
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
        radius = getConfig("radius", 15, Integer.class);
        maxStacks = getConfig("maxStacks", 3, Integer.class);
    }
}
