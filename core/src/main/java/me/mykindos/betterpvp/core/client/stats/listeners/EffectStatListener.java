package me.mykindos.betterpvp.core.client.stats.listeners;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.core.EffectDurationStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.Relation;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class EffectStatListener extends TimedStatListener {
    private final EffectManager effectManager;

    protected EffectStatListener(ClientManager clientManager, EffectManager effectManager) {
        super(clientManager);
        this.effectManager = effectManager;
    }

    @Override
    public void onUpdate(Client client, long deltaTime) {
        effectManager.getAllEffects(client.getUniqueId()).forEach(
                effect -> {
                    final EffectDurationStat receiveStat = EffectDurationStat.builder()
                            .relation(Relation.RECEIVED)
                            .effectType(effect.getEffectType().getName())
                            .effectName(effect.getName())
                            .build();

                    final EffectDurationStat dealStat = EffectDurationStat.builder()
                            .relation(Relation.DEALT)
                            .effectType(effect.getEffectType().getName())
                            .effectName(effect.getName())
                            .build();

                    client.getStatContainer().incrementStat(receiveStat, deltaTime);
                    LivingEntity applier = effect.getApplier().get();
                    if (applier instanceof Player player) {
                        clientManager.incrementStat(player, dealStat, deltaTime);
                    }
                }
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEffectExpire(EffectExpireEvent event) {
        if (event.getTarget() instanceof Player player) {
            doUpdate(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEffectReceive(EffectReceiveEvent event) {
        if (event.getTarget() instanceof Player player) {
            doUpdate(player);
        }
    }
}
