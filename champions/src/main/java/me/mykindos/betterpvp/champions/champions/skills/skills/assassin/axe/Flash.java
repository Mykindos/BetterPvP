package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.FlashData;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Flash extends Skill implements InteractSkill, Listener, MovementSkill {

    private final WeakHashMap<Player, FlashData> charges = new WeakHashMap<>();

    // Action bar
    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();

        // Only display charges in hotbar if holding the weapon
        if (player == null || !charges.containsKey(player) || !isHolding(player)) {
            return null; // Skip if not online or not charging
        }

        final int maxCharges = getMaxCharges(getLevel(player));
        final int newCharges = charges.get(player).getCharges();

        return Component.text(getName() + " ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(newCharges)).color(NamedTextColor.GREEN))
                .append(Component.text("\u25A0".repeat(Math.max(0, maxCharges - newCharges))).color(NamedTextColor.RED));
    });

    private int baseMaxCharges;

    private int chargeIncreasePerLevel;

    private double baseRechargeSeconds;

    private double rechargeReductionPerLevel;
    private double teleportDistance;

    @Inject
    public Flash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Flash";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Teleport " + getValueString(this::getTeleportDistance, level) + " blocks forward",
                "in the direction you are facing",
                "",
                "Store up to " + getValueString(this::getMaxCharges, level) + " charges",
                "",
                "Cannot be used while <effect>Slowed</effect>",
                "",
                "Gain a charge every: " + getValueString(this::getRechargeSeconds, level) + " seconds"
        };
    }

    private int getMaxCharges(int level) {
        return baseMaxCharges + ((level - 1) * chargeIncreasePerLevel);
    }

    private double getRechargeSeconds(int level) {
        return baseRechargeSeconds - ((level - 1) * rechargeReductionPerLevel);
    }

    private double getTeleportDistance(int level) {
        return teleportDistance;
    }

    @Override
    public void loadSkillConfig() {
        baseMaxCharges = getConfig("baseMaxCharges", 5, Integer.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 0, Integer.class);
        baseRechargeSeconds = getConfig("baseRechargeSeconds", 9.0, Double.class);
        rechargeReductionPerLevel = getConfig("rechargeReductionPerLevel", 1.0, Double.class);
        teleportDistance = getConfig("teleportDistance", 5.0, Double.class);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean displayWhenUsed() {
        return false;
    }

    private void notifyCharges(Player player, int charges) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "Flash Charges: <alt2>" + charges);
    }

    public boolean canUse(Player player) {
        FlashData flashData = charges.get(player);
        if (flashData != null && flashData.getCharges() > 0) {
            return true;
        }

        UtilMessage.simpleMessage(player, getClassType().getName(), "You don't have any <alt>" + getName() + "</alt> charges.");
        return false;
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        charges.remove(player);
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        charges.computeIfAbsent(player, k -> new FlashData());
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void activate(Player player, int level) {
        final Location origin = player.getLocation();
        UtilLocation.teleportForward(player, teleportDistance, false, success -> {
            player.getWorld().playSound(origin, Sound.ENTITY_WITHER_SHOOT, 0.4F, 1.2F);
            player.getWorld().playSound(origin, Sound.ENTITY_SILVERFISH_DEATH, 1.0F, 1.6F);

            if (!success) {
                return;
            }

            // Lessen charges and add cooldown to prevent from instantly getting a flash charge if they're full
            FlashData flashData = charges.get(player);
            if (flashData == null) {
                return;
            }

            final int curCharges = flashData.getCharges();
            if (curCharges >= getMaxCharges(level)) {
                championsManager.getCooldowns().use(player, getName(), getRechargeSeconds(level), false, true, true);
            }

            final int newCharges = curCharges - 1;
            flashData.setCharges(newCharges);

            // Cues
            notifyCharges(player, newCharges);
            final Location lineStart = origin.add(0.0, player.getHeight() / 2, 0.0);
            final Location lineEnd = player.getLocation().clone().add(0.0, player.getHeight() / 2, 0.0);
            final VectorLine line = VectorLine.withStepSize(lineStart, lineEnd, 0.25f);
            for (Location point : line.toLocations()) {
                Particle.FIREWORKS_SPARK.builder().location(point).count(2).receivers(100).extra(0).spawn();
            }
        });
    }

    @UpdateEvent(delay = 100)
    public void recharge() {
        final Iterator<Map.Entry<Player, FlashData>> iterator = charges.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, FlashData> entry = iterator.next();
            final Player player = entry.getKey();
            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            final FlashData data = entry.getValue();
            final int maxCharges = getMaxCharges(level);

            if (data.getCharges() >= maxCharges) {
                continue; // skip if already at max charges
            }

            if (!championsManager.getCooldowns().use(player, getName(), getRechargeSeconds(level), false, true, true)) {
                continue; // skip if not enough time has passed
            }

            // add a charge
            data.addCharge();
            notifyCharges(player, data.getCharges());
        }
    }

}
