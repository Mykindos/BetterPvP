package me.mykindos.betterpvp.core.interaction.actor;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Implementation of {@link InteractionActor} for players.
 */
@Getter
public class PlayerInteractionActor implements InteractionActor {

    private final Player player;
    private final Client client;
    private final EnergyService energyService;
    private final EffectManager effectManager;

    public PlayerInteractionActor(@NotNull Player player, @NotNull Client client,
                                   @NotNull EnergyService energyService, @NotNull EffectManager effectManager) {
        this.player = player;
        this.client = client;
        this.energyService = energyService;
        this.effectManager = effectManager;
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public @NotNull LivingEntity getEntity() {
        return player;
    }

    @Override
    public @NotNull Location getLocation() {
        return player.getLocation();
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Override
    public boolean isValid() {
        return player.isOnline() && !player.isDead();
    }

    @Override
    public boolean isSneaking() {
        return player.isSneaking();
    }

    @Override
    public boolean isSprinting() {
        return player.isSprinting();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isOnGround() {
        return player.isOnGround();
    }

    @Override
    public boolean isInLiquid() {
        return UtilBlock.isInLiquid(player);
    }

    @Override
    public boolean hasEnergy(double amount) {
        return energyService.getEnergy(player.getUniqueId()) >= amount;
    }

    @Override
    public boolean useEnergy(String name, double amount, boolean inform) {
        return energyService.use(player, name, amount, inform);
    }

    @Override
    public boolean isSilenced() {
        return effectManager.hasEffect(player, EffectTypes.SILENCE);
    }

    @Override
    public boolean isStunned() {
        return effectManager.hasEffect(player, EffectTypes.STUN);
    }
}
