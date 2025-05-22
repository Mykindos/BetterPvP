package me.mykindos.betterpvp.core.energy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.events.DegenerateEnergyEvent;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.energy.events.RegenerateEnergyEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.display.GamerDisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.experience.data.ExperienceBarData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@BPvPListener
@Singleton
public class EnergyHandler implements Listener {

    private final EnergyService energyService;
    private final EffectManager effectManager;

    private final GamerDisplayObject<ExperienceBarData> displayObject;

    @Inject
    public EnergyHandler(EnergyService energyService, EffectManager effectManager) {
        this.energyService = energyService;
        this.effectManager = effectManager;
        this.displayObject = new GamerDisplayObject<>((gamer) -> {
                final Energy energy = energyService.getEnergyObject(gamer.getUniqueId());
                if (energy == null) return null;
                final float percentage = (float) (energy.getCurrent()/ energy.getMax());
                return new ExperienceBarData(percentage);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(ClientJoinEvent event) {
        event.getClient().getGamer().getExperienceBar().add(500, displayObject);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDegen(DegenerateEnergyEvent event) {
        if(event.getPlayer().getGameMode().isInvulnerable()) return;
        if (event.getCause() != EnergyEvent.CAUSE.USE) return;
        effectManager.getEffect(event.getPlayer(), EffectTypes.ENERGY_REDUCTION).ifPresent(effect -> {
            event.setEnergy(event.getEnergy() * (1 - (effect.getAmplifier() / 100d)));
        });

    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDegenerateFinal(DegenerateEnergyEvent event) {
        if(event.getPlayer().getGameMode().isInvulnerable()) return;
        if (event.getCause() != EnergyEvent.CAUSE.USE) return;
        if (event.getEnergy() <= 0) return;
        energyService.addEnergyCooldown(event.getPlayer());
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegen(RegenerateEnergyEvent event) {
        if (event.getCause() != EnergyEvent.CAUSE.NATURAL) return;
        if (energyService.isOnRegenCooldown(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void handleRespawn(PlayerRespawnEvent event) {
        energyService.setEnergy(event.getPlayer().getUniqueId(), energyService.getMax(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void handleExp(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }

    @UpdateEvent(delay = EnergyService.UPDATE_RATE)
    public void update() {
        energyService.tick();
    }

}
