package me.mykindos.betterpvp.core.utilities.search;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.exception.ClientNotLoadedException;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class SearchEngineBase<T> {

    private final Function<UUID, Optional<T>> onlineSearch;
    private final BiConsumer<UUID, Consumer<Optional<T>>> offlineUuidSearch;
    private final BiConsumer<String, Consumer<Optional<T>>> offlineNameSearch;

    public SearchEngineBase(Function<UUID, Optional<T>> onlineSearch,
                            BiConsumer<UUID, Consumer<Optional<T>>> offlineUuidSearch,
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
    public T online(final CommandSender sender) {
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
    public T online(final Player player) {
        Optional<T> online = this.online(player.getUniqueId());

        return online.orElseThrow(() -> {
            log.warn(ClientManager.RETRIEVE_ERROR_FORMAT_SERVER, player.getName());
            player.kick(Component.text(ClientManager.LOAD_ERROR_FORMAT_ENTITY));
            return new ClientNotLoadedException(player);
        });
    }

    /**
     * Get a loaded client by name.
     *
     * @param playerName The name of the player to search for.
     * @return The client.
     */
    public Optional<T> online(final String playerName) {
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
    public void offline(final UUID uuid, final Consumer<Optional<T>> clientConsumer) {
        final Optional<T> clientOnline = this.online(uuid);
        if (clientOnline.isPresent()) {
            clientConsumer.accept(clientOnline);
            return;
        }

        this.offlineUuidSearch.accept(uuid, clientConsumer);
    }

    /**
     * Search for an online client by exact name, otherwise load from database.
     *
     * @param playerName     The name of the player to search for.
     * @param clientConsumer The callback to run when the client is found, after searched.
     */
    public void offline(final String playerName, final Consumer<Optional<T>> clientConsumer) {
        final Optional<T> clientOnline = this.online(playerName);
        if (clientOnline.isPresent()) {
            clientConsumer.accept(clientOnline);
            return;
        }

        this.offlineNameSearch.accept(playerName, clientConsumer);
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
    public Collection<T> advancedOnline(final String playerName) {
        final Optional<T> clientOnline = this.online(playerName);

        if (clientOnline.isPresent()) {
            return Collections.singleton(clientOnline.get());
        }

        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> {
                    if (player.getName().toLowerCase().startsWith(playerName.toLowerCase())) {
                        return true;
                    } else {
                        return UtilFormat.isSimilar(player.getName(), playerName);
                    }
                })
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
    public void advancedOffline(final String playerName, final Consumer<Collection<T>> clientConsumer) {
        final Collection<T> onlineResult = this.advancedOnline(playerName);
        if (!onlineResult.isEmpty()) {
            clientConsumer.accept(onlineResult);
            return;
        }

        this.offline(playerName, result -> clientConsumer.accept(
                result.map(Collections::singleton).orElse(Collections.emptySet())));
    }

}
