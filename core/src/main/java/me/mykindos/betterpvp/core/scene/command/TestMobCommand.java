package me.mykindos.betterpvp.core.scene.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

/** {@code /testmob} spawns and clears the staff test mob used to playtest mob AI. */
@Singleton
public class TestMobCommand extends Command {

    @Override
    public String getName() {
        return "testmob";
    }

    @Override
    public String getDescription() {
        return "core.command.test-mob.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "core.prefix.test_mob", "core.command.testmob.usage");
    }
}
