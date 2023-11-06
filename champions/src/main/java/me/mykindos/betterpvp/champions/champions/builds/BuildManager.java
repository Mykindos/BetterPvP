package me.mykindos.betterpvp.champions.champions.builds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.builds.repository.BuildRepository;
import me.mykindos.betterpvp.core.framework.manager.Manager;

@Singleton
@Getter
public class BuildManager extends Manager<GamerBuilds> {

    private final BuildRepository buildRepository;

    @Inject
    public BuildManager(BuildRepository buildRepository) {
        this.buildRepository = buildRepository;
    }
}
