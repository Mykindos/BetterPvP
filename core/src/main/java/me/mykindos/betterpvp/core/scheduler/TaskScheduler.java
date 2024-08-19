package me.mykindos.betterpvp.core.scheduler;

import com.google.inject.Singleton;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Getter
public class TaskScheduler {

    private final List<BPVPTask> tasks = new ArrayList<>();

    public void addTask(BPVPTask task) {
        tasks.add(task);
    }

}
