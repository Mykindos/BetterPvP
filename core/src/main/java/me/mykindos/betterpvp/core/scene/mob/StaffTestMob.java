package me.mykindos.betterpvp.core.scene.mob;

import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.mob.ai.component.LookAtTargetComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.MeleeAttackComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.RetaliateComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.ReturnHomeComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.TargetingComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.WanderComponent;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import me.mykindos.betterpvp.core.scene.mob.target.TargetSelectors;
import org.bukkit.entity.EntityType;

import java.util.function.LongSupplier;

/**
 * A hostile skeleton warrior that staff spawn with {@code /testmob} to playtest the mob AI and animation framework.
 * It wanders near home, fights the nearest player or whoever hit it, and walks back when pulled too far away.
 */
public class StaffTestMob extends SceneMob {

    public StaffTestMob(SceneObjectFactory factory) {
        this(factory, System::currentTimeMillis);
    }

    StaffTestMob(SceneObjectFactory factory, LongSupplier clock) {
        super(factory, EntityType.VINDICATOR, Disposition.HOSTILE, clock);
        setModelId("skeleton_warrior");
        setAnimation(MobAnimation.IDLE, "idle");
        setAnimation(MobAnimation.WALK, "walk");
        setAnimation(MobAnimation.ATTACK, "attack");
        setAnimation(MobAnimation.HURT, "hit");
    }

    @Override
    protected void registerComponents() {
        getAi().add(new ReturnHomeComponent(this));
        getAi().add(new RetaliateComponent(this));
        getAi().add(new TargetingComponent(this, TargetSelectors.nearestPlayer(16)));
        // The attack clip is 2.2s long and the blade lands about 0.8s in.
        getAi().add(new MeleeAttackComponent(this, getClock()).windupMillis(800).cooldownMillis(2200));
        getAi().add(new LookAtTargetComponent(this));
        getAi().add(new WanderComponent(this, getClock()));
    }
}
