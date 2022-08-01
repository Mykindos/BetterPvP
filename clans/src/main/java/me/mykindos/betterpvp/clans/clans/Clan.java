package me.mykindos.betterpvp.clans.clans;

import lombok.Builder;
import lombok.Data;
import me.mykindos.betterpvp.clans.clans.components.ClanAlliance;
import me.mykindos.betterpvp.clans.clans.components.ClanEnemy;
import me.mykindos.betterpvp.clans.clans.components.ClanMember;
import me.mykindos.betterpvp.clans.clans.components.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
public class Clan {

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

    @Builder.Default
    private List<ClanMember> members = new ArrayList<>();

    @Builder.Default
    private List<ClanAlliance> alliances = new ArrayList<>();

    @Builder.Default
    private List<ClanEnemy> enemies = new ArrayList<>();

    @Builder.Default
    private List<ClanTerritory> territory = new ArrayList<>();

    public Optional<ClanMember> getLeader() {
        return members.stream().filter(clanMember -> clanMember.getRank() == ClanMember.MemberRank.OWNER).findFirst();
    }

    public Optional<ClanMember> getMemberByUUID(String uuid){
        return members.stream().filter(clanMember -> clanMember.getUuid().equalsIgnoreCase(uuid)).findFirst();
    }

    public boolean isAllied(Clan clan) {
        return alliances.stream().anyMatch(ally -> ally.getOtherClan().equalsIgnoreCase(clan.getName()));
    }

    public Optional<ClanAlliance> getAlliance(Clan clan) {
        return alliances.stream().filter(ally -> ally.getOtherClan().equalsIgnoreCase(clan.getName())).findFirst();
    }

    public boolean hasTrust(Clan clan) {
        Optional<ClanAlliance> allianceOptional = getAlliance(clan);
        return allianceOptional.map(ClanAlliance::isTrusted).orElse(false);
    }

    public boolean isEnemy(Clan clan) {
        for (ClanEnemy enemy : getEnemies()) {
            if (enemy.getOtherClan().equalsIgnoreCase(clan.getName())) {
                return true;
            }

        }
        return false;
    }

    public ClanEnemy getEnemy(Clan clan) {
        for (ClanEnemy enemy : getEnemies()) {
            if (enemy.getOtherClan().equalsIgnoreCase(clan.getName())) {
                return enemy;
            }

        }
        return null;
    }

    public String getDominanceString(Clan clan) {
        ClanEnemy enemy = getEnemy(clan);
        ClanEnemy theirEnemy = clan.getEnemy(this);
        if (enemy != null && theirEnemy != null) {
            return ChatColor.GRAY + "(" + ChatColor.GREEN + theirEnemy.getDominance() + ChatColor.GRAY + ":"
                    + ChatColor.RED + enemy.getDominance() + ChatColor.GRAY + ")" + ChatColor.GRAY;
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
                return ChatColor.DARK_RED + " -" + (theirEnemy.getDominance());
            } else {

                return ChatColor.GREEN + " +" + (enemy.getDominance());
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
    public void messageClan(String message, UUID ignore, boolean prefix) {
        members.forEach(member -> {
            if(ignore != null && ignore.toString().equalsIgnoreCase(member.getUuid())) return;

            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if(player != null) {
                UtilMessage.message(player, prefix ? "Clans" : "", message);
            }

        });
    }

    public String getEnergyTimeRemaining() {

        if (getTerritory().isEmpty()) {
            return "∞";
        }
        return UtilTime.getTime((getEnergy() / (float) (getTerritory().size() * 25)) * 3600000, UtilTime.TimeUnit.BEST, 2);

    }

}
