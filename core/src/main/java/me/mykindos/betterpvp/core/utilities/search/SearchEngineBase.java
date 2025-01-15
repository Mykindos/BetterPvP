package me.mykindos.betterpvp.core.utilities.search;

import io.netty.util.concurrent.CompleteFuture;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.exception.ClientNotLoadedException;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.manager.PlayerManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@CustomLog
public class SearchEngineBase<T> {

    private final Function<@Nullable UUID, Optional<T>> onlineSearch;
    private final BiConsumer<@Nullable UUID, Consumer<Optional<T>>> offlineUuidSearch;
    private final BiConsumer<@Nullable String, Consumer<Optional<T>>> offlineNameSearch;

    public SearchEngineBase(Function<@Nullable UUID, Optional<T>> onlineSearch,
                            BiConsumer<@Nullable UUID, Consumer<Optional<T>>> offlineUuidSearch,
                            BiConsumer<String, Consumer<Optional<T>>> offlineNameSearch) {
        this.onlineSearch = onlineSearch;
        this.offlineUuidSearch = offlineUuidSearch;
        this.offlineNameSearch = offlineNameSearch;
    }

    /**
     * Get a loaded client by {@link UUID}.
     *
     * @param uuid The {@link UUID} of the client.
     * @return The client.
     */
    public Optional<T> online(final UUID uuid) {
        return this.onlineSearch.apply(uuid);
    }

    /**
     * Get a loaded client by {@link CommandSender} instance, who must be an instance of player.
     * This client must have already been loaded, otherwise something went wrong.
     * See {@link #online(Player)} for more information.
     *
     * @param sender The player to search for.
     * @return The client.
     */
    public T online(@NotNull final CommandSender sender) {
        return this.online((Player) sender);
    }

    /**
     * Get a loaded client by {@link Player} instance.
     * If the player does not have an associated client, they will be kicked.
     * This client must have already been loaded, otherwise something went wrong.
     *
     * @param player The player to search for.
     * @return The client.
     */
    public T online(@NotNull final Player player) {
        Optional<T> online = this.online(player.getUniqueId());

        return online.orElseThrow(() -> {
            log.warn(PlayerManager.RETRIEVE_ERROR_FORMAT_SERVER, player.getName()).submit();
            player.kick(Component.text(PlayerManager.LOAD_ERROR_FORMAT_ENTITY));
            return new ClientNotLoadedException(player);
        });
    }

    /**
     * Get a loaded client by name.
     *
     * @param playerName The name of the player to search for.
     * @return The client.
     */
    public Optional<T> online(@Nullable final String playerName) {
        if (playerName == null) {
            return Optional.empty();
        }

        final Player found = Bukkit.getPlayerExact(playerName);
        if (found == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.online(found));
    }

    /**
     * Search for a loaded client by {@link UUID}, otherwise load from database.
     *
     * @param uuid           The {@link UUID} of the client to search for.
     * @param clientConsumer The callback to run when the client is found, after searched.
     */
    public void offline(@Nullable final UUID uuid, final Consumer<Optional<T>> clientConsumer, boolean async) {
        final Optional<T> clientOnline = this.online(uuid);
        if (clientOnline.isPresent()) {
            clientConsumer.accept(clientOnline);
            return;
        }

        if (async) {
            CompletableFuture.runAsync(() -> this.offlineUuidSearch.accept(uuid, clientConsumer));
        } else {
            this.offlineUuidSearch.accept(uuid, clientConsumer);
        }
    }

    /**
     * Search for an online client by exact name, otherwise load from database.
     *
     * @param playerName     The name of the player to search for.
     * @param clientConsumer The callback to run when the client is found, after searched.
     */
    public void offline(@Nullable final String playerName, final Consumer<Optional<T>> clientConsumer, boolean async) {
        final Optional<T> clientOnline = this.online(playerName);
        if (clientOnline.isPresent()) {
            clientConsumer.accept(clientOnline);
            return;
        }

        if (async) {
            CompletableFuture.runAsync(() -> this.offlineNameSearch.accept(playerName, clientConsumer));
        } else {
            this.offlineNameSearch.accept(playerName, clientConsumer);
        }

    }

    /**
     * Search for an online client with advanced name search. Advanced name search will ensure name
     * completion is used, it will search for partial matches.
     * <p>
     * Example: <p></p>
     * "ReyB" finds player "ReyBot"
     * "Bot" finds player "ReyBot"
     * "a" will return 2 matches for players "James" and "Mary"
     *
     * @param playerName The name of the player to search for.
     * @return The collection of client search results.
     */
    public Collection<T> advancedOnline(@Nullable final String playerName) {
        if (playerName == null) {
            return Collections.emptyList();
        }

        final Optional<T> clientOnline = this.online(playerName);

        if (clientOnline.isPresent()) {
            return Collections.singleton(clientOnline.get());
        }

        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().toLowerCase().startsWith(playerName.toLowerCase()))
                .map(this::online)
                .toList();
    }

    /**
     * Search for an online client with advanced name search. Advanced name search will ensure name
     * completion is used, it will search for partial matches. Otherwise, load from database.
     * See {@link #advancedOnline(String)} for more information.
     * Example:
     * "ReyB" finds player "ReyBot"
     * "Bot" finds player "ReyBot"
     * "a" will return 2 matches for players "James" and "Mary"
     *
     * @param playerName     The name of the player to search for.
     * @param clientConsumer The callback to run when the client is found, after searched.
     */
    public void advancedOffline(@Nullable final String playerName, final Consumer<Collection<T>> clientConsumer, boolean async) {
        final Collection<T> onlineResult = this.advancedOnline(playerName);
        if (!onlineResult.isEmpty()) {
            clientConsumer.accept(onlineResult);
            return;
        }

        this.offline(playerName, result -> clientConsumer.accept(
                result.map(Collections::singleton).orElse(Collections.emptySet())), async);
    }

}
