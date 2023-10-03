package me.mykindos.betterpvp.clans.clans;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.events.ClanPropertyUpdateEvent;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Slf4j
@Data
public class Clan extends PropertyContainer implements IClan, Invitable, IMapListener {

    private final UUID id;
    private String name;
    private Location home;
    private boolean admin;
    private boolean safe;
    private boolean online;

    private List<ClanMember> members = new ArrayList<>();
    private List<ClanAlliance> alliances = new ArrayList<>();
    private List<ClanEnemy> enemies = new ArrayList<>();
    private List<ClanTerritory> territory = new ArrayList<>();
    private List<Insurance> insurance = Collections.synchronizedList(new ArrayList<>());

    public long getTimeCreated() {
        return (long) getProperty(ClanProperty.TIME_CREATED).orElse(0);
    }

    public long getLastLogin() {
        return (long) getProperty(ClanProperty.LAST_LOGIN).orElse(0);
    }

    public int getEnergy() {
        return (int) getProperty(ClanProperty.ENERGY).orElse(9999);
    }

    public int getPoints() {
        return (int) getProperty(ClanProperty.POINTS).orElse(0);
    }

    public int getLevel() {
        return (int) getProperty(ClanProperty.LEVEL).orElse(1);
    }

    /**
     * While a clan is on cooldown, they cannot gain or lose any dominance to other clans
     *
     * @return The time the cooldown expires (epoch)
     */
    public long getNoDominanceCooldown() {
        return (long) getProperty(ClanProperty.NO_DOMINANCE_COOLDOWN).orElse(0);
    }

    public boolean isNoDominanceCooldownActive() {
        return getNoDominanceCooldown() - System.currentTimeMillis() <= 0;
    }

    public long getLastTntedTime() {
        return (long) getProperty(ClanProperty.LAST_TNTED).orElse(0);
    }

    public int getBalance() {
        return (int) getProperty(ClanProperty.BALANCE).orElse(0);
    }

    public void setBalance(int balance) {
        putProperty(ClanProperty.BALANCE, balance);
    }

    public void setEnergy(int energy) {
        putProperty(ClanProperty.ENERGY, energy);
    }

    public Optional<ClanMember> getLeader() {
        return members.stream().filter(clanMember -> clanMember.getRank() == ClanMember.MemberRank.LEADER).findFirst();
    }

    public String getAge() {
        return UtilTime.getTime(System.currentTimeMillis() - getTimeCreated(), UtilTime.TimeUnit.BEST, 1);
    }

    /**
     * Only use this method if you already acquired the Clan using the player
     *
     * @param uuid UUID of the member
     * @return ClanMember
     */
    @NotNull
    public ClanMember getMember(UUID uuid) {
        return getMemberByUUID(uuid).orElseThrow();
    }

    public Optional<ClanMember> getMemberByUUID(UUID uuid) {
        return getMemberByUUID(uuid.toString());
    }

    public Optional<ClanMember> getMemberByUUID(String uuid) {
        return members.stream().filter(clanMember -> clanMember.getUuid().equalsIgnoreCase(uuid)).findFirst();
    }

    public List<Player> getMembersAsPlayers() {
        List<Player> players = new ArrayList<>();
        for (ClanMember member : getMembers()) {
            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if (player != null) {
                players.add(player);
            }
        }

        return players;
    }

    public List<Player> getAdminsAsPlayers() {
        List<Player> playerAdmins = getMembersAsPlayers();
        playerAdmins.forEach(player -> {
            if (!getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
                playerAdmins.remove(player);
            }
        });
        return playerAdmins;
    }

    /**
     * @return The total amount of members in an entire alliance
     */
    public int getSquadCount() {
        int count = 0;

        count += getMembers().size();

        for (ClanAlliance alliance : getAlliances()) {
            count += alliance.getClan().getMembers().size();
        }

        return count;
    }

    /**
     * @return Returns true if any clan has 90% or more dominance on this clan
     */
    public boolean isAlmostPillaged() {
        return getEnemies().stream().anyMatch(enemy -> enemy.getClan().getEnemy(this).orElseThrow().getDominance() >= 90);
    }


    public String getDominanceString(IClan clan) {
        Optional<ClanEnemy> enemyOptional = getEnemy(clan);
        Optional<ClanEnemy> theirEnemyOptional = clan.getEnemy(this);
        if (enemyOptional.isPresent() && theirEnemyOptional.isPresent()) {

            ClanEnemy enemy = enemyOptional.get();
            ClanEnemy theirEnemy = theirEnemyOptional.get();

            String text;
            if (enemy.getDominance() > 0) {
                text = "<green>" + enemy.getDominance() + "%";
            } else if (theirEnemy.getDominance() > 0) {
                text = "<red>" + theirEnemy.getDominance() + "%";
            } else {
                return "";
            }
            return "<gray> (" + text + "<gray>)";
        }
        return "";
    }

    public Component getSimpleDominanceString(IClan clan) {
        Optional<ClanEnemy> enemyOptional = getEnemy(clan);
        Optional<ClanEnemy> theirEnemyOptional = clan.getEnemy(this);
        if (enemyOptional.isPresent() && theirEnemyOptional.isPresent()) {

            ClanEnemy enemy = enemyOptional.get();
            ClanEnemy theirEnemy = theirEnemyOptional.get();

            if (theirEnemy.getDominance() == 0 && enemy.getDominance() == 0) {
                return Component.text(" 0", NamedTextColor.WHITE);
            }
            if (theirEnemy.getDominance() > 0) {
                return Component.text(" " + theirEnemy.getDominance() + "%", NamedTextColor.GREEN);
            } else {
                return Component.text(" " + enemy.getDominance() + "%", NamedTextColor.RED);
            }

        }
        return Component.empty();
    }

    /**
     * Send message to all online clan members
     *
     * @param message The message to send
     * @param ignore  Ignore a specific clan member (perhaps the creator)
     * @param prefix  Whether to add a prefix or not. 'Clans>'
     */
    @Override
    public void messageClan(String message, UUID ignore, boolean prefix) {
        members.forEach(member -> {
            if (ignore != null && ignore.toString().equalsIgnoreCase(member.getUuid())) return;

            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if (player != null) {
                UtilMessage.simpleMessage(player, prefix ? "Clans" : "", message);
            }

        });
    }

    public String getEnergyTimeRemaining() {

        if (getTerritory().isEmpty()) {
            return "\u221E";
        }
        return UtilTime.getTime(getEnergyRatio() * 3600000, UtilTime.TimeUnit.BEST, 2);

    }

    public double getEnergyRatio() {
        return getEnergy() / (float) (getTerritory().size() * 25);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Clan otherClan) {
            return getName().equalsIgnoreCase(otherClan.getName());
        }

        return false;
    }

    @Override
    public void saveProperty(String key, Object object, boolean updateScoreboard) {
        properties.put(key, object);
        if (updateScoreboard) {
            getMembersAsPlayers().forEach(player -> UtilServer.callEvent(new ScoreboardUpdateEvent(player)));
        }
    }

    @Override
    public void onMapValueChanged(String key, Object value) {
        try {
            ClanProperty property = ClanProperty.valueOf(key);
            if (property.isSaveProperty()) {
                UtilServer.callEvent(new ClanPropertyUpdateEvent(this, key, value));
            }
        } catch (IllegalArgumentException ex) {
            log.error("Could not find a ClanProperty named {}", key, ex);
        }
    }
}
