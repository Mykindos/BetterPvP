package me.mykindos.betterpvp.clans.gamer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.clans.champions.builds.repository.BuildRepository;
import me.mykindos.betterpvp.clans.gamer.repository.GamerRepository;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;

@Singleton
public class GamerManager extends Manager<Gamer> {

    @Getter
    private final GamerRepository gamerRepository;

    @Getter
    private final BuildRepository buildRepository;

    @Inject
    public GamerManager(GamerRepository gamerRepository, BuildRepository buildRepository) {
        this.gamerRepository = gamerRepository;
        this.buildRepository = buildRepository;

    }

    @Override
    public void loadFromList(List<Gamer> objects) {
        objects.forEach(gamer -> {
            addObject(gamer.getUuid(), gamer);
            buildRepository.loadBuilds(gamer);
        });
    }
}
