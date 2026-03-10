package me.mykindos.betterpvp.clans.clans;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Data;
import me.mykindos.betterpvp.clans.clans.chat.AllianceChatChannel;
import me.mykindos.betterpvp.clans.clans.chat.ClanChatChannel;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.events.ClanPropertyUpdateEvent;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerColor;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@CustomLog
@Data
public class Clan extends PropertyContainer implements IClan, Invitable, IMapListener {

    private final long id;
    private final ClanCore core = new ClanCore(this);
    private String name;
    private boolean admin;
    private boolean safe;
    private boolean online;
    private BannerWrapper banner = BannerWrapper.builder().baseColor(BannerColor.WHITE).build();
    private List<ClanMember> members = new ArrayList<>();
    private List<ClanAlliance> alliances = new ArrayList<>();
    private List<ClanEnemy> enemies = new ArrayList<>();
    private List<ClanTerritory> territory = new ArrayList<>();
    private List<Insurance> insurance = Collections.synchronizedList(new ArrayList<>());
    private BukkitTask tntRecoveryRunnable = null;

    private IChatChannel clanChatChannel = new ClanChatChannel(this);
    private IChatChannel allianceChatChannel = new AllianceChatChannel(this);

    /**
     * Calculates the amount of experience required for a specific level.
     *
     * @param level the level for which the required experience is to be calculated
     * @return the experience required to reach the given level
     */
    public static double getExperienceForLevel(final long level) {
        return Math.pow(level, 2) - 1;
    }

    /**
     * Calculates the level based on the given experience points.
     *
     * @param experience the amount of experience points to determine the level from
     * @return the calculated level corresponding to the provided experience
     */
    public static long getLevelFromExperience(final double experience) {
        return (long) Math.sqrt(experience + 1d);
    }

    /**
     * Retrieves the time when the clan was created.
     *
     * @return The time of creation in milliseconds since epoch, or 0 if the property is not set.
     */
    public long getTimeCreated() {
        return this.getLongProperty(ClanProperty.TIME_CREATED);
    }

    /**
     * Retrieves the timestamp of the last login for the clan.
     * If the last login timestamp is not present, returns 0.
     *
     * @return the timestamp of the last login as a long value, or 0 if not present
     */
    public long getLastLogin() {
        return this.getLongProperty(ClanProperty.LAST_LOGIN);
    }

    /**
     * Retrieves the energy value of the clan.
     * If the energy property is not set, a default value of 9999 is returned.
     *
     * @return The current energy value of the clan, or 9999 if not explicitly set.
     */
    public int getEnergy() {
        return (int) this.getProperty(ClanProperty.ENERGY).orElse(9999);
    }

    /**
     * Sets the energy value for the clan. The value is capped at a maximum of 100,000.
     *
     * @param energy the energy value to be set for the clan
     */
    public void setEnergy(final int energy) {
        this.saveProperty(ClanProperty.ENERGY.name(), Math.min(100_000, energy));
    }

    /**
     * Retrieves the total points associated with the clan.
     * If the points property is not set, a default value of 0 is returned.
     *
     * @return the total points of the clan as an integer, or 0 if not set.
     */
    public int getPoints() {
        return this.getIntProperty(ClanProperty.POINTS);
    }

    /**
     * Retrieves the cooldown period during which the clan cannot establish dominance.
     *
     * @return The cooldown period in milliseconds. Returns 0 if no cooldown is set.
     */
    public long getNoDominanceCooldown() {
        return this.getLongProperty(ClanProperty.NO_DOMINANCE_COOLDOWN);
    }

    /**
     * Checks if the "No Dominance Cooldown" is currently active for the clan.
     *
     * The cooldown is considered active if the remaining time until the cooldown expires
     * is greater than or equal to zero.
     *
     * @return true if the "No Dominance Cooldown" is active, otherwise false.
     */
    public boolean isNoDominanceCooldownActive() {
        return this.getNoDominanceCooldown() - System.currentTimeMillis() >= 0;
    }

