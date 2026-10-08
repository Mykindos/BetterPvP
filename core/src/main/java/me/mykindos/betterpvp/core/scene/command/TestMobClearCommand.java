package me.mykindos.betterpvp.core.scene.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.mob.StaffTestMob;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Collection;

/** {@code /testmob clear} removes every test mob on the server. */
@Singleton
@SubCommand(TestMobCommand.class)
public class TestMobClearCommand extends Command {

    private final SceneObjectRegistry registry;

    @Inject
    public TestMobClearCommand(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "core.command.test-mob-clear.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Collection<StaffTestMob> mobs = registry.getObjects(StaffTestMob.class);
        if (mobs.isEmpty()) {
            UtilMessage.message(player, "core.prefix.test_mob",
                    Translations.component("core.command.testmob.clear.none").color(NamedTextColor.RED));
            return;
        }

        mobs.forEach(StaffTestMob::remove);
        UtilMessage.message(player, "core.prefix.test_mob",
                Translations.component("core.command.testmob.clear.done").color(NamedTextColor.GREEN));
    }
}
