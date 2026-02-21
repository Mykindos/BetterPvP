package me.mykindos.betterpvp.core.client.stats.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.start.StartStatMenu;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.model.IStringName;
import org.bukkit.entity.Player;

@Singleton
@CustomLog
public class TestStatsCommand extends Command implements IStringName {
    private final RealmManager realmManager;

    @Inject
    public TestStatsCommand(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    @Override
    public String getName() {
        return "statsmenu";
    }

    @Override
    public String getDescription() {
        return "Show stats for yourself";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new StartStatMenu(client, null, StatFilterType.ALL, null, realmManager).show(player);
    }
}
