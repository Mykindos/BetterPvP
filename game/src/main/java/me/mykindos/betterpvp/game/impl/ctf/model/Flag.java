package me.mykindos.betterpvp.game.impl.ctf.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.framework.hat.PacketHatController;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.impl.ctf.CaptureTheFlag;
import me.mykindos.betterpvp.game.impl.ctf.controller.FlagInventoryCache;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Flag implements Lifecycled {

    @Getter private final Team team;
    @Getter private final Location baseLocation;
    @Getter @Setter private State state = State.AT_BASE;
    @Getter @Setter private Team holderTeam;
    @Getter @Setter private Player holder;
    @Getter @Setter private double returnCountdown = RETURN_COUNTDOWN;
    @Getter
    private Location currentLocation;
    @Getter
    private final float size;
    private long lastPickup = 0;

    @Getter
    private final FlagBlock display;
    @Getter
    private final FlagPlayerHandler inventoryHandler;
    @Getter
    private final FlagFX fx;

    public static final double RETURN_COUNTDOWN = 12.0;

    public enum State {
        AT_BASE,
        PICKED_UP,
        DROPPED
    }

    public Flag(float size, Team team, Location baseLocation, FlagInventoryCache cache, ClientManager clientManager,
                PacketHatController hatController, EffectManager effectManager, CaptureTheFlag game) {
        this.team = team;
        this.baseLocation = baseLocation.toCenterLocation();
        this.size = size;
        this.currentLocation = this.baseLocation.clone();
        this.display = new FlagBlock(this);
        this.inventoryHandler = new FlagPlayerHandler(this, cache, hatController, effectManager);
        this.fx = new FlagFX(this, clientManager, game);
    }

    public Material getMaterial() {
        return switch (team.getProperties().vanillaColor()) {
            case RED -> Material.RED_BANNER;
            case BLUE -> Material.BLUE_BANNER;
            default -> Material.WHITE_BANNER;
        };
    }

    public boolean canPickup() {
        return state != State.PICKED_UP && System.currentTimeMillis() - lastPickup > 800;
    }

    public void spawn() {
        display.spawn(currentLocation);
    }

    public void tick() {
        // Expiration logic
        if (state == Flag.State.DROPPED) {
            if (returnCountdown > 0) {
                returnCountdown = returnCountdown - (1 / 20.0);
            } else {
                returnToBase();
            }
        } else if (state == State.PICKED_UP) {
            currentLocation = holder.getLocation();
        }

        // Effects
        fx.tick();
        display.tick();
    }

    public void returnToBase() {
        if (state == State.AT_BASE) {
            return;
        }
        Player oldHolder = holder;

        // Update State
        currentLocation = baseLocation.clone();
        state = State.AT_BASE;
        holder = null;
        holderTeam = null;
        returnCountdown = RETURN_COUNTDOWN;

        // Play effects
        display.returnToBase();
        fx.playReturnEffects();
        if (oldHolder!= null) {
            inventoryHandler.drop(oldHolder);
        }
    }

    public void capture() {
        fx.playCaptureEffects(holder, holderTeam);
        returnToBase();
    }

    public void pickup(Team opponent, Player holder) {
        Preconditions.checkState(canPickup(), "Flag cannot be picked up");

        fx.playPickupEffects(holder, opponent);

        // Update state
        lastPickup = System.currentTimeMillis();
        state = State.PICKED_UP;
        this.holder = holder;
        this.holderTeam = opponent;

        // Play effects
        display.pickup(holder);
        inventoryHandler.pickUp(holder);
    }

    public void drop(Location location) {
        if (state != State.PICKED_UP || holder == null) {
            return;
        }
        Player oldHolder = holder;
        Team oldHolderTeam = holderTeam;

        // Update state
        lastPickup = System.currentTimeMillis();
        currentLocation = location;
        state = State.DROPPED;
        holder = null;
        holderTeam = null;

        // Play effects
        display.drop(location);
        inventoryHandler.drop(oldHolder);
        fx.playDropEffects(oldHolder, oldHolderTeam);
    }

    @Override
    public void setup() {
        inventoryHandler.setup();
    }

    @Override
    public void tearDown() {
        inventoryHandler.tearDown();
    }
}