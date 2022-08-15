package me.mykindos.betterpvp.core.gamer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.gamer.repository.GamerRepository;

import java.util.List;

@Singleton
public class GamerManager extends Manager<Gamer> {

    @Getter
    private final GamerRepository gamerRepository;

    @Inject
    public GamerManager(GamerRepository gamerRepository) {
        this.gamerRepository = gamerRepository;

    }

    @Override
    public void loadFromList(List<Gamer> objects) {
        objects.forEach(gamer -> {
            addObject(gamer.getUuid(), gamer);
        });
    }
}