    /**
     * Retrieves the balance of the clan.
     *
     * @return the current balance of the clan, or 0 if the balance is not set.
     */
    public int getBalance() {
        return this.getIntProperty(ClanProperty.BALANCE);
    }

    /**
     * Updates the balance of the clan.
     *
     * @param balance The new balance to be set for the clan.
     */
    public void setBalance(final int balance) {
        this.putProperty(ClanProperty.BALANCE, balance);
    }

    /**
     * Grants the specified amount of energy to the clan, with a maximum cap of 100,000.
     *
     * @param energy the amount of energy to be granted to the clan. The resulting energy value will be the
     *               current energy plus the specified energy, capped at 100,000.
     */
    public void grantEnergy(final int energy) {
        this.saveProperty(ClanProperty.ENERGY.name(), Math.min(100_000, this.getEnergy() + energy));
    }

    /**
     * Retrieves the leader of the clan by filtering members with the rank of LEADER.
     *
     * @return an {@link Optional} containing the clan's leader if one exists, otherwise an empty {@link Optional}
     */
    public Optional<ClanMember> getLeader() {
        return this.members.stream().filter(clanMember -> clanMember.getRank() == ClanMember.MemberRank.LEADER).findFirst();
    }

    /**
     * Retrieves the age of the clan based on the time elapsed since its creation.
     *
     * @return A string representing the elapsed time since the clan was created, formatted in a human-readable manner.
     */
    public String getAge() {
        return UtilTime.getTime((System.currentTimeMillis() - this.getTimeCreated()), 1);
    }

    /**
     * Retrieves a {@link ClanMember} from the clan by their unique identifier (UUID).
     * If the member is not found, an exception is thrown.
     *
     * @param uuid the UUID of the member to retrieve
     * @return the {@link ClanMember} associated with the provided UUID
     */
    @NotNull
    public ClanMember getMember(final UUID uuid) {
        return this.getMemberByUUID(uuid).orElseThrow();
    }

    /**
     * Retrieves a clan member based on their UUID.
     *
     * @param uuid the UUID of the clan member to be retrieved as a string
     * @return an {@code Optional} containing the {@code ClanMember} if found,
     *         or an empty {@code Optional} if no member with the specified UUID exists
     */
    public Optional<ClanMember> getMemberByUUID(final String uuid) {
        return this.getMemberByUUID(UUID.fromString(uuid));
    }

    /**
     * Retrieves a clan member by their unique UUID.
     * Searches through the list of members and returns the first match if found.
     *
     * @param uuid the unique identifier of the clan member
     * @return an Optional containing the ClanMember if found, or an empty Optional if not
     */
    public Optional<ClanMember> getMemberByUUID(final UUID uuid) {
        return this.members.stream().filter(clanMember -> clanMember.getUuid().equals(uuid)).findFirst();
    }

