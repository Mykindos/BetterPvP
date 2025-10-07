package me.mykindos.betterpvp.core.client.stats.display;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
@CustomLog
public class StatBackButton extends BackButton {
    public StatBackButton(Windowed previousMenu) {
        super(previousMenu);
        setOnBack(() -> {
            if (!(getGui() instanceof IAbstractStatMenu current)) {
                log.warn("Using StatBackButton on non IAbstractStatMenu").submit();
                return;
            }
            current.updateCurrentPeriod(previousMenu);
        });
    }
}
