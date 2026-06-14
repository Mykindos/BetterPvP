package me.mykindos.betterpvp.clans.display;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.world.zone.Zone;
import net.kyori.adventure.text.Component;

@Getter
@Setter
@AllArgsConstructor
public class CachedHudInfo {
    int coins;
    String clan;
    Component head;
    Zone zone;
    Component rendered;
}
