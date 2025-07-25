package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class HeavyArrows extends Skill implements PassiveSkill, EnergySkill, MovementSkill {

    private final Set<Arrow> arrows = Collections.newSetFromMap(new WeakHashMap<>());
    public double energyDecreasePerLevel;
    public double basePushBack;

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
                "",
                "For every arrow you shoot you will be",
                "pushed backwards (unless crouching)",
                "",
                "Energy used per shot: "+ getValueString(this::getEnergy, level),
        };
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
            if (arrow == null || arrow.isDead() || !(arrow.getShooter() instanceof Player)) {
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

        if (UtilBlock.isInLiquid(player)
                || championsManager.getEffects().hasEffect(player, EffectTypes.STUN)) {
            return;
        }

        int level = getLevel(player);
        if (level > 0) {
            PlayerCanUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerCanUseSkillEvent(player, this));
            if (!skillEvent.isCancelled()) {

                float charge = event.getForce() / 3;
                float scaledEnergy = getEnergy(level) * charge;

                // Ensure the player isn't sneaking before using energy
                boolean hasEnoughEnergy = !player.isSneaking() && championsManager.getEnergy().use(player, getName(), scaledEnergy, false);

                if (hasEnoughEnergy) {
                    arrows.add(arrow);
                    Vector pushback = player.getLocation().getDirection().multiply(-1);
                    pushback.multiply(basePushBack * charge);
                    player.setVelocity(pushback);
                }
            }
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
        basePushBack = getConfig("basePushBack", 1.0, Double.class);
        energyDecreasePerLevel = getConfig("energyDecreasePerLevel", 2.0, Double.class);
    }
}