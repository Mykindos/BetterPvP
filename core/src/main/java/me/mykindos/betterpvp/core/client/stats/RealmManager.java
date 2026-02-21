package me.mykindos.betterpvp.core.client.stats;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class RealmManager extends Manager<Integer, Realm> {
    private final RealmRepository realmRepository;
    @Getter
    private final Map<Integer, Season> seasonMap = new HashMap<>();
    private final Multimap<Season, Realm> seasonRealmMap = ArrayListMultimap.create();

    @Inject
    public RealmManager(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
        objects.putAll(realmRepository.loadAll().join());
        objects.values().forEach(realm -> {
            seasonRealmMap.put(realm.getSeason(), realm);
            seasonMap.put(realm.getSeason().getId(), realm.getSeason());
        });
    }

    public Collection<Realm> getRealmsBySeason(Season season) {
        return seasonRealmMap.get(season);
    }

    public Optional<Season> getSeason(int id) {
        return Optional.ofNullable(seasonMap.get(id));
    }

    public Optional<Season> getSeason(String name) {
        return seasonMap.values().stream()
                .filter(season -> season.getName().equalsIgnoreCase(name))
                .findAny();
    }
}
