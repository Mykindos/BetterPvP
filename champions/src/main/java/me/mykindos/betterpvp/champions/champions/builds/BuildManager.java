package me.mykindos.betterpvp.champions.champions.builds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.builds.repository.BuildRepository;
import me.mykindos.betterpvp.core.framework.manager.Manager;

@Singleton
public class BuildManager extends Manager<GamerBuilds> {

    @Getter
    private final BuildRepository buildRepository;

    @Inject
    public BuildManager(BuildRepository buildRepository) {
        this.buildRepository = buildRepository;
    }

    public GamerBuilds loadBuildsForPlayer(String UUID) {
        //getObjects().clear();
        GamerBuilds builds = new GamerBuilds(UUID);
        getBuildRepository().loadBuilds(builds);
        getBuildRepository().loadDefaultBuilds(builds);
        getBuildRepository().loadActiveBuilds(builds);
        addObject(UUID, builds);
        return builds;
    }
}
