package me.mykindos.betterpvp.champions.stats.repository.weapon;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import me.mykindos.betterpvp.champions.stats.ChampionsKill;
import me.mykindos.betterpvp.core.items.BPvPItem;

@Data
public class ChampionsKillItemData {
    final ChampionsKill kill;
    final Map<UUID, Set<BPvPItem.SerializedItem>> playerItems = new HashMap<>();
}
