package me.mykindos.betterpvp.core.interaction.actor;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Implementation of {@link InteractionActor} for non-player entities.
 */
@Getter
public class EntityInteractionActor implements InteractionActor {

    private final LivingEntity entity;
    private final EffectManager effectManager;

    public EntityInteractionActor(@NotNull LivingEntity entity, @NotNull EffectManager effectManager) {
        this.entity = entity;
        this.effectManager = effectManager;
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return entity.getUniqueId();
    }

    @Override
    public @NotNull LivingEntity getEntity() {
        return entity;
    }

    @Override
    public @NotNull Location getLocation() {
        return entity.getLocation();
    }

    @Override
    public @Nullable Client getClient() {
        return null;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public boolean isValid() {
        return entity.isValid() && !entity.isDead();
    }

    @Override
    public boolean isSneaking() {
        return false;
    }

    @Override
    public boolean isSprinting() {
        return false;
    }

    @Override
    public boolean isOnGround() {
        return entity.isOnGround();
    }

    @Override
    public boolean isInLiquid() {
        return UtilBlock.isInLiquid(entity);
    }

    @Override
    public boolean hasEnergy(double amount) {
        return true; // Entities always have energy
    }

    @Override
    public boolean useEnergy(String name, double amount, boolean inform) {
        return true; // Entities don't use energy
    }

    @Override
    public boolean isSilenced() {
        return effectManager.hasEffect(entity, EffectTypes.SILENCE);
    }

    @Override
    public boolean isStunned() {
        return effectManager.hasEffect(entity, EffectTypes.STUN);
    }
}
