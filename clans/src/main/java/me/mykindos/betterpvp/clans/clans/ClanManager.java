package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;
import java.util.Optional;

@Singleton
public class ClanManager extends Manager<Clan> {

    @Getter
    private final ClanRepository repository;
    private final ClientManager clientManager;

    @Inject
    public ClanManager(ClanRepository repository, ClientManager clientManager) {
        this.repository = repository;
        this.clientManager = clientManager;

        var clans = repository.getAll();

        clans.forEach(clan -> {
            clan.setTerritory(repository.getTerritory(this, clan));
            clan.setAlliances(repository.getAlliances(this, clan));
            clan.setEnemies(repository.getEnemies(this, clan));
            clan.setMembers(repository.getMembers(this, clan));
        });

        loadFromList(clans);
    }

    public Optional<Clan> getClanById(int id) {
        return objects.values().stream().filter(clan -> clan.getId() == id).findFirst();
    }

    public Optional<Clan> getClanByClient(Client client) {
        return objects.values().stream()
                .filter(clan -> clan.getMembers().stream()
                        .anyMatch(clanMember -> clientManager.getObject(client.getUuid()).isPresent())).findFirst();
    }

    @Override
    public void loadFromList(List<Clan> objects) {
        objects.forEach(clan -> addObject(clan.getName(), clan));
    }
}
