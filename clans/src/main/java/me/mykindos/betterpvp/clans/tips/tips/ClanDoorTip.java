package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class ClanDoorTip extends ClanTip {

    @Inject
    public ClanDoorTip(Clans clans) {
        super(clans, 1, 1,
                UtilMessage.deserialize("All wooden doors and trapdoors can be crafted into an iron door or trapdoor. " +
                        "Iron Doors prevent players from glitching through them, but act just like normal doors."));
    }

    @Override
    public String getName() {
        return "clandoor";
    }

    public boolean isValid(Player player, Clan clan) {
        return true;
    }

}
