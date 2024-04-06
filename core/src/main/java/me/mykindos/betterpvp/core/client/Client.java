package me.mykindos.betterpvp.core.client;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.redis.CacheObject;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode(callSuper = false, of = {"uuid"})
public class Client extends PropertyContainer implements IMapListener, CacheObject, Unique {

    private final transient @NotNull Gamer gamer;
    private final @NotNull String uuid;
    private @NotNull String name;
    private @NotNull Rank rank;
    private long connectionTime;
    private final List<Punishment> punishments;

    boolean administrating;
    boolean online;
    boolean newClient;

    public Client(@NotNull Gamer gamer, @NotNull String uuid, @NotNull String name, @NotNull Rank rank) {
        this.gamer = gamer;
        this.uuid = uuid;
        this.name = name;
        this.rank = rank;
        this.connectionTime = System.currentTimeMillis();
        this.punishments = new ArrayList<>();
        properties.registerListener(this);
    }

    public boolean isLoaded() {
        final Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        return player != null && player.isOnline();
    }

    public UUID getUniqueId() {
        return UUID.fromString(uuid);
    }

    public boolean hasRank(Rank rank) {
        return this.rank.getId() >= rank.getId();
    }

    @Override
    public void saveProperty(String key, Object object) {
        properties.put(key, object);
    }

    @Override
    public void onMapValueChanged(String key, Object value) {
        UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> UtilServer.callEvent(new ClientPropertyUpdateEvent(this, key, value)));
    }

    public boolean hasPunishment(String punishmentType) {
        return hasPunishment(PunishmentTypes.getPunishmentType(punishmentType));
    }

    public boolean hasPunishment(IPunishmentType punishmentType) {
        return getPunishment(punishmentType).isPresent();
    }

    public Optional<Punishment> getPunishment(IPunishmentType punishmentType) {
        if (punishmentType == null) return Optional.empty();
        Optional<Punishment> permanentPunishment = punishments.stream()
                .filter(punishment -> punishment.isActive() && punishment.getType() == punishmentType && punishment.getExpiryTime() == -1)
                .findFirst();

        if (permanentPunishment.isPresent()) {
            return permanentPunishment;
        } else {
            return punishments.stream()
                    .filter(punishment -> punishment.isActive() && punishment.getType() == punishmentType)
                    .max(Comparator.comparingLong(Punishment::getExpiryTime));
        }
    }

    @Override
    public String getKey() {
        return uuid;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public void copy(Client other) {
        this.name = other.name;
        this.rank = other.rank;
        this.administrating = other.administrating;
        this.online = this.online || other.online;
        this.newClient = other.newClient;
        this.properties.getMap().clear();
        this.properties.getMap().putAll(other.properties.getMap());
        this.punishments.clear();
        this.punishments.addAll(other.punishments);
    }
}
