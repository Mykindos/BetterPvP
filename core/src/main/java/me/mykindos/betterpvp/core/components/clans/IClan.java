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

    /**
     * Retrieves the unique identifier of the clan.
     *
     * @return the UUID representing the clan's unique identifier
     */
    UUID getId();
    /**
     * Retrieves the name of the clan.
     *
     * @return the name of the clan as a String
     */
    String getName();

    /**
     * Determines if the current clan has administrative privileges.
     *
     * @return true if the clan has administrative privileges, false otherwise
     */
    boolean isAdmin();

    /**
     * Sends a message to all members of the clan.
     *
     * @param message The message string to be sent to all members of the clan.
     * @param ignore The UUID of a member to exclude from receiving the message; can be null to include everyone.
     * @param prefix Indicates whether the message should include a prefix. If true, a prefix will be added.
     */
    void messageClan(String message, UUID ignore, boolean prefix);

    /**
     * Retrieves the list of members in the clan.
     *
     * @return a list of {@code ClanMember} objects representing the members of the clan.
     */
    List<ClanMember> getMembers();
    /**
     * Retrieves a list of all alliances associated with the current clan.
     *
     * @return a list of ClanAlliance objects representing the alliances of the current clan
     */
    List<ClanAlliance> getAlliances();
    /**
     * Retrieves the list of enemies associated with the clan. Each enemy is represented
     * by a {@link ClanEnemy} object, containing the enemy clan and their respective dominance level.
     *
     * @return a list of {@code ClanEnemy} instances representing the enemies of the clan
     */
    List<ClanEnemy> getEnemies();
    /**
     * Retrieves the list of territories owned by the clan.
     *
     * @return a list of {@code ClanTerritory} objects representing the territories owned by the clan.
     */
    List<ClanTerritory> getTerritory();

    /**
     * Retrieves a list of online players corresponding to the members of the clan.
     * Converts each clan member's UUID to an online Player instance using Bukkit's player management system.
     * Filters out members who are not currently online.
     *
     * @return a list of online players representing the members of the clan, or an empty list if no members are online.
     */
    default List<Player> getMembersAsPlayers() {
        return getMembers().stream()
                .map(member -> Bukkit.getPlayer(UUID.fromString(member.getUuid())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Converts the clan members into an Audience object, which represents a group of players who can
     * receive messages or interact with the system in some way through the Adventure API.
     *
     * @return an {@link Audience} instance containing all eligible players in the clan.
     */
    default Audience asAudience() {
        return Audience.audience(getMembersAsPlayers());
    }

    /**
     * Retrieves the count of squads associated with this clan.
     *
     * @return The number of squads in the clan.
     */
    int getSquadCount();

    /**
     * Determines if the specified clan is an ally of the current clan.
     *
     * @param clan the {@code IClan} instance to check for an alliance
     * @return {@code true} if the specified clan is an ally, otherwise {@code false}
     */
    default boolean isAllied(IClan clan) {
        return getAlliances().stream().anyMatch(ally -> ally.getClan().equals(clan));
    }

    /**
     * Retrieves the ClanAlliance associated with the specified clan, if such an alliance exists.
     *
     * @param clan the clan to search for an alliance with
     * @return an Optional containing the ClanAlliance if found, otherwise an empty Optional
     */
    default Optional<ClanAlliance> getAlliance(IClan clan) {
        return getAlliances().stream().filter(ally -> ally.getClan().getName().equalsIgnoreCase(clan.getName())).findFirst();
    }

    /**
     * Determines if the given clan has a trusted relationship with this clan.
     *
     * @param clan The clan to check for a trusted relationship.
     * @return true if the given clan has a trusted relationship with this clan, false otherwise.
     */
    default boolean hasTrust(IClan clan) {
        Optional<ClanAlliance> allianceOptional = getAlliance(clan);
        return allianceOptional.map(ClanAlliance::isTrusted).orElse(false);
    }

    /**
     * Determines if the specified clan is considered an enemy by this clan.
     *
     * @param clan the clan to check against this clan's enemy list
     * @return true if the specified clan is an enemy of this clan, false otherwise
     */
    default boolean isEnemy(IClan clan) {
        return getEnemies().stream().anyMatch(enemy -> enemy.getClan().getName().equalsIgnoreCase(clan.getName()));
    }

    /**
     * Retrieves the enemy information for a given clan.
     *
     * @param clan The clan to check for an enemy relationship.
     * @return An {@code Optional} containing the {@code ClanEnemy} object associated with the given clan,
     *         or an empty {@code Optional} if the provided clan is not an enemy.
     */
    default Optional<ClanEnemy> getEnemy(IClan clan) {
        return getEnemies().stream().filter(enemy -> enemy.getClan().getName().equalsIgnoreCase(clan.getName())).findFirst();
    }

    /**
     * Retrieves the number of online members in the clan by checking if
     * the players corresponding to the UUIDs of the members are currently active.
     *
     * @return the total count of online members in the clan.
     */
    default int getOnlineMemberCount() {
        return (int) getMembers().stream().filter(clanMember -> Bukkit.getPlayer(UUID.fromString(clanMember.getUuid()))!= null).count();
    }

    /**
     * Checks if the clan is currently online.
     *
     * @return true if the clan is online, false otherwise.
     */
    boolean isOnline();

}
