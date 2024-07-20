package me.mykindos.betterpvp.champions.utilities;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class MobPathfinder implements Goal<Mob> {
    protected final Champions champions;

    private final GoalKey<Mob> key = GoalKey.of(Mob.class, new NamespacedKey("champions", "clone"));
    private final Mob mob;

    @Setter
    @Getter
    private LivingEntity target;

    public MobPathfinder(Champions champions, Mob mob, LivingEntity target) {
        this.champions = champions;
        this.mob = mob;
        this.target = target;
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

}