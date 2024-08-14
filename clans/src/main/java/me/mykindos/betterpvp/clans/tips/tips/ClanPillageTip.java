package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class ClanPillageTip extends ClanTip {

    private final ClanManager clanManager;

    @Inject
    public ClanPillageTip(Clans clans, ClanManager clanManager) {
        super(clans, 2, 1);
        this.clanManager = clanManager;
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanpillage";
    }

    @Override
    public Component generateComponent() {
        return Component.empty().append(UtilMessage.deserialize("Upon reaching <yellow>100</yellow>% dominance on an <red>enemy</red> clan, " +
                "your clan will be able to pillage that clan. This will allow you to attack them and potentially gain valuable loot"));
        // TODO make this more descriptive of the pillage process
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null && clanManager.isPillageEnabled();

    }

}
