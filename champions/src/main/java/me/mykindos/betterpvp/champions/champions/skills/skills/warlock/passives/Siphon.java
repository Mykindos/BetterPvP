package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
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

    private int radius;

    private double energySiphoned;
    @Inject
    public Siphon(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Siphon";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Siphon energy from all enemies within <val>" + (radius + level) + "</val> blocks,",
                "Granting you Speed II and sometimes a small amount of health.",
                "",
                "Energy siphoned per second: <val>" + energySiphoned
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
                for(Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius + level)) {;
                    championsManager.getEnergy().degenerateEnergy(target, ((float) energySiphoned)/10.0f);
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
                    }.runTaskTimer(champions, 0l, 2);
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
        radius = getConfig("radius", 4, Integer.class);
        energySiphoned = getConfig("energySiphoned", 1.0, Double.class);
    }
}
