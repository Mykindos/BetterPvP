package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class HeavyArrows extends Skill implements PassiveSkill, EnergySkill{

    private final WeakHashMap<Arrow, Location> arrows = new WeakHashMap<>();

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
                "pushed backwards (unless crouching)."};
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @UpdateEvent
    public void update() {
        Iterator<Arrow> it = arrows.keySet().iterator();
        while (it.hasNext()) {
            Arrow next = it.next();
            if (next == null || next.isDead()) {
                it.remove();
            } else {
                Location location = next.getLocation().add(new Vector(0, 0.25, 0));
                Particle.CRIT_MAGIC.builder().location(location).receivers(60).extra(0).spawn();
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        int level = getLevel(player);
        if (level > 0) {
            PlayerCanUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerCanUseSkillEvent(player, this));
            if (!skillEvent.isCancelled()) {
                arrows.put(arrow, arrow.getLocation());

                if (!player.isSneaking()) {

                    if (championsManager.getEnergy().use(player, getName(), getEnergy(level) / 2, false)) {
                        Vector pushback = player.getLocation().getDirection().multiply(-1);

                        float charge = event.getForce();

                        pushback.multiply(1.25 * charge);
                        championsManager.getEnergy().degenerateEnergy(player, 0.1);
                        player.setVelocity(pushback);
                    }
                }
            }
        }
    }

    @Override
    public float getEnergy(int level) {

        return energy - ((level - 1));
    }
    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }
}