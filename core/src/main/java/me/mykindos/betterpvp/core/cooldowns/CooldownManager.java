package me.mykindos.betterpvp.core.cooldowns;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Synchronized;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.events.CooldownEvent;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@CustomLog
@Singleton
public class CooldownManager extends Manager<ConcurrentHashMap<String, Cooldown>> {

    @Inject
    private ClientManager clientManager;

    /**
     * Triggers the usage of a player's ability with a specified duration and cooldown settings.
     *
     * This method initializes a cooldown for the given player and ability with the specified
     * duration. If the `inform` parameter is true, a notification will be sent to the player
     * regarding the cooldown. The cooldown can optionally be set to remove upon death.
     *
     * @param player the player who is using the ability
     * @param ability the name of the ability being used
     * @param duration the duration of the cooldown in seconds
     * @param inform whether to notify the player regarding the cooldown
     * @return true if the cooldown is successfully started or extended, false otherwise
     */
    public boolean use(Player player, String ability, double duration, boolean inform) {
        return use(player, ability, duration, inform, true);
    }

    /**
     * Sets a cooldown on an ability for a player with specified parameters.
     * This method is used to enforce ability usage restrictions for a specified
     * duration and allows customization of notifications and behavior upon the player's death.
     *
     * @param player the player for whom the ability cooldown is being applied
     * @param ability the name of the ability that is being restricted
     * @param duration the duration of the cooldown, in seconds
     * @param inform whether to notify the player about the cooldown
     * @param removeOnDeath whether the cooldown should automatically be removed upon the player's death
     * @return true if the cooldown was successfully applied, false if the cooldown already exists or fails
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath) {
        return use(player, ability, duration, inform, removeOnDeath, false, x -> false);
    }

    /**
     * Handles the use of an ability by a player and applies a cooldown with the specified parameters.
     * This version of the method allows additional customization using the provided parameters and
     * applies specific behavior when certain options like "cancellable" or "removeOnDeath" are chosen.
     *
     * @param player the player who is using the ability
     * @param ability the identifier of the ability being used
     * @param duration the duration of the cooldown in seconds
     * @param inform whether to notify the player about the cooldown
     * @param removeOnDeath whether the cooldown should be removed if the player dies
     * @param cancellable whether the cooldown may be manually cancelled before it expires
     * @return true if the cooldown was successfully applied, false if the ability is already on cooldown
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, x -> false);
    }

    /**
     * Applies a cooldown for a specific player and ability, with the option to configure various
     * behaviors such as duration, inform triggers, and conditions related to cancellation and display.
     *
     * @param player the player to whom the cooldown will apply
     * @param ability the name or identifier of the ability requiring a cooldown
     * @param duration the duration of the cooldown in seconds
     * @param inform whether the player should be notified about the cooldown
     * @param removeOnDeath whether the cooldown should be removed if the player dies
     * @param cancellable whether the cooldown can be manually cancelled before it completes
     * @param actionBar whether the cooldown should display progress on the player's action bar
     * @return true if the cooldown was successfully applied, false if it could not be applied (e.g., a cooldown already exists)
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, boolean actionBar) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, x -> actionBar);
    }

    /**
     * Attempts to initiate the use of a specific ability for a given player with the specified parameters.
     * This method handles cooldowns, action bar updates, and various conditions surrounding the ability's use.
     *
     * @param player The player attempting to use the ability.
     * @param ability The identifier of the ability to be used.
     * @param duration The duration for which the ability will be active, in seconds.
     * @param inform Whether the player should be informed about the cooldown.
     * @param removeOnDeath Whether the cooldown should be removed upon the player's death.
     * @param cancellable Indicates if the ability's cooldown or effects can be cancelled by external factors.
     * @param actionBarCondition A condition that determines whether the action bar should be updated for this ability, based on the player's state or context.
     * @return True if the ability was successfully initiated, false otherwise (e.g., due to an active cooldown or unmet conditions).
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, @Nullable Predicate<Gamer> actionBarCondition) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, actionBarCondition, 1000);
    }

    /**
     * Attempts to initiate the use of an ability for a specified player with specific conditions and configurations.
     *
     * @param player              The player attempting to use the ability.
     * @param ability             The name of the ability to be used.
     * @param duration            The duration of the ability's cooldown in seconds.
     * @param inform              Whether the player should be informed of the cooldown.
     * @param removeOnDeath       Whether the cooldown should be removed if the player dies.
     * @param cancellable         Whether the cooldown can be cancelled/reverted.
     * @param actionBarCondition  An optional condition to determine if an action bar should be displayed for the player.
     * @param actionBarPriority   The priority of the action bar for display handling.
     * @return True if the ability usage was successfully initiated, false otherwise.
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, @Nullable Predicate<Gamer> actionBarCondition, int actionBarPriority) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, actionBarCondition, actionBarPriority, null);
    }

    /**
     * Attempts to use a specified ability for a player, applying cooldowns and various effects.
     *
     * @param player The player attempting to use the ability.
     * @param ability The name of the ability being used.
     * @param duration The duration of the cooldown in seconds.
     * @param inform Determines whether the player should be informed if the cooldown is active.
     * @param removeOnDeath If true, the cooldown will be removed if the player dies.
     * @param cancellable Indicates whether the cooldown can be canceled by certain mechanics.
     * @param actionBarCondition A condition to determine if an action bar update should be displayed for the cooldown.
     * @param actionBarPriority The priority level for displaying the action bar component.
     * @param onExpire A callback function to execute when the cooldown expires.
     * @return True if the ability was successfully used and the cooldown was applied, or false if the cooldown is already active or an error occurred.
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, @Nullable Predicate<Gamer> actionBarCondition, int actionBarPriority, Consumer<Cooldown> onExpire) {
        final Gamer gamer = clientManager.search().online(player).getGamer();


        // We add 1.5f to the duration in seconds, so they can see that it expired, and it doesn't instantly disappear
        TimedComponent actionBarComponent = null;
        if (actionBarCondition != null) {
            actionBarComponent = new TimedComponent(duration + 1.5, false, g -> {

                if (!actionBarCondition.test(gamer)) {
                    return null; // Skip if we should not send the action bar message;
                }

                final TextComponent cooldownName = Component.text(ability).decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE);
                final Optional<ConcurrentHashMap<String, Cooldown>> cooldowns = getObject(player.getUniqueId());
                if (cooldowns.isEmpty()) {
                    return null; // Skip
                }

                final Cooldown cooldown = cooldowns.get().get(ability);

                // Show READY after cooldown has been removed, not expired.
                // If it has expired that means that the cooldown remaining time is 0 or -1, and we want to show full bar for that
                if (cooldown == null || cooldown.getRemaining() <= 0) {
                    return Component.join(JoinConfiguration.separator(Component.space()), cooldownName.decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN), Component.text("Recharged").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN));
                }

                final double max = cooldown.getSeconds() / 1000;
                final double progress = Math.min(1f, Math.max(0, (max - cooldown.getRemaining()) / max));
                final ProgressBar progressBar = ProgressBar.withProgress((float) progress);

                final TextComponent bar = progressBar.build();
                final double remainingSeconds = Math.max(0.0, cooldown.getRemaining());
                final TextComponent cooldownRemaining = Component.text(String.format("%.1fs", remainingSeconds)).color(NamedTextColor.WHITE);
                return Component.join(JoinConfiguration.separator(Component.space()), cooldownName, bar, cooldownRemaining);
            });
        }

        var cooldownOptional = getObject(player.getUniqueId().toString()).or(() -> {
            ConcurrentHashMap<String, Cooldown> cooldowns = new ConcurrentHashMap<>();
            objects.put(player.getUniqueId().toString(), cooldowns);
            return Optional.of(cooldowns);
        });

        if (cooldownOptional.isPresent()) {
            ConcurrentHashMap<String, Cooldown> cooldowns = cooldownOptional.get();


            if (player.isOp()) {
                if (player.getGameMode() == GameMode.CREATIVE) {
                    return true;
                }
            }

            if (hasCooldown(player, ability)) {

                if (inform) {
                    informCooldown(player, ability);
                }

                return false;
            }

            Cooldown cooldown = new Cooldown(ability, duration, System.currentTimeMillis(), removeOnDeath, inform, cancellable);
            if (onExpire != null) {
                cooldown.setOnExpire(onExpire);
            }

            CooldownEvent event = UtilServer.callEvent(new CooldownEvent(player, cooldown));
            if (!event.isCancelled()) {
                cooldowns.put(ability, cooldown);

                if (actionBarComponent != null) {
                    gamer.getActionBar().add(actionBarPriority, actionBarComponent);
                }
                return true;
            }

            return false;
        }

        log.error("Could not find cooldown entry for {}", player.getName()).submit();
        return false;
    }

    /**
     * Checks whether the specified player currently has an active cooldown for the given ability.
     *
     * A cooldown indicates whether an ability or action is currently unavailable for use
     * until a specific amount of time has elapsed. If a cooldown exists, it is identified
     * based on the player's unique identifier and the ability name.
     *
     * @param player the player for whom to check the cooldown
     * @param ability the name of the ability to check for an active cooldown
     * @return true if the player has an active cooldown for the specified ability, false otherwise
     */
    public boolean hasCooldown(Player player, String ability) {

        Optional<ConcurrentHashMap<String, Cooldown>> cooldownOptional = getObject(player.getUniqueId().toString());
        if (cooldownOptional.isPresent()) {
            var cooldowns = cooldownOptional.get();
            return cooldowns.containsKey(ability);
        }

        return false;
    }

