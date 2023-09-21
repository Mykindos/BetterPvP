package me.mykindos.betterpvp.core.cooldowns;


import com.google.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@Slf4j
@Singleton
public class CooldownManager extends Manager<ConcurrentHashMap<String, Cooldown>> {

    public boolean add(Player player, String ability, double duration, boolean inform) {
        return add(player, ability, duration, inform, true);
    }

    public boolean add(Player player, String ability, double duration, boolean inform, boolean removeOnDeath) {
        return add(player, ability, duration, inform, removeOnDeath, false);
    }

    public boolean add(Player player, String ability, double duration, boolean inform, boolean removeOnDeath, boolean cancellable) {

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

            if (isCooling(player, ability)) {

                if (inform) {
                    UtilMessage.simpleMessage(player, "Cooldown", "You cannot use <alt>%s</alt> for <alt>%s</alt> seconds.", ability, Math.max(0, getAbilityRecharge(player, ability).getRemaining()));
                }

                return false;
            }

            cooldowns.put(ability, new Cooldown(duration, System.currentTimeMillis(), removeOnDeath, inform, cancellable));
            return true;
        }

        log.error("Could not find cooldown entry for {}", player.getName());
        return false;
    }

    public boolean isCooling(Player player, String ability) {

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
