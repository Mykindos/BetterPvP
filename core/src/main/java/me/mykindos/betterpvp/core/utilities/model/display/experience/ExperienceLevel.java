package me.mykindos.betterpvp.core.utilities.model.display.experience;

import me.mykindos.betterpvp.core.utilities.model.display.AbstractDisplayQueue;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.experience.data.ExperienceLevelData;
import org.bukkit.entity.Player;

public class ExperienceLevel extends AbstractDisplayQueue<ExperienceLevelData, DisplayObject<ExperienceLevelData>> {

    private static final ExperienceLevelData EMPTY = new ExperienceLevelData(0);


    /**
     * Send this information to the player
     *
     * @param player
     * @param data
     */
    @Override
    public void sendTo(Player player, ExperienceLevelData data) {
        player.setLevel(data.getLevel());
    }

    /**
     * Get an element representing nothing, i.e. {@link Component#empty()}
     *
     * @return
     */
    @Override
    protected ExperienceLevelData getEmpty() {
        return EMPTY;
    }
}