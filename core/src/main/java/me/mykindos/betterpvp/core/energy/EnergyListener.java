package me.mykindos.betterpvp.core.energy;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.events.DegenerateEnergyEvent;
import me.mykindos.betterpvp.core.energy.events.RegenerateEnergyEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@BPvPListener
public class EnergyListener implements Listener {

    private final EnergyHandler energyHandler;
    private final EffectManager effectManager;

    @Inject
    public EnergyListener(EnergyHandler energyHandler, EffectManager effectManager) {
        this.energyHandler = energyHandler;
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegen(RegenerateEnergyEvent event) {
        Player player = event.getPlayer();
        energyHandler.regenerateEnergy(event.getPlayer(), event.getEnergy());

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDegen(DegenerateEnergyEvent event) {
        if(event.isCancelled()) return;
        if(event.getPlayer().getGameMode().isInvulnerable()) return;

        effectManager.getEffect(event.getPlayer(), EffectTypes.ENERGY_REDUCTION).ifPresent(effect -> {
            event.setEnergy(event.getEnergy() * (1 - (effect.getAmplifier() / 100d)));
        });

        energyHandler.degenerateEnergy(event.getPlayer(), event.getEnergy());
    }

    @EventHandler
    public void handleRespawn(PlayerRespawnEvent event) {
        energyHandler.setEnergy(event.getPlayer(), 1.0f);
    }

    @EventHandler
    public void handleExp(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }

    @UpdateEvent(delay = EnergyHandler.UPDATE_RATE)
    public void update() {
        for (Player players : Bukkit.getOnlinePlayers()) {
            if (players.getExp() >= 0.999) continue;
            energyHandler.updateEnergy(players);
        }
    }

}
