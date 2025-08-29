package me.mykindos.betterpvp.core.utilities.model.display;

public interface ITimedDisplay {
    long getRemaining();

    boolean hasStarted();

    void startTime();

    double getSeconds();

    boolean isWaitToExpire();

    long getStartTime();
}
