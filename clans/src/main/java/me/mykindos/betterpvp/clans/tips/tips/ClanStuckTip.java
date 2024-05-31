package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.tips.types.IRunCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanStuckTip extends ClanTip implements IRunCommand {

    @Inject
    public ClanStuckTip(Clans clans) {
        super(clans, 1, 2);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanstuck";
    }

    @Override
    public Component generateComponent() {
        Component runComponent = runCommand("/c stuck");
        return Component.text("If you ever get stuck in another clans territory, you can run ", NamedTextColor.GRAY)
                .append(runComponent).append(Component.text(" to be teleported to the nearest wilderness."));
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return true;
    }
}
