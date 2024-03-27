package me.mykindos.betterpvp.clans.logging.types;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.logging.FormattedLog;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class FormattedClanLog extends FormattedLog {
    @Nullable
    protected OfflinePlayer offlinePlayer1;
    @Nullable
    protected Clan clan1;
    @Nullable
    protected OfflinePlayer offlinePlayer2;
    @Nullable
    protected Clan clan2;
    protected ClanLogType type;

    public FormattedClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable Clan clan1, @Nullable OfflinePlayer offlinePlayer2, @Nullable Clan clan2, ClanLogType type) {
        super(time);
        this.offlinePlayer1 = offlinePlayer1;
        this.clan1 = clan1;
        this.offlinePlayer2 = offlinePlayer2;
        this.clan2 = clan2;
        this.type = type;
    }

    @Override
    public Component getComponent() {
        return super.getComponent().append(UtilMessage.deserialize("<yellow>%s</yellow> <aqua>%s</aqua> <white>%s</white> <yellow>%s</yellow> <aqua>%s</aqua>",
                offlinePlayer1 == null ? null : offlinePlayer1.getName(),
                clan1 == null ? null : clan1.getName(),
                type.name(),
                offlinePlayer2 == null ? null : offlinePlayer2.getName(),
                clan2 == null ? null : clan2.getName()
                ));
    }
}