    /**
     * Notifies a player about the cooldown duration remaining for a specific ability.
     *
     * @param player  The player to inform about the cooldown for the ability.
     * @param ability The name of the ability for which the cooldown information is being provided.
     */
    public void informCooldown(Player player, String ability) {
        UtilMessage.simpleMessage(player, "Cooldown", "You cannot use <alt>%s</alt> for <alt>%s</alt> seconds.", ability, Math.max(0, getAbilityRecharge(player, ability).getRemaining()));
    }

    /**
     * Reduces the cooldown duration for a specific ability associated with the given player.
     * This method retrieves the player's active cooldowns, updates the specified ability's
     * system time by subtracting the reduction time, and ensures the updated time does not surpass the original.
     *
     * @param player the player whose cooldown is to be reduced
     * @param ability the name of the ability whose cooldown is to be reduced
     * @param reductionSeconds the amount of time, in seconds, to reduce from the current cooldown
     */
    public void reduceCooldown(Player player, String ability, double reductionSeconds) {
        Optional<ConcurrentHashMap<String, Cooldown>> cooldownOptional = getObject(player.getUniqueId().toString());
        if (cooldownOptional.isPresent()) {
            ConcurrentHashMap<String, Cooldown> cooldowns = cooldownOptional.get();
            Cooldown cooldown = cooldowns.get(ability);
            if (cooldown != null) {
                long reductionMillis = (long) (reductionSeconds * 1000);
                long newSystemTime = cooldown.getSystemTime() - reductionMillis;

                if (newSystemTime > cooldown.getSystemTime()) {
                    newSystemTime = cooldown.getSystemTime();
                }

                Cooldown newCooldown = new Cooldown(ability, cooldown.getSeconds() / 1000.0, newSystemTime, cooldown.isRemoveOnDeath(), cooldown.isInform(), cooldown.isCancellable());

                cooldowns.put(ability, newCooldown);
            }
        }
    }

