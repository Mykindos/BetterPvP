package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@RequiredArgsConstructor
@Getter
public class ClanBannerUpdateEvent extends CustomEvent {

    private final Clan clan;

}
