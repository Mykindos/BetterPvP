package me.mykindos.betterpvp.core.framework.statusbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.CombatFeaturesService;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.energy.EnergyService;

@Singleton
public class StatusBarController {

    private final EntityHealthService healthService;
    private final EnergyService energyService;
    private final CombatFeaturesService combatFeaturesService;
    private final EffectManager effectManager;

    @Inject
    private StatusBarController(EntityHealthService healthService, EnergyService energyService, CombatFeaturesService combatFeaturesService, EffectManager effectManager) {
        this.healthService = healthService;
        this.energyService = energyService;
        this.combatFeaturesService = combatFeaturesService;
        this.effectManager = effectManager;
    }

    public void setup(Gamer gamer) {
        gamer.setActionBar(new StatusBar(healthService, energyService, combatFeaturesService, effectManager));
    }

}
