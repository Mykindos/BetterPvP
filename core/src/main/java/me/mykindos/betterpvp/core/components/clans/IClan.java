package me.mykindos.betterpvp.core.components.clans;

import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public interface IClan {

    UUID getId();
    String getName();

    boolean isAdmin();

    void messageClan(String message, UUID ignore, boolean prefix);

    List<ClanMember> getMembers();
    List<ClanAlliance> getAlliances();
    List<ClanEnemy> getEnemies();
    List<ClanTerritory> getTerritory();

    default List<Player> getMembersAsPlayers() {
        return getMembers().stream()
                .map(member -> Bukkit.getPlayer(UUID.fromString(member.getUuid())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    default Audience asAudience() {
        return Audience.audience(getMembersAsPlayers());
    }

    int getSquadCount();

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

    default Optional<ClanEnemy> getEnemy(IClan clan) {
        return getEnemies().stream().filter(enemy -> enemy.getClan().getName().equalsIgnoreCase(clan.getName())).findFirst();
    }

    default int getOnlineMemberCount() {
        return (int) getMembers().stream().filter(clanMember -> Bukkit.getPlayer(UUID.fromString(clanMember.getUuid()))!= null).count();
    }

    boolean isOnline();

}
