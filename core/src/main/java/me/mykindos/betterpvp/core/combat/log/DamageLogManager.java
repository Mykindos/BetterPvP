package me.mykindos.betterpvp.core.combat.log;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Singleton
public class DamageLogManager extends Manager<ConcurrentLinkedDeque<DamageLog>> {

    public void add(LivingEntity damagee, DamageLog damageLog) {
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
}
