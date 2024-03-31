package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.repository.OldClanRepository;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@CustomLog
@Singleton
public class OldClanManager extends Manager<OldClan> {

    @Getter
    private final OldClanRepository repository;
    public final ClanManager clanManager;


    @Inject
    public OldClanManager(OldClanRepository repository, ClanManager clanManager) {
        this.repository = repository;
        this.clanManager = clanManager;
    }

    /**
     *
     * @param id the id of the clan
     * @return the IOldClan, or null if id is null, or if no OldClan by that id can be found
     */
    public IOldClan getOldClan(String id) {
        if (id == null) {
            return null;
        }
        return getOldClan(UUID.fromString(id));
    }

    /**
     *
     * @param id the id of the clan
     * @return the IOldClan, or null if id is null, or if no OldClan by that id can be found
     */
    public IOldClan getOldClan(UUID id) {
        if (id == null) {
            return null;
        }
        Optional<Clan> clanOptional = clanManager.getClanById(id);
        if (clanOptional.isPresent()) {
            return clanOptional.get();
        }
        return (IOldClan) getObject(id).orElse(null);
    }

    @Override
    public void loadFromList(List<OldClan> objects) {
        objects.forEach(oldClan -> addObject(oldClan.getId(), oldClan));
        log.info("Loaded {} oldClans", objects.size());
    }


}
