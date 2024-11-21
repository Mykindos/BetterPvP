package me.mykindos.betterpvp.progression.profession.fishing.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Singleton
@BPvPListener
public class Sharkbait extends Weapon implements LegendaryWeapon, Listener {

    private List<FishHook> activeHooks;
    private double catchSpeedMultiplier;
    private double radius;

    @Inject
    public Sharkbait(Progression progression) {
        super(progression, "sharkbait");
        this.activeHooks = new ArrayList<>();

    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> description = new ArrayList<>();
        description.add(Component.text("Forged by a blind blacksmith blessed by the sea god Thalrion,", NamedTextColor.WHITE));
        description.add(Component.text("Sharkbait is a legendary fishing rod said to be crafted,", NamedTextColor.WHITE));
        description.add(Component.text("from unbreakable seaweed, tempered star-iron, and", NamedTextColor.WHITE));
        description.add(Component.text("a hook carved from the fang of a leviathan shark.", NamedTextColor.WHITE));
        description.add(Component.text(""));
        description.add(Component.text("This enchanted rod is famed for its ability to lure", NamedTextColor.WHITE));
        description.add(Component.text("and subdue even the mightiest sea predators, its", NamedTextColor.WHITE));
        description.add(Component.text("line unyielding and its hook capable of piercing the toughest scales.", NamedTextColor.WHITE));
        description.add(Component.text(""));
        description.add(Component.text("Its most famous wielder, Captain Idris Graywind, used Sharkbait", NamedTextColor.WHITE));
        description.add(Component.text("to catch and slay Blackjaw, a monstrous shark that terrorized the seas.", NamedTextColor.WHITE));
        description.add(Component.text(""));
        description.add(Component.text("Upon Graywind's victory, a preserved spawn of Blackjaw was mounted onto", NamedTextColor.WHITE));
        description.add(Component.text("the rod, distilling fear into any shark that lays eyes upon it.", NamedTextColor.WHITE));
        description.add(Component.text(""));
        description.add(Component.text("Effects", NamedTextColor.YELLOW));
        description.add(Component.text(" - The hook of Sharkbait acts as a bait, increasing fishing catch speed", NamedTextColor.GRAY));
        description.add(UtilMessage.deserialize("    by <green>%s%%</green> for all nearby fishermen.", UtilFormat.formatNumber((1 - catchSpeedMultiplier )* 100d), 1));
        description.add(Component.text(" - No durability loss", NamedTextColor.GRAY));

        return description;
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStartFishing(PlayerStartFishingEvent event) {
        Player player = event.getPlayer();

        if(matches(player.getInventory().getItemInMainHand())) {
            event.getHook().setWaitTime((int) (event.getHook().getWaitTime() * (catchSpeedMultiplier)));
            activeHooks.add(event.getHook());
            return;
        }

        for(FishHook hook : activeHooks) {
            if(hook != null && hook.isValid()) {
                if (hook.getLocation().distance(event.getHook().getLocation()) <= radius) {
                    event.getHook().setWaitTime((int) (event.getHook().getWaitTime() * (catchSpeedMultiplier)));
                    return;
                }
            }
        }
    }

    @UpdateEvent (delay = 1000)
    public void invalidateHooks() {
        if(activeHooks.isEmpty()) return;
        Iterator<FishHook> iterator = activeHooks.iterator();
        while(iterator.hasNext()) {
            FishHook hook = iterator.next();
            if(hook == null || !hook.isValid() || !UtilBlock.isInLiquid(hook)) {
                iterator.remove();
                return;
            }

            Location center = hook.getLocation();

            new BukkitRunnable() {
                final Collection<Player> receivers = center.getWorld().getNearbyPlayers(center, 48);
                double currentRadius = radius - 3.5;
                int i = 0;
                final Color color1 = Color.fromRGB(125, 122, 180);
                final Color color2 = Color.fromRGB(125, 88, 255);
                @Override
                public void run() {

                    if (i >= 0 && i <= 1) {

                        createCircle(center, currentRadius, 360, receivers, 1, color1, color2);
                        currentRadius += 1;
                    } else if (i > 2) {
                        createCircle(center, currentRadius, 11 - i, receivers, 60, color1, color2);
                    }

                    if (i > 10) {
                        this.cancel();
                    }
                    currentRadius += 0.2;
                    i++;
                }
            }.runTaskTimer(JavaPlugin.getPlugin(Progression.class), 0, 1);
        }
    }

    private void createCircle(Location center, final double radius, int modulusRange, Collection<Player> receivers, int angleFreq, Color color1, Color color2) {
        for (int degree = 0; degree <= 360; degree++) {
            if (!(degree % angleFreq < modulusRange || (degree % angleFreq) > angleFreq - modulusRange)) {
                continue;
            }
            double dx = radius * Math.sin(Math.toRadians(degree));
            double dz = radius * Math.cos(Math.toRadians(degree));
            Location newLoc = new Location(center.getWorld(), center.getX() + dx, center.getY(), center.getZ() + dz);
            Particle.DUST_COLOR_TRANSITION.builder()
                    .colorTransition(color1, color2)
                    .location(newLoc)
                    .receivers(receivers)
                    .spawn();
        }
    }

    @Override
    public boolean hasDurability() {
        return false;
    }

    @Override
    public void loadWeaponConfig() {
        catchSpeedMultiplier = getConfig("catchSpeedMultiplier", 0.7, Double.class);
        radius = getConfig("radius", 6.0, Double.class);
    }
}
