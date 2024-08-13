package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import lombok.Getter;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import java.util.Map;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Set;

@Singleton
public class DaggerDataManager {

    @Getter
    private final Map<Player, DaggerData> daggerDataMap = new HashMap<>();

    @Getter
    private static final DaggerDataManager instance = new DaggerDataManager();

    private DaggerDataManager() {}

    public void setDaggerData(Player player, DaggerData daggerData) {
        DaggerData existingData = daggerDataMap.put(player, daggerData);
        if (existingData != null && existingData.getSwordDisplay() != null) {
            existingData.getSwordDisplay().remove();
        }
    }

    public DaggerData getDaggerData(Player player) {
        return daggerDataMap.get(player);
    }

    public void removeDaggerData(Player player) {
        DaggerData existingData = daggerDataMap.remove(player);
        if (existingData != null && existingData.getSwordDisplay() != null) {
            existingData.getSwordDisplay().remove();
        }
    }

    public Set<Player> getAllPlayers() {
        return daggerDataMap.keySet();
    }
}
