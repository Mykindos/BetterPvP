package me.mykindos.betterpvp.champions.champions.skills.types;

import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.WeakHashMap;
@CustomLog
public abstract class ChargeSkill extends ChannelSkill {
    protected final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    public ChargeSkill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public void cancel(Player player) {
        super.cancel(player);
        charging.remove(player);
    }

    public void activate(Player player, int level) {
        charging.put(player, new ChargeData((float) getChargePerSecond(level)));
        active.add(player.getUniqueId());
    }

    @UpdateEvent
    public void updateCharge() {
        // Charge check
        Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            ChargeData charge = charging.get(player);
            if (player == null || !player.isOnline() || !player.isConnected()) {
                iterator.remove();
                continue;
            }

            // Remove if they no longer have the skill
            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            // Check if they still are blocking and charge
            if (shouldCancelCharge(player, charge, level)) {
                cancel(player);
                iterator.remove();
            }
            switch (getTickBehavior(player, charge, level)) {
                case TICK -> {
                    charge.tick();
                    charge.tickSound(player);
                    onTick(player, charge, level);
                }
                case USE -> {
                    use(player, charge, level);
                    iterator.remove();
                }
            }

            if (forceUse(player, charge, level)) {
                use(player, charge, level);
                iterator.remove();
            }
        }
    }

    /**
     * Force the player to use the skill, after canTickCharge
     * @param player
     * @param chargeData
     * @param level
     * @return
     */
    public boolean forceUse(Player player, ChargeData chargeData, int level) {
        return false;
    }

    /**
     * Use the skill
     * @param player
     * @param chargeData
     * @param level
     * @return
     */
    public boolean use(Player player, ChargeData chargeData, int level) {
        return true;
    }

    /**
     * Whether this should cancel the charge of the skill
     * @param player
     * @return
     */
    boolean shouldCancelCharge(Player player, ChargeData chargeData, int level) {
        return false;
    }

    public void onTick(Player player, ChargeData chargeData, int level) {

    }

    /**
     * Whether this charge should tick
     * Use shouldCancelCharge if you want the charge to cancel entirely
     * @param player
     * @return
     */
    public TickBehavior getTickBehavior(Player player, ChargeData chargeData, int level) {
        return TickBehavior.TICK;
    }

    /**
     * The charge of the skill at a given level
     * @param level
     * @return
     */
    public double getChargePerSecond(int level) {
        return baseCharge + chargeIncreasePerLevel * (level - 1);
    }

    public enum TickBehavior {
        /**
         * Attempt to tick up
         */
        TICK,
        /**
         * Don't attempt to tick the skill
         */
        PAUSE,
        /**
         * Use the skill
         */
        USE,
    }
}
