package me.mykindos.betterpvp.core.components.clans;

import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IClan {

    int getId();
    String getName();

    boolean isAdmin();

    void messageClan(String message, UUID ignore, boolean prefix);

    List<ClanMember> getMembers();
    List<ClanAlliance> getAlliances();
    List<ClanEnemy> getEnemies();
    List<ClanTerritory> getTerritory();

    default boolean isAllied(IClan clan) {
        return getAlliances().stream().anyMatch(ally -> ally.getClan().equals(clan));
    }

    default Optional<ClanAlliance> getAlliance(IClan clan) {
        return getAlliances().stream().filter(ally -> ally.getClan().getName().equalsIgnoreCase(clan.getName())).findFirst();
    }

    default boolean hasTrust(IClan clan) {
        Optional<ClanAlliance> allianceOptional = getAlliance(clan);
        return allianceOptional.map(ClanAlliance::isTrusted).orElse(false);
    }

    default boolean isEnemy(IClan clan) {
        return getEnemies().stream().anyMatch(enemy -> enemy.getClan().getName().equalsIgnoreCase(clan.getName()));
    }



}
