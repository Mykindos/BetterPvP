package me.mykindos.betterpvp.clans.clans;

import lombok.Builder;
import lombok.Data;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

@Data
@Builder
public class Clan extends PropertyContainer implements IClan, Invitable {

    private int id;
    private String name;
    private Timestamp timeCreated;
    private Timestamp lastLogin;
    private Location home;
    private int energy;
    private int points;
    private int level;
    private long cooldown;

    private boolean admin;
    private boolean safe;

    private long lastTnted;

    private boolean online;

    @Builder.Default
    private List<ClanMember> members = new ArrayList<>();

    @Builder.Default
    private List<ClanAlliance> alliances = new ArrayList<>();

    @Builder.Default
    private List<ClanEnemy> enemies = new ArrayList<>();

    @Builder.Default
    private List<ClanTerritory> territory = new ArrayList<>();

    @Builder.Default
    private List<Insurance> insurance = Collections.synchronizedList(new ArrayList<>());

    public Optional<ClanMember> getLeader() {
        return members.stream().filter(clanMember -> clanMember.getRank() == ClanMember.MemberRank.LEADER).findFirst();
    }

    public String getAge() {
        return UtilTime.getTime(System.currentTimeMillis() - timeCreated.getTime(), UtilTime.TimeUnit.BEST, 1);
    }

    /**
     * Only use this method if you already acquired the Clan using the player
     * @param uuid UUID of the member
     * @return ClanMember
     */
    @NotNull
    public ClanMember getMember(UUID uuid){
        return getMemberByUUID(uuid).orElseThrow();
    }

    public Optional<ClanMember> getMemberByUUID(UUID uuid){
        return getMemberByUUID(uuid.toString());
    }

    public Optional<ClanMember> getMemberByUUID(String uuid){
        return members.stream().filter(clanMember -> clanMember.getUuid().equalsIgnoreCase(uuid)).findFirst();
    }

    public List<Player> getMembersAsPlayers(){
        List<Player> players = new ArrayList<>();
        for(ClanMember member : getMembers()){
            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if(player != null){
                players.add(player);
            }
        }

        return players;
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

    public ClanEnemy getEnemy(Clan clan) {
        for (ClanEnemy enemy : getEnemies()) {
            if (enemy.getClan().equals(clan)) {
                return enemy;
            }

        }
        return null;
    }


    public String getDominanceString(Clan clan) {
        ClanEnemy enemy = getEnemy(clan);
        ClanEnemy theirEnemy = clan.getEnemy(this);
        if (enemy != null && theirEnemy != null) {
            String text = "";
            if(enemy.getDominance() > 0){
                text = ChatColor.GREEN.toString() + enemy.getDominance() + "%";
            }else if(theirEnemy.getDominance() > 0) {
                text = ChatColor.RED.toString() + theirEnemy.getDominance() + "%";
            }else {
                return "";
            }
            return ChatColor.GRAY + " (" + text + ChatColor.GRAY + ")" + ChatColor.GRAY;
        }
        return "";
    }

    public String getSimpleDominanceString(Clan clan) {
        ClanEnemy enemy = getEnemy(clan);
        ClanEnemy theirEnemy = clan.getEnemy(this);

        if (enemy != null && theirEnemy != null) {
            if (theirEnemy.getDominance() == 0 && enemy.getDominance() == 0) {
                return ChatColor.WHITE + " 0";
            }
            if (theirEnemy.getDominance() > 0) {
                return ChatColor.GREEN + " " + theirEnemy.getDominance() + "%";
            } else {
                return ChatColor.DARK_RED + " " + enemy.getDominance() + "%";
            }

        }
        return "";
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
            if(ignore != null && ignore.toString().equalsIgnoreCase(member.getUuid())) return;

            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if(player != null) {
                UtilMessage.simpleMessage(player, prefix ? "Clans" : "", message);
            }

        });
    }

    public String getEnergyTimeRemaining() {

        if (getTerritory().isEmpty()) {
            return "∞";
        }
        return UtilTime.getTime((getEnergy() / (float) (getTerritory().size() * 25)) * 3600000, UtilTime.TimeUnit.BEST, 2);

    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Clan otherClan) {
            return getName().equalsIgnoreCase(otherClan.getName());
        }

        return false;
    }

    @Override
    public void saveProperty(String key, Object object, boolean updateScoreboard) {
        properties.put(key, object);
    }
}
