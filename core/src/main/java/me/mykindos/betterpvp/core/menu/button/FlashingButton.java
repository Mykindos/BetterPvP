package me.mykindos.betterpvp.core.menu.button;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;

@Slf4j
@BPvPListener
public abstract class FlashingButton<G extends Gui> extends ControlItem<G> {
    private long lastSwitch;
    @Getter
    private boolean flash;
    @Getter
    @Setter
    private boolean flashing;
    @Getter
    @Setter
    private long flashPeriod;

    /**
     * Default constructor, flashing = false, flashPeriod = 1s
     */
    protected FlashingButton() {
        this.lastSwitch = System.currentTimeMillis();
        this.flash = false;
        this.flashing = false;
        this.flashPeriod = 1000L;
    }

    /**
     * Sets this button to flashing, and the default flash period in seconds
     * @param period length of time between flashes in seconds
     */
    protected FlashingButton(double period) {
        this.lastSwitch = System.currentTimeMillis();
        this.flash = false;
        this.flashing = true;
        this.flashPeriod = (long) (period * 1000L);
    }

    public void handleFlash() {
        if (flashing) {
            if (UtilTime.elapsed(lastSwitch, this.getFlashPeriod())) {
                this.flash = !this.flash;
                log.info(String.valueOf(flash));
                this.lastSwitch = System.currentTimeMillis();
                this.notifyWindows();
            }
        }
    }
}
