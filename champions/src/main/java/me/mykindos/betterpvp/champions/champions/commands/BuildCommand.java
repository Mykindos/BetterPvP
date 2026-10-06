package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.screen.SkillScreens;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class BuildCommand extends Command {

    private final SkillScreens skillScreens;

    @Inject
    public BuildCommand(SkillScreens skillScreens) {
        this.skillScreens = skillScreens;
    }

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public String getDescription() {
        return "champions.command.build.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        skillScreens.openClasses(player);
    }
}
