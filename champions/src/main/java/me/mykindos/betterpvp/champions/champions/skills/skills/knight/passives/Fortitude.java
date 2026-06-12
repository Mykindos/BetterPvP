package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Fortitude extends Skill implements PassiveSkill, Listener, DefensiveSkill, HealthSkill {
    private final WeakHashMap<Player, Double> health = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> last = new WeakHashMap<>();
    private double healRate;
    private double baseHeal;
    private double healIncreasePerLevel;
    private double healInterval;

    @Inject
    public Fortitude(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fortitude";
    }

    @Override
    public Component[] getDescription(int level) {
        Component maxHeal = getValueComponent(this::getMaxHeal, level);
        Component healRate = getValueComponent(this::getHealRate, level);
        Component healInterval = getValueComponent(this::getHealInterval, level);
        return Translations.componentLines(
                "champions.skill.knight.fortitude.description",
                maxHeal,
                healRate,
                healInterval
        );
    }

    public double getMaxHeal(int level) {
        return baseHeal + ((level -1) * healIncreasePerLevel);
    }
    public double getHealRate(int level) {
        return healRate;
    }

    public double getHealInterval(int level) {
        return healInterval;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHit(DamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;
        int level = getLevel(player);
        if (level <= 0) return;

        health.put(player, Math.min(getMaxHeal(level), event.getDamage()));
        last.put(player, System.currentTimeMillis());
    }

    @UpdateEvent(delay = 250)
    public void update() {

        HashSet<Player> remove = new HashSet<>();
        for (Player player : health.keySet()) {
            if(!player.isOnline()) {
                remove.add(player);
                continue;
            }

            if (UtilTime.elapsed(last.get(player), (long) (healInterval * 1000))) {
                health.put(player, health.get(player) - healRate);
                last.put(player, System.currentTimeMillis());

                boolean hasAntiHeal = championsManager.getEffects().hasEffect(player, EffectTypes.ANTI_HEAL);
                if (health.get(player) <= 0 || hasAntiHeal) {
                    remove.add(player);
                }

                if (!hasAntiHeal) {
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1, 0.2, 0.2, 0.2, 0);
                    double actualHeal = UtilEntity.health(player, healRate);
                    championsManager.getClientManager().search().online(player).getStatContainer().incrementStat(ClientStat.HEAL_FORTITUDE, actualHeal);
                }
            }
        }

        for (Player cur : remove) {
            health.remove(cur);
            last.remove(cur);
        }
    }
    public void loadSkillConfig() {
        healRate = getConfig("healRate", 2.5, Double.class);
        baseHeal = getConfig("baseHeal", 7.5, Double.class);
        healIncreasePerLevel = getConfig("healIncreasePerLevel", 2.5, Double.class);
        healInterval = getConfig("healInterval", 1.5, Double.class);
    }
}
