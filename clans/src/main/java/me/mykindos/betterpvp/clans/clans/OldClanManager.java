package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.repository.OldClanRepository;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

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
        loadFromList(repository.getAll());
    }

    /**
     *
     * @param id the id of the clan
     * @return the IOldClan, or null if id is null, or if no OldClan by that id can be found
     */
    @Nullable
    public IOldClan getOldClan(@Nullable String id) {
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
    @Nullable
    public IOldClan getOldClan(@Nullable UUID id) {
        if (id == null) {
            return null;
        }
        Optional<Clan> clanOptional = clanManager.getClanById(id);
        if (clanOptional.isPresent()) {
            return clanOptional.get();
        }
        return getObject(id).orElse(null);
    }

    @Override
    public void loadFromList(List<OldClan> objects) {
        objects.forEach(oldClan -> addObject(oldClan.getId(), oldClan));
        log.info("Loaded {} oldClans", objects.size());
    }

    public void reload() {
        UtilServer.runTaskLaterAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            loadFromList(repository.getAll());
        }, 3*20L);
    }


}
