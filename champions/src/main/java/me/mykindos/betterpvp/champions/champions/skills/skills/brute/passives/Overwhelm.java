package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Set;

@Singleton
@BPvPListener
public class Overwhelm extends Skill implements PassiveSkill {

    @Inject
    public Overwhelm(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Overwhelm";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You deal <stat>1</stat> bonus damage for every",
                "<stat>2</stat> more health you have than your target",
                "",
                "You can deal a maximum of <val>" + String.format("%.1f", (0.0 + (level * 0.5))) + "</val> bonus damage"
        };
    }
    @Override
    public String getDefaultClassString() {
        return "brute";
    }
    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        int level = getLevel(player);
        if (level > 0) {
            LivingEntity ent = event.getDamagee();
            double difference = (player.getHealth() - ent.getHealth()) / 2;
            if (difference > 0) {
                difference = Math.min(difference, (level * 0.5));
                event.setDamage(event.getDamage() + difference);
            }
        }
    }


}
