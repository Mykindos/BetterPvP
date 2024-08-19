package me.mykindos.betterpvp.core.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

import java.util.ListIterator;

@Singleton
@BPvPListener
public class TaskListener implements Listener {

    private final TaskScheduler taskScheduler;

    @Inject
    public TaskListener(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @UpdateEvent
    public void processTasks() {
        ListIterator<BPVPTask> iterator = taskScheduler.getTasks().listIterator();
        while(iterator.hasNext()) {
            BPVPTask task = iterator.next();
            if(System.currentTimeMillis() >= task.getExpiryTime()) {
                iterator.remove();
                continue;
            }

            if(task.getPredicate().test(task.getUuid())) {
                task.getConsumer().accept(task.getUuid());
                iterator.remove();
            }
        }
    }
}
