package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Gambit extends Skill implements PassiveSkill, DamageSkill {
    private final WeakHashMap<UUID, Double> data = new WeakHashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double damageResetTime;
    private double maxDamage;
    private double maxDamageIncreasePerLevel;

    @Inject
    public Gambit(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Gambit";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Each melee hit you land will increase your next",
                "arrows damage by " + getValueString(this::getDamage, level) + " up to a maximum of " + getValueString(this::getMaxDamage, level),
                "",
                "Stored damage resets after " + getValueString(this::getDamageResetTime, level) + " seconds"
        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    public double getMaxDamage(int level) {
        return maxDamage + ((level - 1) * maxDamageIncreasePerLevel);
    }

    public double getDamageResetTime(int level) {
        return damageResetTime;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if (level > 0) {
            if (!data.containsKey(damager.getUniqueId())) {
                return;
            }

            double extraDamage = Math.min(data.get(damager.getUniqueId()), getMaxDamage(level));
            event.addReason(getName());
            event.setDamage(event.getDamage() + extraDamage);

            UtilMessage.simpleMessage(damager, getClassType().getName(), "<alt>%s</alt> dealt <alt2>%s</alt2> extra damage", getName(), extraDamage);
            damager.playSound(damager.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.7f);
            data.remove(damager.getUniqueId());
        }
    }

    @EventHandler
    public void onHit(CustomDamageEvent event){
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        int level = getLevel(player);

        if (level > 0) {
            if(!data.containsKey(player.getUniqueId())){
                data.put(player.getUniqueId(), 0.0);
            }

            double damage = data.getOrDefault(player.getUniqueId(), 0.0);
            damage += getDamage(level);

            data.put(player.getUniqueId(), damage);
            lastHitTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @UpdateEvent(delay = 100)
    public void updateGambitData() {
        long currentTime = System.currentTimeMillis();

        data.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            Long lastTimeHit = lastHitTime.get(uuid);

            if (lastTimeHit == null) {
                return false;
            }

            Player player = Bukkit.getPlayer(uuid);
            int level = getLevel(player);
            if (currentTime - lastTimeHit > getDamageResetTime(level) * 1000) {
                if (player != null) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> damage has reset.", getName());
                }
                lastHitTime.remove(uuid);
                return true;
            }
            return false;
        });
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        maxDamage = getConfig("maxDamage", 2.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 2.0, Double.class);
        damageResetTime = getConfig("damageResetTime", 4.0, Double.class);
    }
}
