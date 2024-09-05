package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.tips.types.ISuggestCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class ClanBankRaidedTip extends ClanTip implements ISuggestCommand {

    @Inject
    public ClanBankRaidedTip(Clans clans) {
        super(clans, 1, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanbankraided";
    }

    @Override
    public Component generateComponent() {

        return UtilMessage.deserialize("<reset>If you are raided by another clan, you will lose <yellow>50%<reset> of the money in your clan bank.");
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        return clan != null && clan.getBalance() > 0;
    }
}
