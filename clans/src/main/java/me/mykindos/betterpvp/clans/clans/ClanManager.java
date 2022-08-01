package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Slf4j
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

        log.info("Loaded {} clans", clans.size());

        loadFromList(clans);
    }

    public Optional<Clan> getClanById(int id) {
        return objects.values().stream().filter(clan -> clan.getId() == id).findFirst();
    }

    public Optional<Clan> getClanByClient(Client client) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(client.getUuid()).isPresent()).findFirst();
    }

    public Optional<Clan> getClanByPlayer(Player player) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(player.getUniqueId().toString()).isPresent()).findFirst();
    }

    /**
     * Finds a clan if the location is within a claimed chunk
     *
     * @param location The location to check
     * @return a Clan optional
     */
    public Optional<Clan> getClanByLocation(Location location) {
        return objects.values().stream()
                .filter(clan -> clan.getTerritory().stream()
                        .anyMatch(territory -> territory.getChunk().equals(location.getChunk()))).findFirst();
    }

    public boolean isClanMember(Player player, Player target) {
        Optional<Clan> aClanOptional = getClanByPlayer(player);
        Optional<Clan> bClanOptional = getClanByPlayer(target);

        if (aClanOptional.isEmpty() || bClanOptional.isEmpty()) return false;

        return aClanOptional.equals(bClanOptional);

    }

    // TODO implement pillaging
    public ClanRelation getRelation(Clan clanA, Clan clanB) {
        if (clanA == null || clanB == null) {
            return ClanRelation.NEUTRAL;
        } else if (clanA == clanB) {
            return ClanRelation.SELF;
        } else if (clanB.hasTrust(clanA)) {
            return ClanRelation.ALLY_TRUST;
        } else if (clanA.isAllied(clanB)) {
            return ClanRelation.ALLY;
        } else if (clanA.isEnemy(clanB)) {
            return ClanRelation.ENEMY;

            // } else if (Pillage.isPillaging(clanA, clanB)) {
            //    return ClanRelation.PILLAGE;
            //} else if (Pillage.isPillaging(clanB, clanA)) {
            //    return ClanRelation.PILLAGE;
        }

        return ClanRelation.NEUTRAL;
    }

    public boolean hasAccess(Player player, Location location) {
        Optional<Clan> playerClanOptional = getClanByPlayer(player);
        Optional<Clan> locationClanOptional = getClanByLocation(location);

        if (locationClanOptional.isEmpty()) return true;
        if (playerClanOptional.isEmpty()) return false;

        Clan playerClan = playerClanOptional.get();
        Clan locationClan = locationClanOptional.get();


        // TODO implement pillaging
        //if (Pillage.isPillaging(pClan, locClan)) {
        //    return true;
        //}


        ClanRelation relation = getRelation(playerClan, locationClan);

        return relation == ClanRelation.SELF || relation == ClanRelation.ALLY_TRUST;
    }

    @Override
    public void loadFromList(List<Clan> objects) {
        objects.forEach(clan -> addObject(clan.getName(), clan));
    }
}
