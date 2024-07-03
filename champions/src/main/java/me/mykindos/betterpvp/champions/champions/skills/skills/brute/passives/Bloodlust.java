package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Bloodlust extends Skill implements PassiveSkill, BuffSkill, HealthSkill {

    private final DamageLogManager damageLogManager;

    private final WeakHashMap<Player, Long> time = new WeakHashMap<>();
    private final WeakHashMap<Player, Integer> str = new WeakHashMap<>();

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int maxStacks;

    private double health;

    @Inject
    public Bloodlust(Champions champions, ChampionsManager championsManager, DamageLogManager damageLogManager) {
        super(champions, championsManager);
        this.damageLogManager = damageLogManager;
    }

    @Override
    public String getName() {
        return "Bloodlust";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "When you kill an enemy, you go into a Bloodlust,",
                "which heals you for " + getValueString(this::getHealth, level) + " health,",
                "and you receive <effect>Speed I</effect>, and <effect>Strength I</effect> for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Bloodlust can stack up to " + getValueString(this::getMaxStacks, level) + " times",
                "boosting the level of <effect>Speed</effect> and <effect>Strength</effect> by 1",
                "",
                EffectTypes.STRENGTH.getGenericDescription()
        };
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level - 1));
    }

    public double getHealth(int level) {
        return health;
    }

    public int getMaxStacks(int level) {
        return maxStacks;
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if(event.getEntity().hasMetadata("PlayerSpawned")) return;

        DamageLog lastDamager = damageLogManager.getLastDamager(event.getEntity());
        if (lastDamager == null) return;
        if (!(lastDamager.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            int tempStr = 0;
            if (str.containsKey(player)) {
                tempStr = str.get(player) + 1;
            }
            tempStr = Math.min(tempStr, maxStacks);
            str.put(player, tempStr);
            time.put(player, (long) (System.currentTimeMillis() + getDuration(level) * 1000));

            championsManager.getEffects().addEffect(player, player, EffectTypes.STRENGTH, getName(), tempStr, (long) (getDuration(level) * 1000L), true);
            championsManager.getEffects().addEffect(player, player, EffectTypes.SPEED, getName(), tempStr, (long) (getDuration(level) * 1000), true);
            UtilPlayer.health(player, health);
            UtilMessage.simpleMessage(player, getClassType().getName(), "You entered bloodlust at level: <alt2>" + (Math.min(tempStr + 1, maxStacks)) + "</alt2>.");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 2.0F, 0.6F);
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
            UtilMessage.simpleMessage(player, getClassType().getName(), "Your bloodlust has ended at level: <alt2>" + (Math.min(tempStr + 1, maxStacks)) + "</alt2>.");
            time.remove(player);
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 5.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        maxStacks = getConfig("maxStacks", 3, Integer.class);
        health = getConfig("health", 4.0, Double.class);
    }
}
