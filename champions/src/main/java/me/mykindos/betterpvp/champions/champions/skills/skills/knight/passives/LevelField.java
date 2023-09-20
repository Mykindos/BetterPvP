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
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
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
                "You deal " + ChatColor.GREEN + (10 + ((level - 1) * 5)) + "%" + ChatColor.GRAY + " extra damage",
                "Up to a maximum of " + ChatColor.GREEN + (30 + ((level-1) * 15)) + "%" + ChatColor.GRAY + " extra damage",
                "For every ally within 10 blocks of you",
                "You deal " + ChatColor.GREEN + (10 + ((level - 1) * 5)) + "%" + ChatColor.GRAY + " less damage",
                "Down to a minimum of " + ChatColor.GREEN + (70 - (level * 10)) + "%" + ChatColor.GRAY + " less damage"
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
    public void onHit(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), 10).size();
            int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), 10).size();
            int nearbyDifference = ((nearbyEnemies - 1) - nearbyAllies);

            double minDamageDealt = (((100 - (70 - (level * 10))) * 0.01) * event.getDamage());
            double scaledDamageDealt = ((((20 + ((level - 1) * 5)) * nearbyDifference) * 0.01) * event.getDamage());
            double maxDamageDealt = (((30 + ((level-1) * 15)) * 0.01) * event.getDamage());

            if(scaledDamageDealt == 0){
                event.setDamage(event.getDamage());
            } else if (scaledDamageDealt < minDamageDealt) {
                event.setDamage(minDamageDealt);
            } else if (scaledDamageDealt > maxDamageDealt) {
                event.setDamage(maxDamageDealt+event.getDamage());}
            else{
                event.setDamage(event.getDamage() + scaledDamageDealt);
            }
        }
    }

}
