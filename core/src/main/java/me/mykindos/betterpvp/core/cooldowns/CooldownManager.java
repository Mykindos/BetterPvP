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
     *
     * @param player
     * @param ability
     * @param duration
     * @param inform
     * @return TRUE if ability is used, FALSE if a cooldown is already present
     */
    public boolean use(Player player, String ability, double duration, boolean inform) {
        return use(player, ability, duration, inform, true);
    }

    /**
     *
     * @param player
     * @param ability
     * @param duration
     * @param inform
     * @param removeOnDeath
     * @return TRUE if ability is used, FALSE if a cooldown is already present
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath) {
        return use(player, ability, duration, inform, removeOnDeath, false, x -> false);
    }

    /**
     *
     * @param player
     * @param ability
     * @param duration
     * @param inform
     * @param removeOnDeath
     * @param cancellable
     * @return TRUE if ability is used, FALSE if a cooldown is already present
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, x -> false);
    }

    /**
     *
     * @param player
     * @param ability
     * @param duration
     * @param inform
     * @param removeOnDeath
     * @param cancellable
     * @param actionBar
     * @return TRUE if ability is used, FALSE if a cooldown is already present
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, boolean actionBar) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, x -> actionBar);
    }

    /**
     *
     * @param player
     * @param ability
     * @param duration
     * @param inform
     * @param removeOnDeath
     * @param cancellable
     * @param actionBarCondition
     * @return TRUE if ability is used, FALSE if a cooldown is already present
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, @Nullable Predicate<Gamer> actionBarCondition) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, actionBarCondition, 1000);
    }

    /**
     *
     * @param player
     * @param ability
     * @param duration
     * @param inform
     * @param removeOnDeath
     * @param cancellable
     * @param actionBarCondition
     * @param actionBarPriority
     * @return TRUE if ability is used, FALSE if a cooldown is already present
     */
    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, @Nullable Predicate<Gamer> actionBarCondition, int actionBarPriority) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, actionBarCondition, actionBarPriority, null);
    }

    /**
     *
     * @param player
     * @param ability
     * @param duration
     * @param inform
     * @param removeOnDeath
     * @param cancellable
     * @param actionBarCondition
     * @param actionBarPriority
     * @param onExpire
     * @return TRUE if ability is used, FALSE if a cooldown is already present
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
     *
     * @param player the player to check for
     * @param ability the ability to check for
     * @return TRUE if ability is on cooldown for player, FALSE if not on cooldown
     */
    public boolean hasCooldown(Player player, String ability) {

        Optional<ConcurrentHashMap<String, Cooldown>> cooldownOptional = getObject(player.getUniqueId().toString());
        if (cooldownOptional.isPresent()) {
            var cooldowns = cooldownOptional.get();
            return cooldowns.containsKey(ability);
        }

        return false;
    }

    public void informCooldown(Player player, String ability) {
        UtilMessage.simpleMessage(player, "Cooldown", "You cannot use <alt>%s</alt> for <alt>%s</alt> seconds.", ability, Math.max(0, getAbilityRecharge(player, ability).getRemaining()));
    }

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

    public Cooldown getAbilityRecharge(Player player, String ability) {
        Optional<ConcurrentHashMap<String, Cooldown>> cooldownOptional = getObject(player.getUniqueId().toString());
        if (cooldownOptional.isPresent()) {
            var cooldowns = cooldownOptional.get();
            return cooldowns.getOrDefault(ability, null);
        }

        return null;
    }

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