    /**
     * Retrieves the cooldown associated with the specified ability for a given player.
     *
     * This method searches for the player's cooldowns using their unique identifier
     * and attempts to fetch the cooldown data associated with the specified ability.
     * If no cooldown exists for the given ability, it returns {@code null}.
     *
     * @param player the player whose ability cooldown is being retrieved
     * @param ability the name of the ability for which the cooldown is being fetched
     * @return the {@link Cooldown} object associated with the specified ability
     *         if present, or {@code null} if no such cooldown exists
     */
    public Cooldown getAbilityRecharge(Player player, String ability) {
        Optional<ConcurrentHashMap<String, Cooldown>> cooldownOptional = getObject(player.getUniqueId().toString());
        if (cooldownOptional.isPresent()) {
            var cooldowns = cooldownOptional.get();
            return cooldowns.getOrDefault(ability, null);
        }

        return null;
    }

    /**
     * Removes a cooldown associated with a player's ability. If the cooldown exists and is removed, any expiration
     * callback associated with the cooldown is executed. Additionally, an informational message may be sent to the player
     * based on the `silent` flag.
     *
     * @param player the player whose cooldown is to be removed
     * @param ability the name of the ability for which the cooldown is to be removed
     * @param silent if true, no informational message is sent to the player; if false, a message is sent notifying
     *               that the ability has been recharged
     */
    public void removeCooldown(Player player, String ability, boolean silent) {
        Optional<ConcurrentHashMap<String, Cooldown>> cooldownOptional = getObject(player.getUniqueId().toString());
        if (cooldownOptional.isPresent()) {
            var cooldowns = cooldownOptional.get();
            if (cooldowns.containsKey(ability)) {
                Cooldown cooldown = cooldowns.remove(ability);
                if (cooldown.getOnExpire() != null) {
                    cooldown.getOnExpire().accept(cooldown);
                }

                if (!silent) {
                    UtilMessage.simpleMessage(player, "Recharge", "<alt>%s</alt> has been recharged.", ability);
                }
            }

        }
    }

    /**
     * Processes and manages the active cooldowns, removing expired entries and handling
     * associated notifications or expiration actions.
     *
     * This method iterates through all stored cooldown objects and evaluates
     * their remaining time. Expired cooldowns are removed from the collection.
     * If the cooldown has a notification flag enabled (`inform`), the associated
     * player is notified of the cooldown's recharge status. Additionally, a sound
     * notification may be played if the player has the corresponding setting enabled.
     *
     * If the cooldown has an `onExpire` callback, it is invoked upon expiration.
     * After processing all entries, empty collections are cleaned up to maintain
     * efficient storage.
     *
     * The method is thread-safe, as it is synchronized to ensure consistency
     * during concurrent access.
     */
    @Synchronized
    public void processCooldowns() {
        objects.forEach((key, value) -> {
            value.entrySet().removeIf(entry -> {
                Cooldown cd = entry.getValue();
                if (cd.getRemaining() <= 0) {
                    if (cd.isInform()) {
                        Player player = Bukkit.getPlayer(UUID.fromString(key));
                        if (player != null) {

                            Client client = clientManager.search().online(player);
                            final boolean soundSetting = (boolean) client.getProperty(ClientProperty.COOLDOWN_SOUNDS_ENABLED).orElse(false);
                            if (soundSetting) {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, 3.0f);
                            }

                            UtilMessage.simpleMessage(player, "Cooldown", "<alt>%s</alt> has been recharged.", entry.getKey());
                        }
                    }

                    if (cd.getOnExpire() != null) {
                        cd.getOnExpire().accept(cd);
                    }
                    return true;
                }
                return false;
            });
        });

        objects.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
