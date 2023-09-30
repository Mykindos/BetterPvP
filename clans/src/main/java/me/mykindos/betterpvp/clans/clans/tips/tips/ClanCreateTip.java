package me.mykindos.betterpvp.clans.clans.tips.tips;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.tips.Tip;
import me.mykindos.betterpvp.clans.clans.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class ClanCreateTip extends Tip implements ISuggestCommand {
    ClanCreateTip() {
        super(10, 1);
        setComponent(generateComponent());
    }

    private Component generateComponent() {
        Component suggestComponent = suggestCommand("/c create <name>", "/c create ");
        Component component = Component.text("You can create a clan by running ", NamedTextColor.GRAY).append(suggestComponent);
        return component;
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan == null;
    }
}
