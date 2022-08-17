package me.mykindos.betterpvp.core.combat.log;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.entity.LivingEntity;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

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

    public DamageLog getLastDamager(LivingEntity entity) {
        ConcurrentLinkedDeque<DamageLog> logQueue = objects.get(entity.getUniqueId().toString());
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
}
