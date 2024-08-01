package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Kinetics extends Skill implements PassiveSkill, MovementSkill {

    private final WeakHashMap<UUID, Double> data = new WeakHashMap<>();
    private final Map<UUID, Long> arrowHitTime = new HashMap<>();
    public double damageResetTime;
    public int storedVelocityCount;
    public int storedVelocityCountIncreasePerLevel;

    @Inject
    public Kinetics(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Kinetics";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Your arrows no longer deal knockback, and instead",
                "the knockback is stored for up to " + getValueString(this::getDamageResetTime, level) + " seconds",
                "",
                "By pressing shift you can activate this stored velocity on yourself",
                "",
                "Can store up to " + getValueString(this::getStoredVelocityCount, level) + " arrows worth of knockback"
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    public double getDamageResetTime(int level) {
        return damageResetTime;
    }

    public double getStoredVelocityCount(int level) {
        return storedVelocityCount + ((level - 1) * storedVelocityCountIncreasePerLevel);
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            double charge = data.getOrDefault(player.getUniqueId(), 0.0);
            charge ++;

            data.put(player.getUniqueId(), charge);

            if (data.get(player.getUniqueId()) == 3){
                UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> has reached maximum charge.", getName());
            }

            arrowHitTime.put(player.getUniqueId(), System.currentTimeMillis());
            event.setKnockback(false);
        }

    }

    @UpdateEvent(delay = 100)
    public void updateKineticsData() {
        long currentTime = System.currentTimeMillis();

        data.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            Long lastTimeHit = arrowHitTime.get(uuid);

            if (lastTimeHit == null) {
                return false;
            }

            Player player = Bukkit.getPlayer(uuid);
            int level = getLevel(player);
            if (currentTime - lastTimeHit > getDamageResetTime(level) * 1000) {
                if (player != null) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> stored velocity has dissipated.", getName());
                }
                arrowHitTime.remove(uuid);
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void doDash(PlayerToggleSneakEvent event){
        Player player = event.getPlayer();
        int level = getLevel(player);

        if(player.isSneaking()) return;
        if(data.get(player.getUniqueId()) == null){
            return;
        }

        if (data.get(player.getUniqueId()) > 0) {
            Vector vec = player.getLocation().getDirection();
            double multiplier = Math.min(data.get(player.getUniqueId()), getStoredVelocityCount(level));
            VelocityData velocityData = new VelocityData(vec, 0.5 + (0.25 * multiplier), false, 0.0D, (0.15D * multiplier), (0.2D * multiplier), false);
            UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);
            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_LAND, 2.0f, 1.0f);
            data.remove(player.getUniqueId());

            new ParticleBuilder(Particle.GUST_EMITTER_SMALL)
                    .location(player.getLocation().add(0, 1, 0))
                    .count(1)
                    .offset(0.0, 0.0, 0.0)
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    public void loadSkillConfig(){
        damageResetTime = getConfig("damageResetTime", 5.0, Double.class);
        storedVelocityCount = getConfig("storedVelocityCount", 1, Integer.class);
        storedVelocityCountIncreasePerLevel = getConfig(" storedVelocityCountIncreasePerLevel", 1, Integer.class);
    }
}
