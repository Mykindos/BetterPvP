package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.FlashData;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Flash extends Skill implements InteractSkill, Listener {

    private final WeakHashMap<Player, FlashData> charges = new WeakHashMap<>();

    // Action bar
    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();

        // Only display charges in hotbar if holding the weapon
        if (player == null || !charges.containsKey(player) || !UtilPlayer.isHoldingItem(player, getItemsBySkillType())) {
            return null; // Skip if not online or not charging
        }

        final int maxCharges = getMaxCharges(getLevel(player));
        final int newCharges = charges.get(player).getCharges();

        return Component.text(getName() + " ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(newCharges)).color(NamedTextColor.GREEN))
                .append(Component.text("\u25A0".repeat(newCharges >= maxCharges ? 0 : 1)).color(NamedTextColor.YELLOW))
                .append(Component.text("\u25A0".repeat(Math.max(0, maxCharges - newCharges - 1))).color(NamedTextColor.RED));
    });

    private int baseMaxCharges;
    private double baseRechargeSeconds;
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
        return new String[] {
                "Right click with an Axe to activate",
                "",
                "Teleport <stat>" + teleportDistance + "</stat> blocks forward",
                "in the direction you are facing",
                "",
                "Store up to <val>" + getMaxCharges(level) + "</val> charges",
                "",
                "Gain a charge every: <stat>" + getRechargeSeconds(level) + "</stat> seconds"
        };
    }

    private int getMaxCharges(int level) {
        return baseMaxCharges + (level - 1);
    }

    private double getRechargeSeconds(int level) {
        return baseRechargeSeconds;
    }

    @Override
    public void loadSkillConfig() {
        baseMaxCharges = getConfig("baseMaxCharges", 1, Integer.class);
        baseRechargeSeconds = getConfig("baseRechargeSeconds", 4.0, Double.class);
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

    @Override
    public boolean canUse(Player player) {
        if (charges.containsKey(player) && charges.get(player).getCharges() > 0) {
            return true;
        }

        UtilMessage.simpleMessage(player, getClassType().getName(), "You don't have any <alt>" + getName() + "</alt> charges.");
        return false;
    }

    private boolean wouldCollide(Block block, BoundingBox boundingBox) {
        return !block.isPassable() && UtilBlock.doesBoundingBoxCollide(boundingBox, block);
    }

    @Override
    public void invalidatePlayer(Player player) {
        charges.remove(player);
        // Action bar
        final Optional<Gamer> gamerOpt = championsManager.getGamers().getObject(player.getUniqueId());
        gamerOpt.ifPresent(gamer -> gamer.getActionBar().remove(actionBarComponent));
    }

    @Override
    public void trackPlayer(Player player) {
        charges.computeIfAbsent(player, k -> new FlashData());
        // Action bar
        final Optional<Gamer> gamerOpt = championsManager.getGamers().getObject(player.getUniqueId());
        gamerOpt.ifPresent(gamer -> gamer.getActionBar().add(900, actionBarComponent));
    }

    @Override
    public void activate(Player player, int level) {
        // Iterate from their location to their destination
        // Modify the base location by the direction they are facing
        Location teleportLocation = player.getLocation();
        final Vector direction = player.getEyeLocation().getDirection();

        final int iterations = (int) Math.ceil(teleportDistance / 0.2f);
        for (int i = 1; i <= iterations; i++) {
            // Extend their location by the direction they are facing by 0.2 blocks per iteration
            final Vector increment = direction.clone().multiply(0.2 * i);
            final Location newLocation = player.getLocation().add(increment);

            // Get the bounding box of the player as if they were standing on the new location
            BoundingBox relativeBoundingBox = UtilLocation.copyAABBToLocation(player.getBoundingBox(), newLocation);

            // Only cancel for collision if the block isn't passable AND we hit its collision shape
            final Location blockOnTop = newLocation.clone().add(0, 1.0, 0);
            if (wouldCollide(blockOnTop.getBlock(), relativeBoundingBox)) {
                break;
            }

            // We know they won't suffocate because we checked the block above them
            // Now check their feet and see if we can skip this block to allow for through-block flash
            Location newTeleportLocation = newLocation;
            if (wouldCollide(newLocation.getBlock(), relativeBoundingBox)) {
                // If the block at their feet is not passable, try to skip it IF
                // and ONLY IF there isn't a third block above forming a 1x1 gap
                // This allows for through-block flash
                if (!blockOnTop.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()) {
//                if (wouldCollide(blockOnTop.clone().add(0.0, 1.0, 0.0).getBlock(), relativeBoundingBox)) {
                    break;
                }

                // At this point, we can ATTEMPT to skip the block at their feet
                final Vector horizontalIncrement = increment.clone().setY(0);
                final Location frontLocation = player.getLocation().add(horizontalIncrement);
                relativeBoundingBox = UtilLocation.copyAABBToLocation(player.getBoundingBox(), frontLocation);
                if (wouldCollide(frontLocation.getBlock(), relativeBoundingBox)) {
                    continue; // Cancel if that block we're skipping to is not passable
                }

                newTeleportLocation = frontLocation;
            }

            final Location headBlock = newLocation.clone().add(0.0, relativeBoundingBox.getHeight(), 0.0);
            if (wouldCollide(headBlock.getBlock(), relativeBoundingBox)) {
                break; // Stop raying if we hit a block above their head
            }

            if (!player.hasLineOfSight(newLocation) && !player.hasLineOfSight(headBlock)) {
                break; // Stop raying if we don't have line of sight
            }

            teleportLocation = newTeleportLocation;
        }

        // Adjust pitch and yaw to match the direction they are facing
        teleportLocation.setPitch(player.getLocation().getPitch());
        teleportLocation.setYaw(player.getLocation().getYaw());

        // Shift them out of the location to avoid PHASING and SUFFOCATION
        player.leaveVehicle();
        teleportLocation = UtilLocation.shiftOutOfBlocks(teleportLocation, player.getBoundingBox());

        // Teleport
        // Asynchronously because, for some reason, spigot fires PlayerInteractEvent twice if the player looks at a block
        // causing them to use the skill again after being teleported
        // teleportAsync somehow fixes that
        player.teleportAsync(teleportLocation);

        // Lessen charges and add cooldown to prevent from instantly getting a flash charge if they're full
        final int curCharges = charges.get(player).getCharges();
        if (curCharges >= getMaxCharges(level)) {
            championsManager.getCooldowns().use(player, getName(), getRechargeSeconds(level), false, true, true);
        }
        final int newCharges = curCharges - 1;
        charges.get(player).setCharges(newCharges);

        // Cues
        notifyCharges(player, newCharges);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4F, 1.2F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SILVERFISH_DEATH, 1.0F, 1.6F);
        final Location lineStart = player.getLocation().add(0.0, player.getHeight() / 2, 0.0);
        final Location lineEnd = teleportLocation.clone().add(0.0, player.getHeight() / 2, 0.0);
        final VectorLine line = VectorLine.withStepSize(lineStart, lineEnd, 0.25f);
        for (Location point : line.toLocations()) {
            Particle.FIREWORKS_SPARK.builder().location(point).count(2).receivers(100).extra(0).spawn();
        }
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
