package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.locale.Translations;
import org.bukkit.entity.Player;

@Singleton
public class ClanDoorTip extends ClanTip {

    @Inject
    public ClanDoorTip(Clans clans) {
        super(clans, 1, 1, Translations.component("clans.tip.door"));
    }

    @Override
    public String getName() {
        return "clandoor";
    }

    public boolean isValid(Player player, Clan clan) {
        return true;
    }

}
