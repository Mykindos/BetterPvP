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
public class ClanCoreTip extends ClanTip implements IRunCommand {

    @Inject
    public ClanCoreTip(Clans clans) {
        super(clans, 1, 2);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanCore";
    }

    @Override
    public Component generateComponent() {
        Component runComponent = runCommand("/clan core");
        return Component.text("You can teleport back to your territory by running ", NamedTextColor.GRAY).append(runComponent);
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null && clan.getCore() != null;
    }
}
