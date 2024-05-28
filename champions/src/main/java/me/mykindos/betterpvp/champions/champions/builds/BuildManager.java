package me.mykindos.betterpvp.champions.champions.builds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.event.ChampionsBuildLoadedEvent;
import me.mykindos.betterpvp.champions.champions.builds.repository.BuildRepository;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
@Getter
public class BuildManager extends Manager<GamerBuilds> {

    private final BuildRepository buildRepository;

    private final Champions champions;

    @Inject
    public BuildManager(BuildRepository buildRepository, Champions champions) {
        this.buildRepository = buildRepository;
        this.champions = champions;
    }

    public void loadBuilds(Player player) {
        GamerBuilds builds = new GamerBuilds(player.getUniqueId().toString());
        getBuildRepository().loadBuilds(builds);
        getBuildRepository().loadDefaultBuilds(builds);
        addObject(player.getUniqueId().toString(), builds);
        UtilServer.runTask(champions, () -> {
            UtilServer.callEvent(new ChampionsBuildLoadedEvent(player, builds));
        });
    }

    public void reloadBuilds() {
        getObjects().clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadBuilds(player);
        }
    }


}
