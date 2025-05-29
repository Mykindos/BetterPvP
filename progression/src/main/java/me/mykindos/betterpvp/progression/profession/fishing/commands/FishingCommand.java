package me.mykindos.betterpvp.progression.profession.fishing.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.FishingProfessionMenu;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
public class FishingCommand extends Command {

    private final FishingHandler fishingHandler;
    private final ProfessionProfileManager profileManager;
    private final ProfessionNodeManager professionNodeManager;

    @Inject
    public FishingCommand(FishingHandler fishingHandler, ProfessionProfileManager profileManager, ProfessionNodeManager professionNodeManager) {
        this.fishingHandler = fishingHandler;
        this.profileManager = profileManager;
        this.professionNodeManager = professionNodeManager;
    }

    @Override
    public String getName() {
        return "fishing";
    }

    @Override
    public String getDescription() {
        return "Fishing command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            new FishingProfessionMenu(fishingHandler, profile, professionNodeManager).show(player).addCloseHandler(() -> {
                profileManager.getRepository().updateBuildForGamer(player.getUniqueId(), profile.getProfessionDataMap().get("Fishing").getBuild());
            });
        });

    }
}
