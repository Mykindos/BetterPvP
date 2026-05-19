package me.mykindos.betterpvp.clans.injector;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.*;
import me.mykindos.betterpvp.clans.clans.fatigue.punishment.FatiguePunishment;
import me.mykindos.betterpvp.clans.clans.fatigue.punishment.SlownessPunishment;
import me.mykindos.betterpvp.clans.displayname.ClansDisplayNameProvider;
import me.mykindos.betterpvp.core.displayname.DisplayNameProvider;

public class ClansInjectorModule extends AbstractModule {

    private final Clans plugin;

    public ClansInjectorModule(Clans plugin) {
        this.plugin = plugin;

    }

    @Override
    protected void configure() {
        bind(Clans.class).toInstance(plugin);

        bind(DisplayNameProvider.class).to(ClansDisplayNameProvider.class);

        // Battle fatigue strategies. Adding/removing a factor or punishment is a
        // single line here — the manager and hold service never name a concrete
        // implementation (Open/Closed).
        final Multibinder<FatigueFactor> factors = Multibinder.newSetBinder(binder(), FatigueFactor.class);
        factors.addBinding().to(RepeatKillerFactor.class);
        factors.addBinding().to(DeathLocalityFactor.class);
        factors.addBinding().to(DeathFrequencyFactor.class);
        factors.addBinding().to(DistanceFromSafetyFactor.class);
        factors.addBinding().to(PlayerDeathFactor.class);

        final Multibinder<FatiguePunishment> punishments = Multibinder.newSetBinder(binder(), FatiguePunishment.class);
        punishments.addBinding().to(SlownessPunishment.class);
    }

}
