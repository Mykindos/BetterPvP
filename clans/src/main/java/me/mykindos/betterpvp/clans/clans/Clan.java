package me.mykindos.betterpvp.clans.clans;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.events.ClanPropertyUpdateEvent;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerColor;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

@Slf4j
@Data
public class Clan extends PropertyContainer implements IClan, Invitable, IMapListener {

    public static long getExperienceForLevel(long level) {
        return (long) Math.pow(level, 2) - 1;
    }

    public static long getLevelFromExperience(long experience) {
        return (long) Math.sqrt(experience + 1d);
    }

    private final UUID id;
    private String name;
    private Location home;
    private boolean admin;
    private boolean safe;
    private boolean online;
    private BannerWrapper banner = BannerWrapper.builder().baseColor(BannerColor.WHITE).build();
    private ClanVault vault = ClanVault.create(this);

    private List<ClanMember> members = new ArrayList<>();
    private List<ClanAlliance> alliances = new ArrayList<>();
    private List<ClanEnemy> enemies = new ArrayList<>();
    private List<ClanTerritory> territory = new ArrayList<>();
    private List<Insurance> insurance = Collections.synchronizedList(new ArrayList<>());

    private BukkitTask tntRecoveryRunnable = null;

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

    /**
     * While a clan is on cooldown, they cannot gain or lose any dominance to other clans
     *
     * @return The time the cooldown expires (epoch)
     */
    public long getNoDominanceCooldown() {
        return (long) getProperty(ClanProperty.NO_DOMINANCE_COOLDOWN).orElse(0);
    }

    public boolean isNoDominanceCooldownActive() {
        return (getNoDominanceCooldown() - System.currentTimeMillis() >= 0);
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
        saveProperty(ClanProperty.ENERGY.name(), energy, true);
    }

    public Optional<ClanMember> getLeader() {
        return members.stream().filter(clanMember -> clanMember.getRank() == ClanMember.MemberRank.LEADER).findFirst();
    }

    public String getAge() {
        return UtilTime.getTime(System.currentTimeMillis() - getTimeCreated(), 1);
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



    public void updateScoreboards() {
        getMembersAsPlayers().forEach(player -> UtilServer.callEvent(new ScoreboardUpdateEvent(player)));
    }

    public List<Player> getAdminsAsPlayers() {
        return getMembers().stream()
                .filter(member -> member.getRank().hasRank(ClanMember.MemberRank.ADMIN) )
                .map(member -> Bukkit.getPlayer(UUID.fromString(member.getUuid())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

    public long getExperience() {
        return (long) getProperty(ClanProperty.EXPERIENCE).orElse(0L);
    }

    public void grantExperience(long experience) {
        Preconditions.checkArgument(experience > 0, "Experience must be greater than 0");
        saveProperty(ClanProperty.EXPERIENCE.name(), getExperience() + experience, false);
    }

    public void setExperience(long experience) {
        saveProperty(ClanProperty.EXPERIENCE.name(), experience, false);
    }

    public long getLevel() {
        return getLevelFromExperience(getExperience());
    }

    /**
     * @return Returns true if any clan has 90% or more dominance on this clan
     */
    public boolean isAlmostPillaged() {
        return getEnemies().stream().anyMatch(enemy -> enemy.getClan().getEnemy(this).orElseThrow().getDominance() >= 90);
    }

    public int getOnlineEnemyCount() {
        int onlineCount = 0;
        List<ClanEnemy> enemies = getEnemies();

        for (ClanEnemy enemy : enemies) {
            if (enemy.getClan().isOnline()) {
                onlineCount++;
            }
        }

        return onlineCount;
    }

    public int getOnlineAllyCount() {
        int onlineCount = 0;
        List<ClanAlliance> alliances = getAlliances();

        for (ClanAlliance alliance : alliances) {
            if (alliance.getClan().isOnline()) {
                onlineCount++;
            }
        }

        return onlineCount;
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

    public void clanChat(Player player, String message) {
        String playerName = UtilFormat.spoofNameForLunar(player.getName());
        String messageToSend = "<aqua>" + playerName + " <dark_aqua>" + message;
        messageClan(messageToSend, null, false);
    }

    public void allyChat(Player player, String message) {
        String playerName = UtilFormat.spoofNameForLunar(player.getName());
        String messageToSend = "<dark_green>" + playerName + " <green>" + message;

        getAlliances().forEach(alliance -> {
            alliance.getClan().messageClan(messageToSend, null, false);
        });

        messageClan(messageToSend, null, false);
    }

    public String getEnergyTimeRemaining() {

        if (getTerritory().isEmpty()) {
            return "\u221E";
        }
        return UtilTime.getTime((getEnergy() / getEnergyRatio()) * 3600000, 2);

    }

    /**
     * @return The amount of energy a clan will lose per hour
     */
    public double getEnergyRatio() {
        return getTerritory().size() * 25d;
    }

    public ClanRelation getRelation(@Nullable Clan targetClan) {
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

    @Override
    public boolean equals(Object other) {
        if (other instanceof Clan otherClan) {
            return getName().equalsIgnoreCase(otherClan.getName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
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
                UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> UtilServer.callEvent(new ClanPropertyUpdateEvent(this, key, value)));
            }
        } catch (IllegalArgumentException ex) {
            log.error("Could not find a ClanProperty named {}", key, ex);
        }
    }
}
