package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

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
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class LevelField extends Skill implements PassiveSkill, Listener {

    @Inject
    public LevelField(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Level Field";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "For every enemy within 10 blocks of you",
                "you deal " + (level * 0.5) + " extra damage and take " + (level * 0.5) + " less damage",
                "Up to a maximum of " + (2 + (level * 2)) + " extra damage",
                "For every ally within 10 blocks of you",
                "you deal " + (level * 0.5) + " less damage and take " + (level * 0.5) + " more damage",
                "Down to a minimum of " + (6 - ((level) * 1.5)) + " less damage"
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), 10;
            int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), 10);
            event.setDamage(event.getDamage() - (Math.min((2 + (level * 2)), Math.max((-6 + ((level) * 1.5)),(nearbyDifference) * (level * 0.5)))));        }
    }
  
    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), 10;
            int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), 10);
            int nearbyDifference = (nearbyEnemies - nearbyAllies);
            event.setDamage(event.getDamage() + (Math.min((2 + (level * 2)), Math.max((-6 + ((level) * 1.5)),(nearbyDifference) * (level * 0.5)))));
        }
    }

}
