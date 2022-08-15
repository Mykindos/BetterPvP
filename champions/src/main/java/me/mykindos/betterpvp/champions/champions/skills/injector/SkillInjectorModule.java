package me.mykindos.betterpvp.champions.champions.skills.injector;

import com.google.inject.AbstractModule;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;

public class SkillInjectorModule extends AbstractModule {

    private final Champions champions;

    public SkillInjectorModule(Champions champions) {
        this.champions = champions;
    }

    @Override
    protected void configure() {
        bind(ExtendedYamlConfiguration.class).toInstance(champions.getConfig());
    }
}
