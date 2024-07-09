package me.mykindos.betterpvp.clans.weapons.impl.cannon.model;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Getter
@CustomLog
public final class Cannon implements SoundProvider {

    private static TextDisplay getOrCreateDisplay(final Cannon cannon, @NotNull final Location location, float viewBlocks, final NamespacedKey key) {
        final Entity backingEntity = cannon.getBackingEntity();
        final PersistentDataContainer pdc = backingEntity.getPersistentDataContainer();
        // If the entity already has a display, return it
        if (pdc.has(key)) {
            final UUID entId = pdc.get(key, CustomDataType.UUID);
            final Entity found = Bukkit.getEntity(Objects.requireNonNull(entId));
            if (found instanceof TextDisplay display && display.isValid()) {
                return (TextDisplay) found;
            }
        }

        // Otherwise, create a new display
        final TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, ent -> {
            ent.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setAlignment(TextDisplay.TextAlignment.CENTER);
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setPersistent(true);
        });
        UtilEntity.setViewRangeBlocks(display, viewBlocks);
        pdc.set(key, CustomDataType.UUID, display.getUniqueId());
        return display;
    }

    public static final long COOLDOWN_LERP_OUT = 5_000L;
    @Setter private long lastFuseTime = 0L;
    @Setter private long lastShotTime = 0L;
    private final @NotNull CannonManager cannonManager;
    private final @NotNull UUID uuid;
    private final @NotNull UUID placedBy;
    private @NotNull IronGolem backingEntity;
    private final @NotNull ModeledEntity modeledEntity;
    private @NotNull ActiveModel activeModel;
    private TextDisplay healthBar;
    private TextDisplay instructions;

    private boolean loaded;

    public Cannon(@NotNull CannonManager cannonManager,
                  @NotNull UUID uuid,
                  @NotNull UUID placedBy,
                  @NotNull IronGolem backingEntity,
                  @NotNull ModeledEntity modeledEntity,
                  @NotNull ActiveModel activeModel,
                  boolean loaded) {
        this.cannonManager = cannonManager;
        this.placedBy = placedBy;
        this.uuid = uuid;
        this.backingEntity = backingEntity;
        this.modeledEntity = modeledEntity;
        this.activeModel = activeModel;
        this.loaded = loaded;
    }

    @Override
    public @Nullable Sound apply(@NotNull CustomDamageEvent event) {
        return SoundProvider.DEFAULT.apply(event);
    }

    @Override
    public boolean fromEntity() {
        return false;
    }

    public @NotNull Location getLocation() {
        return this.backingEntity.getLocation();
    }

    public double getHealth() {
        return this.backingEntity.getHealth();
    }

    public void setHealth(double health) {
        this.backingEntity.setHealth(health);
    }

    public void rotate(final @NotNull Vector vector) {
        final Location location = this.backingEntity.getLocation();
        location.setDirection(vector);
        this.backingEntity.setRotation(location.getYaw(), location.getPitch());
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
        this.backingEntity.getPersistentDataContainer().set(ClansNamespacedKeys.CANNON_LOADED, PersistentDataType.BOOLEAN, loaded);
    }

    public boolean isLoaded() {
        return this.backingEntity.getPersistentDataContainer().getOrDefault(ClansNamespacedKeys.CANNON_LOADED, PersistentDataType.BOOLEAN, false);
    }

    public @Nullable TextDisplay getHealthBar() {
        if ((this.healthBar == null || this.healthBar.isDead() || !this.healthBar.isValid())) {
            this.healthBar = getOrCreateDisplay(this, backingEntity.getLocation(), 40, ClansNamespacedKeys.CANNON_HEALTHBAR);
            this.healthBar.setTextOpacity((byte) 160);
        }
        return this.healthBar;
    }

    public @Nullable TextDisplay getInstructions() {
        if ((this.instructions == null || this.instructions.isDead() || !this.instructions.isValid())) {
            this.instructions = getOrCreateDisplay(this, backingEntity.getLocation(), 5, ClansNamespacedKeys.CANNON_LEGEND);
        }
        return this.instructions;
    }

    public void updateTag() {

        if (this.backingEntity.isDead() || !this.backingEntity.isValid()) {
            this.backingEntity = (IronGolem) Objects.requireNonNull(Bukkit.getEntity(backingEntity.getUniqueId()));
            this.activeModel = ModelEngineAPI.getModeledEntity(backingEntity).getModel("cannon").orElseThrow();
            return;
        }

        final TextDisplay health = Objects.requireNonNull(getHealthBar());
        this.activeModel.getBone("text_healthbar").ifPresentOrElse(bone -> {
            health.teleport(bone.getLocation());
            health.text(healthBar());
        }, () -> log.info("Could not find bone 'text_healthbar' in cannon model").submit());

        final TextDisplay instructions = Objects.requireNonNull(getInstructions());

        this.activeModel.getBone("text_legend").ifPresentOrElse(bone -> {
            instructions.teleport(bone.getLocation());
            instructions.text(instructions());
        }, () -> log.warn("Could not find bone 'text_legend' in cannon model").submit());
    }

    private TextComponent healthBar() {
        final double health = getHealth();
        final double maxHealth = Objects.requireNonNull(backingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();
        final ProgressBar progressBar = new ProgressBar((float) (health / maxHealth), 15);
        return progressBar.build();
    }

    public TextComponent instructions() {
        final TextComponent.Builder component = Component.text()
                .append(Component.text("Hold ", NamedTextColor.AQUA)
                        .append(Component.text("Right Click", NamedTextColor.WHITE, TextDecoration.BOLD))
                        .append(Component.text(" to ", NamedTextColor.AQUA))
                        .append(Component.text("Aim", NamedTextColor.WHITE, TextDecoration.BOLD)))
                .appendNewline()
                .append(Component.text("Shift-Right-Click", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" to ", NamedTextColor.AQUA))
                .append(Component.text("Fire", NamedTextColor.WHITE, TextDecoration.BOLD))
                .appendNewline()
                .appendNewline();

        final long fuseTime = (long) (cannonManager.getFuseSeconds() * 1000L);
        final long useCooldown = (long) (cannonManager.getShootCooldownSeconds() * 1000L);
        if (lastShotTime < lastFuseTime) {
            // If the cannon is fusing, display the fuse time
            final double secondsLeft = (lastFuseTime + fuseTime - System.currentTimeMillis()) / 1000d;
            final ProgressBar fuseBar = new ProgressBar((float) (secondsLeft / (fuseTime / 1000d)), 25)
                    .inverted()
                    .withCharacter(' ');
            component.append(fuseBar.build().decoration(TextDecoration.STRIKETHROUGH, true));
        } else if (!UtilTime.elapsed(lastShotTime, useCooldown)) {
            // If the cannon is on cooldown, display the cooldown
            final double secondsLeft = (lastShotTime + useCooldown - System.currentTimeMillis()) / 1000d;
            final TextColor color = ProgressColor.of((float) (secondsLeft / (useCooldown / 1000d))).inverted().getTextColor();
            final String timeText = UtilFormat.formatNumber(secondsLeft, 1);
            component.append(Component.text(timeText + "s", color, TextDecoration.BOLD));
        } else if (isLoaded()) {
            component.append(Component.text("LOADED", NamedTextColor.GOLD, TextDecoration.BOLD));
        } else if (!UtilTime.elapsed(lastShotTime, useCooldown + COOLDOWN_LERP_OUT)) {
            component.append(Component.text("READY", NamedTextColor.GREEN, TextDecoration.BOLD));
        }

        return component.build();
    }
}