    /**
     * Retrieves a list of online players who are members of the clan
     * and have an admin rank or higher.
     *
     * @return a list of {@link Player} objects representing the admins of the clan who are currently online
     */
    public List<Player> getAdminsAsPlayers() {
        return this.getMembers().stream()
                .filter(member -> member.getRank().hasRank(ClanMember.MemberRank.ADMIN))
                .map(member -> Bukkit.getPlayer(member.getUuid()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the total number of members in the squad, including members of the current clan
     * and all allied clans.
     *
     * @return the total number of members in the squad from the current clan and its alliances
     */
    @Override
    public int getSquadCount() {
        int count = 0;

        count += this.getMembers().size();

        for (final ClanAlliance alliance : this.getAlliances()) {
            count += alliance.getClan().getMembers().size();
        }

        return count;
    }

    /**
     * Retrieves the current experience of the clan.
     * If the experience property is not set, this method returns 0 as the default value.
     *
     * @return the experience value of the clan as a double. If not defined, returns 0.
     */
    public double getExperience() {
        return (double) this.getProperty(ClanProperty.EXPERIENCE).orElse(0d);
    }

    /**
     * Sets the experience value for the clan.
     *
     * @param experience the new experience value to assign to the clan
     */
    public void setExperience(final double experience) {
        this.saveProperty(ClanProperty.EXPERIENCE.name(), experience);
    }

    /**
     * Grants additional experience to the clan by adding the specified amount
     * to the current experience value.
     *
     * @param experience the amount of experience to be granted to the clan.
     *                   Must be a positive value greater than 0.
     * @throws IllegalArgumentException if the provided experience value is less than or equal to 0.
     */
    public void grantExperience(final double experience) {
        Preconditions.checkArgument(experience > 0, "Experience must be greater than 0");
        this.saveProperty(ClanProperty.EXPERIENCE.name(), this.getExperience() + experience);
    }

    /**
     * Retrieves the level of the clan based on its current experience points.
     *
     * @return the calculated level of the clan as a long value
     */
    public long getLevel() {
        return getLevelFromExperience(this.getExperience());
    }

    /**
     * Determines if the clan is almost pillaged. A clan is considered almost pillaged
     * if any of its enemies have a dominance level of 90 or higher.
     *
     * @return true if the clan is almost pillaged based on enemy dominance levels, false otherwise
     */
    public boolean isAlmostPillaged() {
        return this.getEnemies().stream().anyMatch(enemy -> enemy.getClan().getEnemy(this).orElseThrow().getDominance() >= 90);
    }

    /**
     * Calculates and returns the number of enemy clans that are currently online.
     *
     * The method retrieves the list of enemy clans associated with this clan,
     * iterates through them, and counts the number of enemy clans that are online.
     *
     * @return the number of enemy clans that are online
     */
    public int getOnlineEnemyCount() {
        int onlineCount = 0;
        final List<ClanEnemy> enemies = this.getEnemies();

        for (final ClanEnemy enemy : enemies) {
            if (enemy.getClan().isOnline()) {
                onlineCount++;
            }
        }

        return onlineCount;
    }

    /**
     * Calculates and returns the count of online allied clans.
     *
     * @return the number of allied clans that are currently online
     */
    public int getOnlineAllyCount() {
        int onlineCount = 0;
        final List<ClanAlliance> alliances = this.getAlliances();

        for (final ClanAlliance alliance : alliances) {
            if (alliance.getClan().isOnline()) {
                onlineCount++;
            }
        }

        return onlineCount;
    }


    /**
     * Sends a message to all members of the clan except the specified ignored member.
     * Prefixes the message with "Clans" if the PREFIX parameter is true.
     *
     * @param message The message to send to clan members.
     * @param ignore The UUID of the member to ignore when sending the message. Can be null.
     * @param prefix If true, adds "Clans" as a PREFIX to the message.
     */
    @Override
    public void messageClan(final String message, final UUID ignore, final boolean prefix) {
        this.members.forEach(member -> {
            if (ignore != null && ignore.equals(member.getUuid())) {
                return;
            }

            final Player player = Bukkit.getPlayer(member.getUuid());
            if (player != null) {
                UtilMessage.simpleMessage(player, prefix ? "Clans" : "", message);
            }

        });
    }

    /**
     * Retrieves the remaining energy time in a formatted string representation.
     *
     * @return a string representing the remaining time of energy based on the energy duration,
     *         formatted to two decimal places.
     */
    public String getEnergyTimeRemaining() {
        return UtilTime.getTime(getEnergyDuration(), 2);
    }

    /**
     * Calculates the duration for which the clan's energy can last based on its current energy level
     * and the rate of energy depletion.
     *
     * @return the duration in milliseconds that the clan's energy will last before being depleted.
     */
    public long getEnergyDuration() {
        return (long) ((this.getEnergy() / this.getEnergyDepletionRatio()) * 3600000);
    }

    /**
     * Calculates the energy depletion ratio based on the size of the clan's territory.
     * The ratio is determined by multiplying the size of the territory (with a minimum value of 1) by 25.
     *
     * @return the energy depletion ratio as a double, calculated as 25 multiplied by the greater of 1 or the number of territories owned by the clan.
     */
    public double getEnergyDepletionRatio() {
        return Math.max(1, this.getTerritory().size()) * 25d;
    }

    /**
     * Determines the relationship between the current clan and the specified target clan.
     *
     * @param targetClan the target clan for which the relationship is being determined.
     *                   Can be null, in which case the relation is considered neutral.
     * @return the {@link ClanRelation} representing the relationship between the current clan and the target clan.
     *         Returns {@code SELF} if the target clan is the same as the current clan,
     *         {@code ALLY} if the target clan is an ally,
     *         {@code ENEMY} if the target clan is an enemy,
     *         and {@code NEUTRAL} otherwise.
     */
    public ClanRelation getRelation(@Nullable final Clan targetClan) {
        if (targetClan == null) {
            return ClanRelation.NEUTRAL;
        }

        if (targetClan.equals(this)) {
            return ClanRelation.SELF;
        } else if (targetClan.isAllied(this)) {
            return ClanRelation.ALLY;
        } else if (targetClan.isEnemy(this)) {
            return ClanRelation.ENEMY;
        }

        return ClanRelation.NEUTRAL;
    }

    /**
     * Checks if the specified chunk is owned by the clan.
     *
     * @param chunkString the chunk identifier in string format
     * @return true if the clan owns the specified chunk, false otherwise
     */
    public boolean isChunkOwnedByClan(final String chunkString) {
        for (ClanTerritory claim : this.getTerritory()) {
            if (claim.getChunk().equalsIgnoreCase(chunkString)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares this {@code Clan} object with the specified object to determine equality.
     * Two clans are considered equal if their names are the same, ignoring case sensitivity.
     *
     * @param other the object to compare against this {@code Clan} object
     * @return {@code true} if the specified object is a {@code Clan} and has the same name (case-insensitive);
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof final Clan otherClan) {
            return this.getName().equalsIgnoreCase(otherClan.getName());
        }

        return false;
    }

    /**
     * Computes the hash code for the Clan object based on its name.
     *
     * @return the hash code of the Clan object as an integer, derived from the hash code of its name.
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * Saves a property by associating the specified key with the given object.
     * The property is stored in the internal properties map of the Clan instance.
     *
     * @param key    the key representing the property to be saved
     * @param object the value to be associated with the specified key
     */
    @Override
    public void saveProperty(final String key, final Object object) {
        this.properties.put(key, object);
    }

    /**
     * Invoked when a value in the map is changed. The method attempts to find a corresponding
     * {@link ClanProperty} for the given key and triggers appropriate actions if the property
     * is marked as a saveable property.
     *
     * @param key   the property key whose value has been updated
     * @param value the new value associated with the specified key
     */
    @Override
    public void onMapValueChanged(final String key, final Object newValue, final Object oldValue) {
        try {
            final ClanProperty property = ClanProperty.valueOf(key);
            if (property.isSaveProperty()) {
                UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> UtilServer.callEvent(new ClanPropertyUpdateEvent(this, key, newValue, oldValue)));
            }
        } catch (final IllegalArgumentException ex) {
            log.error("Could not find a ClanProperty named {}", key, ex).submit();
        }
    }

    /**
     * Clears the clan's core and removes all associated territory data.
     *
     * This method performs the following actions:
     * - Deletes the core of the clan by invoking {@link #getCore()} and its {@code deleteCore()} method,
     *   which handles clearing related resources such as the mailbox, vault contents, and the core block itself.
     * - Iterates through the clan's list of territories obtained via {@link #getTerritory()} and for each territory:
     *     - Removes the clan-related data stored in the {@code PersistentDataContainer} of the corresponding world chunk.
     * - Clears the list of territories from the clan.
     *
     * This method is typically used during operations that disband the clan or reassign its ownership,
     * ensuring that all core and territorial data related to the clan is effectively cleaned up.
     */
    public void clearTerritory() {
        getCore().deleteCore();
        getTerritory().forEach(terr -> {
            terr.getWorldChunk().getPersistentDataContainer().remove(ClansNamespacedKeys.CLAN);
        });
        getTerritory().clear();
    }
}
