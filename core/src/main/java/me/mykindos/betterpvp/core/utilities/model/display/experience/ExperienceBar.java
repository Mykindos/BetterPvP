package me.mykindos.betterpvp.core.utilities.model.display.experience;

import me.mykindos.betterpvp.core.utilities.model.display.AbstractDisplayQueue;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.experience.data.ExperienceBarData;
import org.bukkit.entity.Player;

public class ExperienceBar extends AbstractDisplayQueue<ExperienceBarData, DisplayObject<ExperienceBarData>> {

    private static final ExperienceBarData EMPTY = new ExperienceBarData(0);

    /**
     * Send this information to the player
     *
     * @param player
     */
    @Override
    public void sendTo(Player player, ExperienceBarData data) {
        player.setExp(data.getPercentage());
    }

    /**
     * Get an element representing nothing, i.e. {@link Component#empty()}
     *
     * @return
     */
    @Override
    protected ExperienceBarData getEmpty() {
        return EMPTY;
    }
}