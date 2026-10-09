package me.mykindos.betterpvp.core.scene.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.mob.StaffTestMobFactory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** {@code /testmob spawn} spawns a test mob on the block the player is looking at. */
@Singleton
@SubCommand(TestMobCommand.class)
public class TestMobSpawnCommand extends Command {

    private final StaffTestMobFactory factory;

    @Inject
    public TestMobSpawnCommand(StaffTestMobFactory factory) {
        this.factory = factory;
    }

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "core.command.test-mob-spawn.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Block target = player.getTargetBlockExact(24);
        if (target == null) {
            UtilMessage.message(player, "core.prefix.test_mob",
                    Translations.component("core.command.testmob.spawn.no_block").color(NamedTextColor.RED));
            return;
        }

        factory.spawn(target.getLocation().add(0.5, 1, 0.5));
        UtilMessage.message(player, "core.prefix.test_mob",
                Translations.component("core.command.testmob.spawn.done").color(NamedTextColor.GREEN));
    }
}
