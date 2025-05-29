package me.mykindos.betterpvp.progression.profession.mining.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.MiningProfessionMenu;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
public class MiningCommand extends Command {

    private final MiningHandler miningHandler;
    private final ProfessionProfileManager profileManager;
    private final ProfessionNodeManager progressionSkillManager;

    @Inject
    public MiningCommand(MiningHandler miningHandler, ProfessionProfileManager profileManager, ProfessionNodeManager professionNodeManager) {
        this.miningHandler = miningHandler;

        this.profileManager = profileManager;
        this.progressionSkillManager = professionNodeManager;
    }

    @Override
    public String getName() {
        return "mining";
    }

    @Override
    public String getDescription() {
        return "Mining command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            new MiningProfessionMenu(miningHandler, profile, progressionSkillManager).show(player).addCloseHandler(() -> {
                profileManager.getRepository().updateBuildForGamer(player.getUniqueId(), profile.getProfessionDataMap().get("Mining").getBuild());
            });
        });

    }
}
