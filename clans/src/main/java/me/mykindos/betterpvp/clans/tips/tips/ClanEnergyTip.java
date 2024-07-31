package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
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
        return Component.empty().append(UtilMessage.deserialize("You can gain <aqua>Clan</aqua> <light_purple>energy</light_purple> by killing other players, completing dungeons " +
                "and raids, participating in world events, or mining in the world or at fields."));
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null;
    }

}
