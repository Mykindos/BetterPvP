package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.clans.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanCreateTip extends ClanTip implements ISuggestCommand {
    public ClanCreateTip() {
        super(10, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clancreate";
    }

    @Override
    public Component generateComponent() {
        Component suggestComponent = suggestCommand("/c create <name>", "/c create ");
        return Component.text("You can create a clan by running ", NamedTextColor.GRAY).append(suggestComponent);
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan == null;
    }
}
