package me.mykindos.betterpvp.core.world.settler.presence;

import lombok.Getter;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.mob.Disposition;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.component.LookAtNearbyPlayerComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.PostComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.WanderComponent;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The body of one settler. It works at its post while it has one and otherwise wanders around its home, resting
 * between trips, and faces a player close by while it stands.
 */
public class SettlerNPC extends SceneMob {

    @Getter private final UUID settlerId;
    private final Supplier<Optional<Location>> post;
    private final LongSupplier clock;

    public SettlerNPC(@NotNull SceneObjectFactory factory, @NotNull UUID settlerId,
                      @NotNull Supplier<Optional<Location>> post) {
        this(factory, settlerId, post, System::currentTimeMillis);
    }

    SettlerNPC(@NotNull SceneObjectFactory factory, @NotNull UUID settlerId,
               @NotNull Supplier<Optional<Location>> post, @NotNull LongSupplier clock) {
        super(factory, EntityType.PIG, Disposition.NEUTRAL, clock);
        this.settlerId = settlerId;
        this.post = post;
        this.clock = clock;
        setDamageTint(null);
    }

    @Override
    protected void registerComponents() {
        getAi().add(new PostComponent(this, post, clock).speed(0.6));
        getAi().add(new WanderComponent(this, clock).wanderRadius(12).wanderSpeed(0.6)
                .rest(5_000, 15_000).checkFloor(2));
        getAi().add(new LookAtNearbyPlayerComponent(this).radius(5));
    }
}
