package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.types.BowChargeSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@CustomLog
public class HeavyArrows extends BowChargeSkill implements DamageSkill, PassiveSkill, EnergySkill, MovementSkill {
    private final Map<Arrow, ChargeData> arrows = new WeakHashMap<>();

    private double baseMaxDamage;
    private double maxDamageIncreasePerLevel;
    private double baseArrowSpeed;
    private double arrowSpeedIncreasePerLevel;
    private double baseMaxArrowSpeed;
    private double maxArrowSpeedIncreasePerLevel;
    private double basePushBack;
    private double pushBackIncreasePerLevel;

    @Inject
    public HeavyArrows(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Heavy Arrows";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "The arrows you shoot are heavy",
                "Travelling at " + getValueString(this::getArrowSpeed, level, 100, "%", 0) + " of normal speed",
                "",
                "Draw back your bow ",
                "to charge " + getValueString(this::getChargePerSecond, level, 100, "%", 0) + " per second",
                "Draining " + getValueString(this::getEnergy, level) + " energy per second to charge",
                "",
                "The more charge, the faster ",
                "your arrow will travel up to " + getValueString(this::getMaxArrowSpeed, level, 100, "%", 0),
                "of normal speed at max charge, ",
                "increasing extra damage by " + getValueString(this::getMaxDamage, level),
                "and increasing pushback received",
        };
    }
    public double getMaxDamage(int level) {
        return baseMaxDamage + (maxDamageIncreasePerLevel * (level - 1));
    }
    public double getArrowSpeed(int level) {
        return baseArrowSpeed + (arrowSpeedIncreasePerLevel * (level - 1));
    }
    public double getMaxArrowSpeed(int level) {
        return baseMaxArrowSpeed + (maxArrowSpeedIncreasePerLevel * (level - 1));
    }
    public double getPushBack(int level) {
        return basePushBack + (pushBackIncreasePerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @UpdateEvent
    public void update() {
        Iterator<Arrow> it = arrows.keySet().iterator();
        while (it.hasNext()) {
            Arrow arrow = it.next();
            if (arrow == null || arrow.isDead() || !(arrow.getShooter() instanceof Player)) {
                it.remove();
            } else {
                Location location = arrow.getLocation().add(new Vector(0, 0.25, 0));
                Particle.ENCHANTED_HIT.builder().location(location).receivers(60).extra(0).spawn();
            }
        }
    }

    @Override
    public TickBehavior getTickBehavior(Player player, ChargeData chargeData, int level) {
        if (chargeData.getCharge() >= 1.0) {
            return TickBehavior.PAUSE;
        }
        if (championsManager.getEnergy().use(player, getName(), getEnergy(level)/20, false)) {
            return TickBehavior.TICK;
        }
        return TickBehavior.PAUSE;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!(event.getDamagingEntity() instanceof Arrow arrow)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!arrows.containsKey(arrow)) return;
        int level = getLevel(player);
        if (level <= 0) return;
        double damageIncrease = getMaxDamage(level) * arrows.get(arrow).getCharge();
        log.info("Arrow damage increase {}", damageIncrease).submit();
        event.addModifier(new SkillDamageModifier.Flat(this, damageIncrease));
        arrows.remove(arrow);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        int level = getLevel(player);
        if (level > 0) {
            PlayerCanUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerCanUseSkillEvent(player, this));
            if (!skillEvent.isCancelled() && charging.containsKey(player)) {
                ChargeData chargeData = charging.remove(player);
                active.remove(player.getUniqueId());
                double velocity = (getMaxArrowSpeed(level) - getArrowSpeed(level)) * chargeData.getCharge() + getArrowSpeed(level);
                arrow.setVelocity(arrow.getVelocity().multiply(velocity));
                arrows.put(arrow, chargeData);

                Vector pushback = player.getLocation().getDirection().multiply(-1);
                pushback.multiply(getPushBack(level) * chargeData.getCharge());
                player.setVelocity(pushback);
            } else {
                //always reduce arrow speed
                arrow.setVelocity(arrow.getVelocity().multiply(getArrowSpeed(level)));
            }
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    public void loadSkillConfig(){
        baseMaxDamage = getConfig("baseMaxDamage", 2.0, Number.class).doubleValue();
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 0.5, Number.class).doubleValue();
        baseArrowSpeed = getConfig("baseArrowSpeed", 0.80, Number.class).doubleValue();
        arrowSpeedIncreasePerLevel = getConfig("arrowSpeedIncreasePerLevel", 0.0, Number.class).doubleValue();
        baseMaxArrowSpeed = getConfig("baseMaxArrowSpeed", 1.0, Number.class).doubleValue();
        maxArrowSpeedIncreasePerLevel = getConfig("maxArrowSpeedIncreasePerLevel", 0.0, Number.class).doubleValue();
        basePushBack = getConfig("basePushBack", 1.0, Double.class);
        pushBackIncreasePerLevel = getConfig("pushBackIncreasePerLevel", 0.0, Double.class);
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - (energyDecreasePerLevel * (level - 1)));
    }
}