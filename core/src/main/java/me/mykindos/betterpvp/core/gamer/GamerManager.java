package me.mykindos.betterpvp.core.gamer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.gamer.repository.GamerRepository;

import java.util.List;
import java.util.Optional;

@Singleton
public class GamerManager extends Manager<Gamer> {

    @Getter
    private final GamerRepository gamerRepository;

    @Inject
    public GamerManager(GamerRepository gamerRepository) {
        this.gamerRepository = gamerRepository;

    }

    /**
     * Get a gamer by their name
     * Only use this if the UUID is not easily available as this is a slow operation
     * @param name The player name
     * @return The gamer
     */
    public Optional<Gamer> getGamerByName(String name) {
        return objects.values().stream().filter(gamer -> gamer.getClient().getName().equalsIgnoreCase(name)).findFirst();
    }

    public List<Gamer> getGamersOfRank(Rank rank) {
        return objects.values().stream().filter(gamer -> gamer.getClient().hasRank(rank)).toList();
    }

    @Override
    public void loadFromList(List<Gamer> objects) {
        objects.forEach(gamer -> {
            addObject(gamer.getUuid(), gamer);
        });
    }
}
