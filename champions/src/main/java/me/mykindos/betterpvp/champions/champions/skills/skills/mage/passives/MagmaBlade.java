package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@BPvPListener
public class MagmaBlade extends Skill implements PassiveSkill {

    @Inject
    public MagmaBlade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Magma Blade";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your sword is fueled by flames,",
                "dealing an additional <val>" + (level) + "</val> damage",
                "to players who are on fire"};
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) return;

        int level = getLevel(player);
        if (level > 0) {
            LivingEntity ent = event.getDamagee();
            if (ent.getFireTicks() > 0) {
                event.setDamage(event.getDamage() + level);
            }
        }

    }

}



