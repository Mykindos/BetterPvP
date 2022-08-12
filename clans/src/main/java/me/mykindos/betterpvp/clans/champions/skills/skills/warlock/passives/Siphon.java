package me.mykindos.betterpvp.clans.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class Siphon extends Skill implements PassiveSkill {

    @Inject
    public Siphon(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Siphon";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Siphon energy from all enemies within " + ChatColor.GREEN + (4 + level) + ChatColor.GRAY + " blocks,",
                "Granting you Speed II and sometimes a small amount of health.",
                "",
                "Energy siphoned per second: " + ChatColor.GREEN + 5
        };
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @UpdateEvent(delay = 2000)
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if(level > 0) {
                for(var data : UtilPlayer.getNearbyPlayers(player, player.getLocation(), 4+ level, EntityProperty.ENEMY)) {
                    Player target = data.get();
                    championsManager.getEnergy().degenerateEnergy(target, 0.1f);
                    new BukkitRunnable() {
                        private final Location position = target.getLocation().add(0, 1, 0);

                        @Override
                        public void run() {
                            Location playerLoc = player.getLocation().clone().add(0, 1, 0);
                            Vector v = UtilVelocity.getTrajectory(position, playerLoc);
                            if (player.isDead()) {
                                this.cancel();
                                return;
                            }
                            if (position.distance(playerLoc) < 1) {
                                if (UtilMath.randomInt(10) == 1) {
                                    UtilPlayer.health(player, 1);
                                }
                                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 0));
                                this.cancel();
                                return;
                            }

                            Particle.END_ROD.builder().location(position).receivers(30).extra(0).spawn();
                            v.multiply(0.9);
                            position.add(v);
                        }
                    }.runTaskTimer(clans, 0l, 2);
                }
            }

        }
    }


    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

}
