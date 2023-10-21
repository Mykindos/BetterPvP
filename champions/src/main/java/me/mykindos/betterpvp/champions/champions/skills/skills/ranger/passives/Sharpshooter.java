package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.StackingHitData;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Sharpshooter extends Skill implements PassiveSkill {

    private final WeakHashMap<Player, StackingHitData> data = new WeakHashMap<>();
    private final WeakHashMap<Player, Integer> misses = new WeakHashMap<>();

    private double maxTimeBetweenShots;
    private int maxConsecutiveHits;
    private int numMisses;

    @Inject
    public Sharpshooter(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Sharpshooter";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "You deal <val>" + (level * 0.75) + "</val> extra damage for",
                "each consecutive hit up to a maximum of <stat>"+ maxConsecutiveHits +"</stat> hits",
                "",
                "After <stat>" + maxTimeBetweenShots + "</stat> seconds, or after missing <stat>"+numMisses+"</stat> times,",
                "your arrow damage will reset"
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if(!(event.getProjectile() instanceof Arrow)) return;
        if(!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if(level > 0) {
            if(!data.containsKey(damager)) {
                data.put(damager, new StackingHitData());
            }

            StackingHitData hitData = data.get(damager);
            hitData.addCharge();
            event.setDamage(event.getDamage() + (Math.min(maxConsecutiveHits, hitData.getCharge()) * (level * 0.75)));
        }
    }

    @EventHandler
    public void onArrowMiss(ProjectileHitEvent event) {
        if(!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();
        if(!(arrow.getShooter() instanceof Player)) return;
        if(event.getHitEntity() != null) return;

        Player shooter = (Player) arrow.getShooter();
        int level = shooter.getLevel();

        if(level > 0){
            misses.put(shooter, misses.getOrDefault(shooter, 0) + 1);
            if(misses.get(shooter) >= numMisses) {
                data.remove(shooter);
                misses.put(shooter, 0);
            }
        }
    }

    @UpdateEvent(delay=100)
    public void updateSharpshooterData() {
        data.entrySet().removeIf(entry -> {
            if(System.currentTimeMillis() > entry.getValue().getLastHit() + ((maxTimeBetweenShots + getLevel(entry.getKey())) * 1000L)) {
                misses.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }


    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig(){
        maxTimeBetweenShots = getConfig("maxTimeBetweenShots", 5.0, Double.class);
        maxConsecutiveHits = getConfig("maxConsecutiveHits", 4, Integer.class);
        numMisses = getConfig("numMisses",2, Integer.class);
    }

}
