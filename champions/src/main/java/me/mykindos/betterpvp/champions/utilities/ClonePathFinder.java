package me.mykindos.betterpvp.champions.utilities;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;

@Singleton
public class ClonePathFinder implements Goal<Mob>, Listener {
    protected final Champions champions;

    private final GoalKey<Mob> key = GoalKey.of(Mob.class, new NamespacedKey("champions", "clone"));
    private final Mob mob;
    private LivingEntity target;
    private final Player owner;

    @Inject
    public ClonePathFinder(Champions champions, Mob mob, Player owner, LivingEntity target) {
        this.champions = champions;
        this.mob = mob;
        this.target = target;
        this.owner = owner;
    }

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean shouldActivate() {
        return true;
    }

    @Override
    public void tick() {
        if(target == null) return;
        mob.setTarget(target);

        if(mob.getLocation().distanceSquared(target.getLocation()) < 3) return;

        mob.getPathfinder().moveTo(target);
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }

    //Lock/Switch clone onto player being damaged by its owner.
    @EventHandler
    public void onDamageEvent(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player player && event.getDamagee() instanceof Player && owner.equals(player)) {
            this.target = event.getDamagee();
        }
    }
}