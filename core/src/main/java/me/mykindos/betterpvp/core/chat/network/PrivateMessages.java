package me.mykindos.betterpvp.core.chat.network;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.chat.ignore.IIgnoreService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.net.BusMessage;
import me.mykindos.betterpvp.core.framework.net.MessageBus;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Sends one player a message wherever on the network they are.
 * <p>
 * A recipient on this server is written to directly. Anyone else is asked for over the bus, and the server holding
 * them delivers and says so, which is also how the sender learns whether the name they typed is online at all.
 * <p>
 * Being on a different site does not hide a player from this. Sites scope what somebody perceives around them, and a
 * private message is addressed rather than perceived, so the only thing that hides a recipient is vanish.
 */
@Singleton
@CustomLog
public class PrivateMessages {

    static final String TOPIC = "chat.private";

    private static final Duration REACH_TIMEOUT = Duration.ofSeconds(2);
    private static final String SENDER = "sender";
    private static final String SENDER_NAME = "senderName";
    private static final String TARGET = "target";
    private static final String MESSAGE = "message";

    /** What became of a message, and who it was for, which the caller turns into whatever wording its command uses. */
    @Value
    public static class Result {
        Outcome outcome;
        /** The recipient as they are known, which is the typed name only when nobody by it was found. */
        String targetName;
    }

    public enum Outcome {
        DELIVERED,
        NOT_FOUND,
        SELF,
        /** The sender has the recipient on their ignore list, so nothing was sent. */
        IGNORING
    }

    private final MessageBus bus;
    private final ClientManager clientManager;
    private final IFilterService filterService;
    private final IIgnoreService ignoreService;
    private final EffectManager effects;

    @Inject
    public PrivateMessages(@NotNull MessageBus bus, @NotNull ClientManager clientManager,
                           @NotNull IFilterService filterService, @NotNull IIgnoreService ignoreService,
                           @NotNull EffectManager effects) {
        this.bus = bus;
        this.clientManager = clientManager;
        this.filterService = filterService;
        this.ignoreService = ignoreService;
        this.effects = effects;
        bus.answer(TOPIC, this::answer);
    }

    /** Sends to whoever goes by {@code targetName}. */
    public @NotNull CompletableFuture<Result> send(@NotNull Player sender, @NotNull Client senderClient,
                                                  @NotNull String targetName, @NotNull String message) {
        return clientManager.search().offline(targetName)
                .thenCompose(found -> send(sender, senderClient, found.orElse(null), targetName, message));
    }

    /** Sends to a player already known by id, which is how a reply finds whoever wrote last. */
    public @NotNull CompletableFuture<Result> sendTo(@NotNull Player sender, @NotNull Client senderClient,
                                                    @NotNull UUID targetId, @NotNull String message) {
        return clientManager.search().offline(targetId)
                .thenCompose(found -> send(sender, senderClient, found.orElse(null), targetId.toString(), message));
    }

    private @NotNull CompletableFuture<Result> send(@NotNull Player sender, @NotNull Client senderClient,
                                                   @Nullable Client targetClient, @NotNull String typedName,
                                                   @NotNull String message) {
        if (targetClient == null) {
            return CompletableFuture.completedFuture(new Result(Outcome.NOT_FOUND, typedName));
        }

        final String targetName = targetClient.getName();
        if (targetClient.getUniqueId().equals(senderClient.getUniqueId())) {
            return CompletableFuture.completedFuture(new Result(Outcome.SELF, targetName));
        }
        if (ignoreService.isClientIgnored(senderClient, targetClient)) {
            return CompletableFuture.completedFuture(new Result(Outcome.IGNORING, targetName));
        }

        final Player here = Bukkit.getPlayer(targetClient.getUniqueId());
        if (here != null && isHidden(here, senderClient)) {
            return CompletableFuture.completedFuture(new Result(Outcome.NOT_FOUND, targetName));
        }

        return filterService.filterMessage(message).thenCompose(filtered -> here != null
                ? CompletableFuture.completedFuture(deliverHere(sender, senderClient, here, targetClient, filtered))
                : deliverElsewhere(sender, senderClient, targetClient, filtered));
    }

    private @NotNull Result deliverHere(@NotNull Player sender, @NotNull Client senderClient, @NotNull Player target,
                                         @NotNull Client targetClient, @NotNull String message) {
        echo(sender, targetClient.getName(), message);
        if (!ignoreService.isClientIgnored(targetClient, senderClient)) {
            write(target, senderClient.getName(), message);
            targetClient.putProperty(ClientProperty.LAST_MESSAGED.name(), senderClient.getUniqueId(), true);
        }

        spy(senderClient.getName(), targetClient.getName(), message, sender, target);
        senderClient.putProperty(ClientProperty.LAST_MESSAGED.name(), targetClient.getUniqueId(), true);
        log.info("{} messaged {}: {}", senderClient.getName(), targetClient.getName(), message).submit();
        return new Result(Outcome.DELIVERED, targetClient.getName());
    }

