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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class LevelField extends Skill implements PassiveSkill, Listener {

    private int radius;
    private double damagePerPlayer;
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
                "For every enemy that you are outnumbered by within <stat>" + radius + "</stat> blocks,",
                "you deal <val>" + (level * damagePerPlayer) + "</val> more and take <val>" + (level * damagePerPlayer) + "</val> less damage",
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
            int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();
            int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), radius).size() + 1;
            int nearbyDifference = (nearbyEnemies - nearbyAllies);

            double damageAdded = Math.max(0, (nearbyDifference * (damagePerPlayer * level)));

            event.setDamage(event.getDamage() + damageAdded);

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event){
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();
            int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), radius).size() + 1;
            int nearbyDifference = (nearbyEnemies - nearbyAllies);

            double damageSubtracted = Math.min(0, (nearbyDifference * -1) * (damagePerPlayer * level));

            event.setDamage(event.getDamage() + damageSubtracted);
        }
    }
    
    public void loadSkillConfig() {
        radius = getConfig("radius", 10, Integer.class);
        damagePerPlayer = getConfig("damagePerPlayer", 0.5, Double.class);
    }
}
