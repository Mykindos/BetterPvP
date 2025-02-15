package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class NullBlade extends Skill implements PassiveSkill, OffensiveSkill {

    private double energySiphoned;

    @Inject
    public NullBlade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Null Blade";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Your sword sucks <val>" + getSiphonedEnergy() + "</val> energy from",
                "opponents with every attack"
        };
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (event.getDamagee().hasMetadata("PlayerSpawned")) return;

        if (!(event.getDamager() instanceof Player dam)) return;

        if (hasSkill(dam)) {
            double degeneration = getSiphonedEnergy() * 0.01;

            if (event.getDamagee() instanceof Player target) {
                championsManager.getEnergy().degenerateEnergy(target, degeneration);
            }
            championsManager.getEnergy().regenerateEnergy(dam, degeneration);
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    public float getSiphonedEnergy() {
        return (float) (energySiphoned);
    }

    @Override
    public void loadSkillConfig() {
        energySiphoned = getConfig("energySiphoned", 7.0, Double.class);
    }
}
