package me.mykindos.betterpvp.core.framework.delayedactions;

import lombok.Data;

@Data
public class DelayedAction {

    private final Runnable runnable;
    private final long time;

    private String titleText;
    private String subtitleText;
    private boolean countdown;
    private String countdownText;

}
