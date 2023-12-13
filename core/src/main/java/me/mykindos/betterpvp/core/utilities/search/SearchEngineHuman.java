package me.mykindos.betterpvp.core.utilities.search;

import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SearchEngineHuman<T> extends SearchEngineBase<T> {

    private final CommandSender human;
    private boolean inform = true;

    public SearchEngineHuman(CommandSender human,
                             Function<UUID, Optional<T>> onlineSearch,
                             BiConsumer<UUID, Consumer<Optional<T>>> offlineUuidSearch,
                             BiConsumer<String, Consumer<Optional<T>>> offlineNameSearch) {
        super(onlineSearch, offlineUuidSearch, offlineNameSearch);
        this.human = human;
    }

    public SearchEngineBase<T> inform(final boolean inform) {
        this.inform = inform;
        return this;
    }

    @Override
    public Optional<T> online(final UUID uuid) {
        final boolean willInform = this.willInform();
        return this.optionalInform(willInform, () -> super.online(uuid), () -> this.zeroMatches(uuid.toString()));
    }

    @Override
    public Optional<T> online(final String playerName) {
        final boolean willInform = this.willInform();
        return this.optionalInform(willInform, () -> super.online(playerName), () -> this.zeroMatches(playerName));
    }

    @Override
    public void offline(final UUID uuid, final Consumer<Optional<T>> clientConsumer) {
        final boolean willInform = this.willInform();
        super.offline(uuid, result -> clientConsumer.accept(
                this.optionalInform(willInform, () -> result, () -> this.zeroMatches(uuid.toString()))));
    }

    @Override
    public void offline(final String playerName, final Consumer<Optional<T>> clientConsumer) {
        final boolean willInform = this.willInform();
        super.offline(playerName, result -> clientConsumer.accept(
                this.optionalInform(willInform, () -> result, () -> this.zeroMatches(playerName))));
    }

    @Override
    public Collection<T> advancedOnline(final String playerName) {
        final boolean willInform = this.willInform();
        return this.collectionInform(willInform, () -> super.advancedOnline(playerName),
                () -> this.zeroMatches(playerName),
                matches -> this.tooManyMatches(matches, playerName)
        );
    }

    @Override
    public void advancedOffline(final String playerName, final Consumer<Collection<T>> clientConsumer) {
        final boolean willInform = this.willInform();
        super.advancedOffline(playerName, result -> clientConsumer.accept(
                this.collectionInform(willInform, () -> result, () -> this.zeroMatches(playerName),
                        matches -> this.tooManyMatches(matches, playerName)
                )));
    }

    public void tooManyMatches(final Collection<T> matches, final String search) {
        final String matchesList = matches.stream().map(Object::toString).collect(Collectors.joining(", "));
        final int count = matches.size();

        UtilMessage.message(this.human, "Player Search", "<alt2>%s</alt2> matches for [<alt2>%s</alt2>]", count, search);
        UtilMessage.message(this.human, "Player Search", "Possible matches: [<alt2>%s</alt2>]", matchesList);
    }

    public void zeroMatches(final String search) {
        UtilMessage.message(this.human, "Player Search", "<alt2>0</alt2> matches for [<alt2>%s</alt2>]", search);
    }

    private boolean willInform() {
        final boolean willInform = this.inform;
        this.inform = false;
        return willInform;
    }

    private Optional<T> optionalInform(final boolean inform, final Supplier<Optional<T>> resultSupplier,
                                            final Runnable failure) {
        final Optional<T> result = resultSupplier.get();
        if (inform && result.isEmpty()) {
            failure.run();
        }

        if (inform) {
            this.inform = false;
        }

        return result;
    }

    private Collection<T> collectionInform(final boolean inform, final Supplier<Collection<T>> resultSupplier,
                                                final Runnable noneFailure,
                                                final Consumer<Collection<T>> tooManyFailure) {
        final Collection<T> result = resultSupplier.get();
        if (inform && result.isEmpty()) {
            noneFailure.run();
        } else if (inform && result.size() > 1) {
            tooManyFailure.accept(result);
        }

        if (inform) {
            this.inform = true;
        }

        return result;
    }

}
