package me.mykindos.betterpvp.core.combat.damagelog;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Singleton
public class DamageLogManager extends Manager<ConcurrentLinkedDeque<DamageLog>> {

    private static final Collector<DamageLog, ?, Map<String, List<DamageLog>>> SUMMARY_COLLECTOR =
            Collectors.groupingBy(log -> log.getDamager() != null
                    ? log.getDamager().getName()
                    : UtilFormat.cleanString(log.getDamageCause().name()));
    
    public void add(Entity damagee, DamageLog damageLog) {
        ConcurrentLinkedDeque<DamageLog> logs = objects.get(damagee.getUniqueId().toString());
        if (logs == null) {
            logs = new ConcurrentLinkedDeque<>();
            objects.put(damagee.getUniqueId().toString(), logs);
        }

        logs.add(damageLog);
    }

    /**
     * Gets the latest damage logs for the given damagee.
     * If a damage log includes a damager (e.g. a player), the log will be immediately returned.
     * Otherwise it will return the first log that does not include a damager.
     *
     * @param damagee The damagee to get the log for.
     * @return The damage log for the given damagee.
     */
    public DamageLog getLastDamager(LivingEntity damagee) {
        ConcurrentLinkedDeque<DamageLog> logQueue = objects.get(damagee.getUniqueId().toString());
        if (logQueue != null) {
            Iterator<DamageLog> iterator = logQueue.descendingIterator();
            DamageLog nonDamagerLog = null;
            while (iterator.hasNext()) {
                DamageLog log = iterator.next();
                if (log.getDamager() != null) {
                    return log;
                } else {
                    if (nonDamagerLog == null) {
                        nonDamagerLog = log;
                    }
                }
            }
            return nonDamagerLog;
        }
        return null;
    }

    public List<KeyValue<String, Double>> getDamageBreakdown(LivingEntity damagee) {
        List<KeyValue<String, Double>> breakdown = new ArrayList<>();
        ConcurrentLinkedDeque<DamageLog> logQueue = objects.get(damagee.getUniqueId().toString());
        if (logQueue != null) {
            var collector = Collectors.groupingBy(log -> log.getDamager() != null ? UtilFormat.stripColor(log.getDamager().getName()) : "Other",
                    Collectors.summingDouble(DamageLog::getDamage));
            logQueue.stream().collect(collector).forEach((key, value) -> breakdown.add(new KeyValue<>(key, value)));
        }

        breakdown.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return breakdown;
    }

    public void showDeathSummary(long now, Player player, final ConcurrentLinkedDeque<DamageLog> logQueue) {
        final TextComponent.Builder component = Component.empty().toBuilder();
        if (logQueue == null || logQueue.isEmpty()) {
            component.append(Component.text("No damage logs found."));
        } else {
            component.append(Component.text("Death Summary:"));

            logQueue.stream().collect(SUMMARY_COLLECTOR).forEach((source, logs) -> {
                String cause = UtilFormat.cleanString(logs.get(0).getDamageCause().name());
                final double damage = logs.stream().mapToDouble(DamageLog::getDamage).sum();
                final double timePrior = logs.stream()
                        .mapToDouble(DamageLog::getTime)
                        .map(then -> (now - then) / 1000.0)
                        .min()
                        .orElse(0);

                final TextComponent.Builder builder = Component.empty().toBuilder()
                        .appendNewline()
                        .append(Component.text("\u25CF", NamedTextColor.DARK_GREEN))
                        .appendSpace()
                        .append(Component.text(source, NamedTextColor.YELLOW))
                        .appendSpace()
                        .append(Component.text("[", NamedTextColor.GRAY))
                        .append(Component.text(UtilFormat.formatNumber(damage, 1), NamedTextColor.YELLOW))
                        .append(Component.text("]", NamedTextColor.GRAY))
                        .appendSpace();

                // If the cause and the source are different, append the reason
                if (!cause.equalsIgnoreCase(source)) {
                    // Get the reason from the log with the highest damage
                    final Optional<String> reasonOpt = logs.stream()
                            .max(Comparator.comparingDouble(DamageLog::getDamage))
                            .map(log -> String.join(", ", log.getReason()))
                            .map(reason -> reason.isEmpty() ? cause.replace("Entity ", "") : reason);

                    reasonOpt.ifPresent(reason -> builder.append(Component.text("[", NamedTextColor.GRAY))
                            .append(Component.text(reason, NamedTextColor.GREEN))
                            .append(Component.text("]", NamedTextColor.GRAY))
                            .appendSpace());
                }

                builder.append(Component.text("[", NamedTextColor.GRAY))
                        .append(Component.text(String.format("%.1f Seconds Prior", timePrior), NamedTextColor.GREEN))
                        .append(Component.text("]", NamedTextColor.GRAY))
                        .appendSpace();

                component.append(builder.build());
            });
        }

        UtilMessage.message(player, "Death", component.build());
    }
}