    /**
     * Asks the network to deliver, and treats silence as nobody having the recipient.
     * <p>
     * The reply is the only proof that anything happened, so the sender's own line waits for it rather than being
     * shown up front and contradicted a moment later.
     */
    private @NotNull CompletableFuture<Result> deliverElsewhere(@NotNull Player sender, @NotNull Client senderClient,
                                                                @NotNull Client targetClient, @NotNull String message) {
        if (!bus.isAvailable()) {
            return CompletableFuture.completedFuture(new Result(Outcome.NOT_FOUND, targetClient.getName()));
        }

        final Map<String, String> payload = new HashMap<>();
        payload.put(SENDER, senderClient.getUniqueId().toString());
        payload.put(SENDER_NAME, senderClient.getName());
        payload.put(TARGET, targetClient.getUniqueId().toString());
        payload.put(MESSAGE, message);

        return bus.request(BusMessage.of(TOPIC, payload), REACH_TIMEOUT).thenApply(replies -> {
            if (replies.isEmpty()) {
                return new Result(Outcome.NOT_FOUND, targetClient.getName());
            }

            echo(sender, targetClient.getName(), message);
            spy(senderClient.getName(), targetClient.getName(), message, sender, null);
            senderClient.putProperty(ClientProperty.LAST_MESSAGED.name(), targetClient.getUniqueId(), true);
            log.info("{} messaged {} on another server: {}", senderClient.getName(), targetClient.getName(), message).submit();
            return new Result(Outcome.DELIVERED, targetClient.getName());
        });
    }

    /** Delivers a message from another server, and answers only when this server had the recipient. */
    private @NotNull Optional<BusMessage> answer(@NotNull BusMessage incoming) {
        if (Core.getCurrentRealm().getServer().getName().equals(incoming.getOrigin())) {
            return Optional.empty();
        }

        final Optional<String> targetId = incoming.get(TARGET);
        final Optional<String> senderId = incoming.get(SENDER);
        if (targetId.isEmpty() || senderId.isEmpty()) {
            return Optional.empty();
        }

        final Player target = Bukkit.getPlayer(UUID.fromString(targetId.get()));
        if (target == null) {
            return Optional.empty();
        }

        final String senderName = incoming.getOrDefault(SENDER_NAME, "Unknown");
        final String message = incoming.getOrDefault(MESSAGE, "");
        final Client targetClient = clientManager.search().online(target);

        clientManager.search().offline(UUID.fromString(senderId.get())).thenAccept(found -> found.ifPresent(senderClient -> {
            if (!ignoreService.isClientIgnored(targetClient, senderClient)) {
                write(target, senderName, message);
                targetClient.putProperty(ClientProperty.LAST_MESSAGED.name(), senderClient.getUniqueId(), true);
            }
            spy(senderName, targetClient.getName(), message, null, target);
        }));

        return Optional.of(incoming.replyWith(Map.of(TARGET, targetId.get())));
    }

    /** Shows the line to administrating staff on this server, other than the two people it is between. */
    private void spy(@NotNull String from, @NotNull String to, @NotNull String message,
                     @Nullable Player sender, @Nullable Player target) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender) || online.equals(target)) {
                continue;
            }
            if (clientManager.search().online(online).isAdministrating()) {
                UtilMessage.message(online, line(NamedTextColor.DARK_GREEN, NamedTextColor.GREEN,
                        Component.text(from), Component.text(to), message));
            }
        }
    }

    private void echo(@NotNull Player sender, @NotNull String targetName, @NotNull String message) {
        UtilMessage.message(sender, line(NamedTextColor.DARK_AQUA, NamedTextColor.AQUA,
                Translations.component("core.command.message.you"), Component.text(targetName), message));
    }

    private void write(@NotNull Player target, @NotNull String senderName, @NotNull String message) {
        UtilMessage.message(target, line(NamedTextColor.DARK_AQUA, NamedTextColor.AQUA,
                Component.text(senderName), Translations.component("core.command.message.you"), message));
    }

    /**
     * Whether the recipient should read as offline to the sender. Overridable so the surrounding rules can be
     * tested without the effect registry, which needs a running server to initialise.
     */
    protected boolean isHidden(@NotNull Player target, @NotNull Client senderClient) {
        if (senderClient.hasRank(Rank.ADMIN)) {
            return false;
        }
        return effects.hasEffect(target, EffectTypes.VANISH)
                || (target.isOp() && target.hasPotionEffect(PotionEffectType.INVISIBILITY));
    }

    /** Builds a line in the form {@code [from -> to] message}. */
    static @NotNull Component line(@NotNull NamedTextColor bracketColor, @NotNull NamedTextColor nameColor,
                                   @NotNull Component from, @NotNull Component to, @NotNull String message) {
        return Component.text("[", bracketColor)
                .append(from.colorIfAbsent(nameColor))
                .append(Component.text(" -> ", bracketColor))
                .append(to.colorIfAbsent(nameColor))
                .append(Component.text("] ", bracketColor))
                .append(Component.text(message, NamedTextColor.GRAY));
    }
}
