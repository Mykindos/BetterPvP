package me.mykindos.betterpvp.clans.injector;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.DeathFrequencyFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.DeathLocalityFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.DistanceFromSafetyFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.FatigueFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.RepeatKillerFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.punishment.FatiguePunishment;
import me.mykindos.betterpvp.clans.clans.fatigue.punishment.SlownessPunishment;

public class ClansInjectorModule extends AbstractModule {

    private final Clans plugin;

    public ClansInjectorModule(Clans plugin) {
        this.plugin = plugin;

    }

    @Override
    protected void configure() {
        bind(Clans.class).toInstance(plugin);

        // Battle fatigue strategies. Adding/removing a factor or punishment is a
        // single line here — the manager and hold service never name a concrete
        // implementation (Open/Closed).
        final Multibinder<FatigueFactor> factors = Multibinder.newSetBinder(binder(), FatigueFactor.class);
        factors.addBinding().to(RepeatKillerFactor.class);
        factors.addBinding().to(DeathLocalityFactor.class);
        factors.addBinding().to(DeathFrequencyFactor.class);
        factors.addBinding().to(DistanceFromSafetyFactor.class);

        final Multibinder<FatiguePunishment> punishments = Multibinder.newSetBinder(binder(), FatiguePunishment.class);
        punishments.addBinding().to(SlownessPunishment.class);
    }

}
