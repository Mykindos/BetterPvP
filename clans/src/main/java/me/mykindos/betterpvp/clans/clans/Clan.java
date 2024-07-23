package me.mykindos.betterpvp.clans.clans;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Data;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.events.ClanPropertyUpdateEvent;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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

    private final UUID id;
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

    public static double getExperienceForLevel(final long level) {
        return Math.pow(level, 2) - 1;
    }

    public static long getLevelFromExperience(final double experience) {
        return (long) Math.sqrt(experience + 1d);
    }

    public long getTimeCreated() {
        return (long) this.getProperty(ClanProperty.TIME_CREATED).orElse(0L);
    }

    public long getLastLogin() {
        return (long) this.getProperty(ClanProperty.LAST_LOGIN).orElse(0L);
    }

    public int getEnergy() {
        return (int) this.getProperty(ClanProperty.ENERGY).orElse(9999);
    }

    public void setEnergy(final int energy) {
        this.saveProperty(ClanProperty.ENERGY.name(), energy);
    }

    public int getPoints() {
        return (int) this.getProperty(ClanProperty.POINTS).orElse(0);
    }

    /**
     * While a clan is on cooldown, they cannot gain or lose any dominance to other clans
     *
     * @return The time the cooldown expires (epoch)
     */
    public long getNoDominanceCooldown() {
        return (long) this.getProperty(ClanProperty.NO_DOMINANCE_COOLDOWN).orElse(0L);
    }

    public boolean isNoDominanceCooldownActive() {
        return (this.getNoDominanceCooldown() - System.currentTimeMillis() >= 0);
    }

    public int getBalance() {
        return (int) this.getProperty(ClanProperty.BALANCE).orElse(0);
    }

    public void setBalance(final int balance) {
        this.putProperty(ClanProperty.BALANCE, balance);
    }

    public void grantEnergy(final int energy) {
        this.saveProperty(ClanProperty.ENERGY.name(), this.getEnergy() + energy);
    }

    public Optional<ClanMember> getLeader() {
        return this.members.stream().filter(clanMember -> clanMember.getRank() == ClanMember.MemberRank.LEADER).findFirst();
    }

    public String getAge() {
        return UtilTime.getTime((System.currentTimeMillis() - this.getTimeCreated()), 1);
    }

    /**
     * Only use this method if you already acquired the Clan using the player
     *
     * @param uuid UUID of the member
     * @return ClanMember
     */
    @NotNull
    public ClanMember getMember(final UUID uuid) {
        return this.getMemberByUUID(uuid).orElseThrow();
    }

    public Optional<ClanMember> getMemberByUUID(final UUID uuid) {
        return this.getMemberByUUID(uuid.toString());
    }

    public Optional<ClanMember> getMemberByUUID(final String uuid) {
        return this.members.stream().filter(clanMember -> clanMember.getUuid().equalsIgnoreCase(uuid)).findFirst();
    }

    public List<Player> getAdminsAsPlayers() {
        return this.getMembers().stream()
                .filter(member -> member.getRank().hasRank(ClanMember.MemberRank.ADMIN) )
                .map(member -> Bukkit.getPlayer(UUID.fromString(member.getUuid())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * @return The total amount of members in an entire alliance
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

    public double getExperience() {
        return (double) this.getProperty(ClanProperty.EXPERIENCE).orElse(0d);
    }

    public void setExperience(final double experience) {
        this.saveProperty(ClanProperty.EXPERIENCE.name(), experience);
    }

    public void grantExperience(final double experience) {
        Preconditions.checkArgument(experience > 0, "Experience must be greater than 0");
        this.saveProperty(ClanProperty.EXPERIENCE.name(), this.getExperience() + experience);
    }

    public long getLevel() {
        return getLevelFromExperience(this.getExperience());
    }

    /**
     * @return Returns true if any clan has 90% or more dominance on this clan
     */
    public boolean isAlmostPillaged() {
        return this.getEnemies().stream().anyMatch(enemy -> enemy.getClan().getEnemy(this).orElseThrow().getDominance() >= 90);
    }

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
     * Send message to all online clan members
     *
     * @param message The message to send
     * @param ignore  Ignore a specific clan member (perhaps the creator)
     * @param prefix  Whether to add a prefix or not. 'Clans>'
     */
    @Override
    public void messageClan(final String message, final UUID ignore, final boolean prefix) {
        this.members.forEach(member -> {
            if (ignore != null && ignore.toString().equalsIgnoreCase(member.getUuid())) {
                return;
            }

            final Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if (player != null) {
                UtilMessage.simpleMessage(player, prefix ? "Clans" : "", message);
            }

        });
    }

    public void clanChat(final Player player, final String message) {
        final String playerName = UtilFormat.spoofNameForLunar(player.getName());
        final String messageToSend = "<aqua>" + playerName + " <dark_aqua>" + message;
        this.messageClan(messageToSend, null, false);
    }

    public void allyChat(final Player player, final String message) {
        final String playerName = UtilFormat.spoofNameForLunar(player.getName());
        final String messageToSend = "<dark_green>" + playerName + " <green>" + message;

        this.getAlliances().forEach(alliance -> {
            alliance.getClan().messageClan(messageToSend, null, false);
        });

        this.messageClan(messageToSend, null, false);
    }

    public String getEnergyTimeRemaining() {
        if (this.getTerritory().isEmpty()) {
            return "\u221E";
        }

        return UtilTime.getTime((this.getEnergy() / this.getEnergyDepletionRatio()) * 3600000, 2);
    }

    /**
     * @return The amount of energy a clan will lose per hour
     */
    public double getEnergyDepletionRatio() {
        return this.getTerritory().size() * 25d;
    }

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

    public boolean isChunkOwnedByClan(final String chunkString) {
        return this.getTerritory().stream().anyMatch(claim -> claim.getChunk().equalsIgnoreCase(chunkString));
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof final Clan otherClan) {
            return this.getName().equalsIgnoreCase(otherClan.getName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public void saveProperty(final String key, final Object object) {
        this.properties.put(key, object);
    }

    @Override
    public void onMapValueChanged(final String key, final Object value) {
        try {
            final ClanProperty property = ClanProperty.valueOf(key);
            if (property.isSaveProperty()) {
                UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> UtilServer.callEvent(new ClanPropertyUpdateEvent(this, key, value)));
            }
        } catch (final IllegalArgumentException ex) {
            log.error("Could not find a ClanProperty named {}", key, ex).submit();
        }
    }
}
