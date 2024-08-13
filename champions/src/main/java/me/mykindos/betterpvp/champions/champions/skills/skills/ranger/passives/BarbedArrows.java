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
public class BarbedArrows extends Skill implements PassiveSkill, DamageSkill {
    private final WeakHashMap<UUID, Double> data = new WeakHashMap<>();
    private final Map<UUID, Long> arrowHitTime = new HashMap<>();
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double damageResetTime;

    @Inject
    public BarbedArrows(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Barbed Arrows";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hitting an arrow will stick a barb into the target",
                "melee hits on that target will rip the barb out, dealing",
                getValueString(this::getDamage, level) + " extra damage",
                "",
                "The barb will fall out after " + getValueString(this::getDamageResetTime, level) + " seconds"
        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
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
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            data.put(player.getUniqueId(), getDamage(level));
            arrowHitTime.put(player.getUniqueId(), System.currentTimeMillis());
        }

    }

    @EventHandler
    public void onHit(CustomDamageEvent event){
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        int level = getLevel(player);

        if (level > 0) {
            if (!data.containsKey(player.getUniqueId())) {
                return;
            }

            double extraDamage = data.get(player.getUniqueId());
            event.addReason("Barb");
            event.setDamage(event.getDamage() + extraDamage);

            UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> dealt <alt2>%s</alt2> extra damage", getName(), extraDamage);
            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.0f);
            data.remove(player.getUniqueId());
        }
    }

    @UpdateEvent(delay = 100)
    public void updateBarbedData() {
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
                    UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> has fallen off.", getName());
                }
                arrowHitTime.remove(uuid);
                return true;
            }
            return false;
        });
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.75, Double.class);
        damageResetTime = getConfig("damageResetTime", 2.0, Double.class);
    }
}
