package me.mykindos.betterpvp.clans.skills.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;

public class SkillInjectorModule extends AbstractModule {

    private final Clans clans;

    public SkillInjectorModule(Clans clans) {
        this.clans = clans;
    }

    @Override
    protected void configure() {
        bind(ExtendedYamlConfiguration.class).toInstance(clans.getConfig());
    }
}
