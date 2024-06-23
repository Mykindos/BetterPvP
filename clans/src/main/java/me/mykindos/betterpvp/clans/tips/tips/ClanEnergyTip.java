package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

@Singleton
public class ClanEnergyTip extends ClanTip {

    @Inject
    public ClanEnergyTip(Clans clans) {
        super(clans, 2, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanenergy";
    }

    @Override
    public Component generateComponent() {
        return Component.empty().append(Component.text("You can buy energy in the clan menu or by clicking ", NamedTextColor.GRAY))
                .append(Component.text("here", NamedTextColor.YELLOW).decoration(TextDecoration.UNDERLINED, true)
                        .clickEvent(ClickEvent.runCommand("/c energyshop"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open the clan shop", NamedTextColor.GRAY))
                        ));
    }
    public  boolean isValid(Player player, Clan clan) {
        return clan != null;
    }

}
