package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
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

    @Getter
    private double healRate;
    @Getter
    private double maxHeal;
    @Getter
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
    public String[] getDescription() {
        return new String[]{
                "After taking damage, you regenerate",
                "up to <val>" + UtilFormat.formatNumber(getHeal()) + "</val> of the health you lost.",
                "",
                "You restore health at a rate of",
                "<val>" + UtilFormat.formatNumber(getHealRate()) + "</val> health per <val>" + UtilFormat.formatNumber(getHealInterval()) + "</val> seconds.",
                "",
                "This does not stack, and is reset if",
                "you are hit again."
        };
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
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;
        if (hasSkill(player)) {
            health.put(player, Math.min(getMaxHeal(), event.getDamage()));
            last.put(player, System.currentTimeMillis());
        }
    }

    @UpdateEvent(delay = 250)
    public void update() {

        HashSet<Player> remove = new HashSet<>();
        for (Player player : health.keySet()) {
            if (UtilTime.elapsed(last.get(player), (long) (getHealInterval() * 1000))) {
                health.put(player, health.get(player) - getHealRate());
                last.put(player, System.currentTimeMillis());

                boolean hasAntiHeal = championsManager.getEffects().hasEffect(player, EffectTypes.ANTI_HEAL);
                if (health.get(player) <= 0 || hasAntiHeal) {
                    remove.add(player);
                }

                if (!hasAntiHeal) {
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1, 0.2, 0.2, 0.2, 0);
                    UtilPlayer.health(player, healRate);
                }
            }
        }

        for (Player cur : remove) {
            health.remove(cur);
            last.remove(cur);
        }
    }

    public void loadSkillConfig() {
        healRate = getConfig("healRate", 1.0, Double.class);
        maxHeal = getConfig("maxHeal", 3.0, Double.class);
        healInterval = getConfig("healInterval", 1.5, Double.class);
    }
}
