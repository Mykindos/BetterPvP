package me.mykindos.betterpvp.core.framework.statusbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;

@Singleton
@PluginAdapter("Core")
public class StatusBarController {

    private final EntityHealthService healthService;
    private final EnergyService energyService;

    @Inject
    private StatusBarController(EntityHealthService healthService, EnergyService energyService) {
        this.healthService = healthService;
        this.energyService = energyService;
    }

    public void setup(Gamer gamer) {
        gamer.setActionBar(new StatusBar(healthService, energyService));
    }

}
