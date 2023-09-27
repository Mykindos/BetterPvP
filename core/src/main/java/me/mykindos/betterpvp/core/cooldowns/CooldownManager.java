package me.mykindos.betterpvp.core.cooldowns;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Slf4j
@Singleton
public class CooldownManager extends Manager<ConcurrentHashMap<String, Cooldown>> {

    @Inject
    private GamerManager gamerManager;

    public boolean use(Player player, String ability, double duration, boolean inform) {
        return use(player, ability, duration, inform, true);
    }

    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath) {
        return use(player, ability, duration, inform, removeOnDeath, false, x -> false);
    }

    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, x -> false);
    }

    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, boolean actionBar) {
        return use(player, ability, duration, inform, removeOnDeath, cancellable, x -> actionBar);
    }

    public boolean use(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable, @Nullable Predicate<Gamer> actionBarCondition) {
        final Gamer gamer = gamerManager.getObject(player.getUniqueId()).orElseThrow();

        // We add 1.5f to the duration in seconds, so they can see that it expired, and it doesn't instantly disappear
        final TimedComponent actionBarComponent = new TimedComponent(duration + 1.5, false, g -> {
            if (actionBarCondition == null || !actionBarCondition.test(gamer)) {
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
                return Component.join(JoinConfiguration.separator(Component.space()), cooldownName, Component.text("Ready!").color(NamedTextColor.GREEN));
            }

            final double progress = Math.min(1f, Math.max(0, (duration - cooldown.getRemaining()) / duration));
            final ProgressBar progressBar = ProgressBar.withProgress((float) progress);

            final TextComponent bar = progressBar.build();
            final double remainingSeconds = Math.max(0.0, cooldown.getRemaining());
            final TextComponent cooldownRemaining = Component.text(String.format("%.1fs", remainingSeconds)).color(NamedTextColor.WHITE);
            return Component.join(JoinConfiguration.separator(Component.space()), cooldownName, bar, cooldownRemaining);
        });

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
                    UtilMessage.simpleMessage(player, "Cooldown", "You cannot use <alt>%s</alt> for <alt>%s</alt> seconds.", ability, Math.max(0, getAbilityRecharge(player, ability).getRemaining()));
                }

                return false;
            }

            cooldowns.put(ability, new Cooldown(duration, System.currentTimeMillis(), removeOnDeath, inform, cancellable));
            gamer.getActionBar().add(1_000, actionBarComponent);
            return true;
        }

        log.error("Could not find cooldown entry for {}", player.getName());
        return false;
    }

    public boolean hasCooldown(Player player, String ability) {

        Optional<ConcurrentHashMap<String, Cooldown>> cooldownOptional = getObject(player.getUniqueId().toString());
        if (cooldownOptional.isPresent()) {
            var cooldowns = cooldownOptional.get();
            return cooldowns.containsKey(ability);
        }

        return false;
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
                cooldowns.remove(ability);
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
                            UtilMessage.simpleMessage(player, "Cooldown", "<alt>%s</alt> has been recharged.", entry.getKey());
                        }
                    }
                    return true;
                }
                return false;
            });
        });

        objects.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
