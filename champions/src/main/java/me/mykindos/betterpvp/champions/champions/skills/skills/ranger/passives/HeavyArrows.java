package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Singleton
@BPvPListener
public class HeavyArrows extends Skill implements PassiveSkill, EnergySkill, MovementSkill {

    private final Set<Arrow> arrows = new HashSet<>();
    public double energyDecreasePerLevel;
    public double basePushBack;
    public double baseDamage;
    public double damageIncreasePerLevel;
    public double velocityDecreasePercentDecreasePerLevel;
    public double velocityDecreasePercent;

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
                "The arrows you shoot are heavy, being " + getValueString(this::getVelocityDescriptionPercent, level) + "%",
                "slower but dealing " + getValueString(this::getDamage, level) + " extra damage",
                "",
                "For every arrow you shoot you will be",
                "pushed backwards (unless crouching)",
                "",
                "Energy used per shot: "+ getValueString(this::getEnergy, level),
        };
    }

    public double getDamage(int level){
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getVelocityDecreasePercent(int level){
        return velocityDecreasePercent + ((level - 1) * velocityDecreasePercentDecreasePerLevel);
    }

    public double getVelocityDescriptionPercent(int level){
        return (getVelocityDecreasePercent(level) * 100);
    }


    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @UpdateEvent
    public void update() {
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow arrow = it.next();
            if (arrow == null || arrow.isDead() || arrow.isOnGround() || !(arrow.getShooter() instanceof Player)) {
                it.remove();
            } else {
                Location location = arrow.getLocation().add(new Vector(0, 0.25, 0));
                Particle.ENCHANTED_HIT.builder().location(location).receivers(60).extra(0).spawn();
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        int level = getLevel(player);
        if (level > 0) {
            Vector velocity = arrow.getVelocity();
            double reductionFactor = 1 - getVelocityDecreasePercent(level);

            velocity = velocity.multiply(reductionFactor);
            arrow.setVelocity(velocity);

            if (UtilBlock.isInLiquid(player)
                    || championsManager.getEffects().hasEffect(player, EffectTypes.STUN)) {
                return;
            }

            PlayerCanUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerCanUseSkillEvent(player, this));
            if (!skillEvent.isCancelled()) {

                float charge = event.getForce() / 3;
                float scaledEnergy = getEnergy(level) * charge;

                // Ensure the player isn't sneaking before using energy
                if(player.isSneaking()) return;

                if (championsManager.getEnergy().use(player, getName(), scaledEnergy, true)) {
                    arrows.add(arrow);
                    Vector pushback = player.getLocation().getDirection().multiply(-1);
                    pushback.multiply(basePushBack * charge);
                    player.setVelocity(pushback);
                } else if((championsManager.getEnergy().getEnergy(player) * 100) > 5.0){ //allow fully charged shots to activate lower energy cost pushback when low on energy
                    double currEnergy = championsManager.getEnergy().getEnergy(player) * 100;
                    double reducedCharge = ((currEnergy / scaledEnergy) * charge) ;
                    championsManager.getEnergy().use(player, getName(), currEnergy , false);

                    arrows.add(arrow);
                    Vector pushback = player.getLocation().getDirection().multiply(-1);
                    pushback.multiply(basePushBack * reducedCharge);
                    player.setVelocity(pushback);
                }
            }
        }
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            double extraDamage = getDamage(level);
            event.setDamage(event.getDamage() + extraDamage);
            event.addReason("Heavy Arrows");

            arrows.remove(arrow);
        }
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    public void loadSkillConfig(){
        basePushBack = getConfig("basePushBack", 1.5, Double.class);
        energyDecreasePerLevel = getConfig("energyDecreasePerLevel", 10.0, Double.class);
        baseDamage = getConfig("baseDamage", 0.5, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        velocityDecreasePercent = getConfig("velocityDecreasePercent", 25.0, Double.class);
        velocityDecreasePercentDecreasePerLevel = getConfig("velocityDecreasePercentDecreasePerLevel", 0.0, Double.class);
    }
}
