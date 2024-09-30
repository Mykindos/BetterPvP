package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.DaggerData;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.DaggerDataManager;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

@Singleton
@BPvPListener
public class Aerobatics extends Skill implements PassiveSkill, DamageSkill {

    private double damageIncreasePerLevel;
    private double damage;

    @Inject
    public Aerobatics(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Aerobatics";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "While in the air you deal " + getValueString(this::getDamage, level) + " more damage with melee attacks",
        };
    }

    private double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.isCancelled()) return;

        Entity damagee = event.getDamagee();
        int level = getLevel(damager);
        if (level > 0) {

            boolean isPlayerGrounded = UtilBlock.isGrounded(damager, 1);

            DaggerData data = DaggerDataManager.getInstance().getDaggerData(damager);
            if (data != null && event.hasReason("Wind Dagger")) {
                isPlayerGrounded = data.isGrounded();
            }

            if(!isPlayerGrounded && !UtilBlock.isInWater(damager)){
                event.setDamage(event.getDamage() + getDamage(level));
                event.addReason(getName());
                damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_BREEZE_DEFLECT, 1.0F, 1.0F);

                for(int i = 0; i < 20 ; i++) {
                    final Location playerLoc = damagee.getLocation().add(0, 1, 0);
                    Particle.CRIT.builder()
                            .count(3)
                            .extra(0)
                            .offset(0.4, 1.0, 0.4)
                            .location(playerLoc)
                            .receivers(60)
                            .spawn();
                }
            }
        }

    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
    }
}