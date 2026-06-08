package me.mykindos.betterpvp.clans.logging;

import lombok.Data;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

@Data
public class KillClanLog {

    private final String killerName;
    private final long killer;
    private final String killerClanName;
    @Nullable
    private final Clan killerClan;

    private final String victimName;
    private final long victim;
    private final String victimClanName;
    @Nullable
    private final Clan victimClan;

    private final double dominance;
    private final long time;

    public Component getRelativeTimeComponent() {
        return Translations.component("clans.log.kill.relative-time",
                        Component.text(UtilTime.getTime((System.currentTimeMillis() - this.time), 2)))
                .color(NamedTextColor.WHITE)
                .append(Component.text(" "));
    }

    public Component getAbsoluteTimeComponent() {
        return UtilMessage.deserialize("<white>" + UtilTime.getDateTime(this.time));
    }

}
