package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.VanguardsMightAbilityPhase;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.VanguardsMightData;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.damage.ModifierOperation;
import me.mykindos.betterpvp.core.combat.damage.ModifierType;
import me.mykindos.betterpvp.core.combat.damage.ModifierValue;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class VanguardsMight extends ChannelSkill implements CooldownSkill, InteractSkill, DefensiveSkill, OffensiveSkill, BuffSkill {

    /**
     * A map to keep track of players who are currently channeling this skill.
     */
    private final WeakHashMap<Player, Long> handRaisedTime = new WeakHashMap<>();

    /**
     * A map to keep track of players who are currently active with this skill.
     * This includes players who are channeling the skill, those in the transference phase, and in the strength-effect
     * phase.
     */
    private final WeakHashMap<Player, VanguardsMightData> data = new WeakHashMap<>();

    /**
     * While the player is channeling (i.e. blocking with a sword), they will see a number percentage in the action bar.
     * This is intended to look similar to what Zarya from ow sees.
     */
    private final DisplayComponent channelPhaseActionBar = new PermanentComponent(
            gamer -> {
                final @Nullable VanguardsMightData abilityData = getValidAbilityData(gamer, VanguardsMightAbilityPhase.CHANNELING);
                if (abilityData == null) return null;

                final int chargeAsPercentage = (int) (abilityData.getCharge() * 100);

                // ex: Charge: 5%
                return getActionBarComponent("Charge", String.valueOf(chargeAsPercentage), "%");
            }
    );

    /**
     * While the player is in the transference phase, they will see a long and fast charging progress bar.
     * Purely cosmetic, this is intended to look like the player is transferring their charge into a strength effect.
     */
    private final DisplayComponent transferencePhaseActionBar = new PermanentComponent(
            gamer -> {
                final @Nullable VanguardsMightData abilityData = getValidAbilityData(gamer, VanguardsMightAbilityPhase.TRANSFERENCE);
                if (abilityData == null) return null;

                ProgressBar progressBar = ProgressBar.withLength(abilityData.getTransferenceCharge(), 25);
                return progressBar.build();
            }
    );

    /**
     * Simply displays the time left for the strength effect in seconds.
     */
    private final DisplayComponent strengthEffectActionBar = new PermanentComponent(
            gamer -> {
                final @Nullable VanguardsMightData abilityData = getValidAbilityData(gamer, VanguardsMightAbilityPhase.STRENGTH_EFFECT);
                if (abilityData == null) return null;

                final String timeLeft = UtilFormat.formatNumber(abilityData.getStrengthEffectTimeLeft(), 1, true);

                // ex: Strength For: 4.2s
                return getActionBarComponentForDuration("Strength", timeLeft);
            }
    );

    /**
     * Used to specify how often the action bars should update. <code>50</code> is the default value for {@link UpdateEvent}.
     * <p>
     * This value is also used in calculations; therefore, it must be declared here (as a constant).
     */
    private final long ACTION_BAR_UPDATE_DELAY = 50;  // In milliseconds

    private double passiveChargePerSecond;
    private double chargePerDamageTaken;
    private double blockDuration;
    private double transferencePhaseDuration;
    private int strengthLevel;
    private double maxStrengthDuration;
    private double maxStrengthDurationIncreasePerLevel;
    private double noDamageAbsorbedMessageDuration;

    @Inject
    public VanguardsMight(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Vanguards Might";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Right click with a Sword to channel",
                "",
                "While channeling, build charge",
                "over time and by absorbing damage.",
                "",
                "Stop channeling to gain <effect>Strength</effect>",
                "for up to " + getValueString(this::getMaxStrengthDuration, level) + " seconds.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    /**
     * A helper method used only to create the action bars for this skill.
     */
    private @Nullable VanguardsMightData getValidAbilityData(Gamer gamer, VanguardsMightAbilityPhase phase) {
        final Player player = gamer.getPlayer();
        if (!(gamer.isOnline() && isHolding(player))) return null;

        final VanguardsMightData abilityData = data.get(player);
        if (abilityData == null || !phase.equals(abilityData.getPhase())) return null;

        return abilityData;
    }

    /**
     * Calculates the maximum strength duration based on the skill level.
     * After channeling, the player will gain strength for up to this duration.
     * The higher ability charge, the longer the duration.
     */
    private double getMaxStrengthDuration(int level) {
        return maxStrengthDuration + ((level - 1) * maxStrengthDurationIncreasePerLevel);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    // entry pt
    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        data.put(player, new VanguardsMightData(0f, VanguardsMightAbilityPhase.CHANNELING));
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, channelPhaseActionBar);
        gamer.getActionBar().add(900, transferencePhaseActionBar);
        gamer.getActionBar().add(900, strengthEffectActionBar);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(channelPhaseActionBar);
        gamer.getActionBar().remove(transferencePhaseActionBar);
        gamer.getActionBar().remove(strengthEffectActionBar);
    }

    // Need to make sure the action bar is displayed only when we want it to; things can get messy otherwise
    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !handRaisedTime.containsKey(gamer.getPlayer()) && isHolding(gamer.getPlayer());
    }

    @Override
    public void doWhenAbilityCancelled(Player player) {
        final @Nullable VanguardsMightData abilityData = data.get(player);
        if (abilityData == null) return;  // unreachable

        handRaisedTime.remove(player);
        abilityData.setCharge(0f);  // lets go straight to the failure msg
        abilityData.setPhase(VanguardsMightAbilityPhase.STRENGTH_EFFECT);
    }

    /**
     * Deactivates the skill for the player, removing them from the active list and clearing their data.
     * Moves players to the transference {@link VanguardsMightAbilityPhase} if they stop channelling or reach the maximum
     * block duration.
     */
    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            final @Nullable Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }

            final @Nullable VanguardsMightData abilityData = data.get(player);
            if (abilityData == null) {
                iterator.remove();
                continue;
            }

            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                data.remove(player);
                continue;
            }

            // If they just started channeling, track the time they raised their hand
            if (player.isHandRaised() && !handRaisedTime.containsKey(player)) {
                handRaisedTime.put(player, System.currentTimeMillis());
                continue;
            }

            // If player is not channeling, remove them from active
            if (abilityData.getPhase() != VanguardsMightAbilityPhase.CHANNELING) {
                iterator.remove();
                continue;
            }

            // If player stops blocking, move them to next phase
            if (!player.isHandRaised() && handRaisedTime.containsKey(player)) {
                startTransferencePhase(player, abilityData);
                iterator.remove();
                continue;
            }

            final long blockDurationMillis = (long) (blockDuration * 1000L);
            final boolean hasTimedOut = UtilTime.elapsed(handRaisedTime.get(player), blockDurationMillis);

            // Automatically move to transference phase if the player has been blocking for too long
            if (player.isHandRaised() && hasTimedOut) {
                startTransferencePhase(player, abilityData);
                iterator.remove();
                continue;
            }

            // If player reaches here, they are still channeling
            final Location loc = player.getLocation().add(0, 1, 0);
            spawnParticlesForSkill(loc, 1.2, 10, 0.5f);

            // lower pitch sounds more eerie
            player.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
        }
    }

    /**
     * Used to spawn particles around the player while they are channeling the skill or when the strength effect is applied.
     */
    private void spawnParticlesForSkill(Location loc, double radius, int points, float size) {
        final Color purpleColor = Color.fromRGB(255, 0, 255);

        final ParticleBuilder particleBuilder = Particle.DUST.builder()
                .offset(0, 0, 0)
                .color(purpleColor, size);

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            particleBuilder
                    .location(loc.clone().add(x, -0.3, z))
                    .receivers(60, true)
                    .count(1)
                    .spawn();

            particleBuilder
                    .location(loc.clone().add(x, 1.2, z))
                    .receivers(60, true)
                    .count(1)
                    .spawn();
        }
    }

    /**
     * Listens for when the Vanguards Might user takes damage. Then we cancel the damage and add to the player's charge;
     * thus, "absorbing" the damage.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageTaken(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player player)) return;

        int level = getLevel(player);
        if (level <= 0) return;

        // if they're not channeling, do nothing
        if (!handRaisedTime.containsKey(player)) return;

        final @Nullable VanguardsMightData abilityData = data.get(player);
        if (abilityData == null) return;

        // 0 -> 1; if chargePerDamageTaken is 2.0, then rateAtDamageTaken will be 0.02
        final double rateAtDamageTaken = chargePerDamageTaken / 100.0;
        final double addedCharge = event.getDamage() * rateAtDamageTaken;
        float newCharge = abilityData.getCharge() + (float) addedCharge;

        // Update charge & play sound
        abilityData.setCharge(newCharge);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8F, 2.0F);

        event.getDamageModifiers().addModifier(ModifierType.DAMAGE, 100, getName(), ModifierValue.PERCENTAGE, ModifierOperation.DECREASE);
        event.setKnockback(false);
    }

    /**
     * Initiates transferring the player's charge gained from channeling into duration for the strength effect
     * phase.
     * <p>
     * This phase acts as a tradeoff for this skill since the player has to wait to get the buff.
     */
    private void startTransferencePhase(Player player, VanguardsMightData abilityData) {
        abilityData.setPhase(VanguardsMightAbilityPhase.TRANSFERENCE);
        handRaisedTime.remove(player);

        // If no damage was absorbed, skip the transference phase and go straight to the strength effect phase
        if (abilityData.getCharge() <= 0f) {
            abilityData.setPhase(VanguardsMightAbilityPhase.STRENGTH_EFFECT);
            return;
        }

        // Once the transference phase ends, start the strength effect phase
        long transferencePhaseDurationInTicks = (long) (transferencePhaseDuration * 20L);
        UtilServer.runTaskLater(champions, () -> {
            abilityData.setPhase(VanguardsMightAbilityPhase.STRENGTH_EFFECT);
        }, transferencePhaseDurationInTicks);
    }

    /**
     * This method is called every 50ms to update the action bars for players who are not currently channeling the skill
     * but are still using it. This method is also responsible for updating the strength effect action bar.
     * This method is also responsible for doing the passive charging during the Channeling Phase.
     */
    @UpdateEvent(delay = ACTION_BAR_UPDATE_DELAY)
    public void updateActionBars() {
        final Iterator<Map.Entry<Player, VanguardsMightData>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, VanguardsMightData> entry = iterator.next();
            final Player player = entry.getKey();
            final VanguardsMightData abilityData = entry.getValue();

            if (!player.isOnline()) {
                iterator.remove();
                continue;
            }

            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            final @NotNull VanguardsMightAbilityPhase phase = abilityData.getPhase();

            if (phase.equals(VanguardsMightAbilityPhase.TRANSFERENCE)) {
                updateActionBarForTransferencePhase(abilityData, player);

            } else if (phase.equals(VanguardsMightAbilityPhase.STRENGTH_EFFECT)) {
                if (abilityData.isAlreadyAppliedStrengthEffectOrActionBar()) {

                    // this update event is called every 50ms or every 0.05 seconds
                    final double delayInSeconds = ACTION_BAR_UPDATE_DELAY / 1000d;  // if delay=50 -> delayInSeconds=0.05
                    abilityData.setStrengthEffectTimeLeft(abilityData.getStrengthEffectTimeLeft() - delayInSeconds);
                    continue;
                }

                applyStrengthEffectAndAddToActionBar(abilityData, player, level);
                abilityData.setAlreadyAppliedStrengthEffectOrActionBar(true);

            } else if (phase.equals(VanguardsMightAbilityPhase.CHANNELING)) {  // passive charging is done here

                final float chargePerSecondAsFloat = (float) passiveChargePerSecond;

                // This is just in case the delay of this update event ever changes.
                // So things have to be calculated dynamically here.
                final float ticksPerSecond = 1000f / ACTION_BAR_UPDATE_DELAY;
                final float additionalCharge = chargePerSecondAsFloat / ticksPerSecond;

                abilityData.setCharge(abilityData.getCharge() + additionalCharge);
            }
        }
    }

    /**
     * This method's purpose to update the transference charge (which is displayed cosmetically in the action bar).
     * This phase lasts for 0.5 seconds, and every 0.1 seconds, the player gains 0.1 charge.
     * <p>
     * See transferenceCharge in {@link VanguardsMightData} for more information.
     * <p>
     * The standard "charging up" sound is also played here to inform the player that something is happening.
     */
    private void updateActionBarForTransferencePhase(@NotNull VanguardsMightData abilityData, @NotNull Player player) {
        final float addedCharge = (float) (transferencePhaseDuration / 5f);  // 0.5 seconds of transference phase gives 0.1 charge
        float newCharge = abilityData.getTransferenceCharge() + addedCharge;
        if (newCharge > 1f) {
            newCharge = 1f;  // Cap the charge at 1
        }

        abilityData.setTransferenceCharge(newCharge);
        abilityData.playChargeSound(player, abilityData.getTransferenceCharge());
    }

    /**
     * This method's purpose is to apply the strength effect to the player after they have completed the transference
     * phase. If no damage was absorbed, it will notify the player with a message in the action bar.
     * <p>
     * If damage was absorbed, it will apply the strength effect and display the strength effect's duration in the
     * action bar.
     * <p>
     * After everything, the cooldowns will be cleaned up and set properly.
     */
    private void applyStrengthEffectAndAddToActionBar(@NotNull VanguardsMightData abilityData, @NotNull Player player, int level) {
        final Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();

        // (condition) ? failure : success
        final SoundEffect soundEffectToPlay;
        final double calculatedCharge;

        if (abilityData.getCharge() <= 0f) {
            soundEffectToPlay = new SoundEffect(Sound.BLOCK_BEACON_DEACTIVATE, 0.5f);  // failure sound
            calculatedCharge = 0.0;

            final TextComponent message = Component.text("No Damage Absorbed").color(NamedTextColor.DARK_RED);
            gamer.getActionBar().add(400, new TimedComponent(noDamageAbsorbedMessageDuration, true, gmr -> message));
        } else {
            soundEffectToPlay = new SoundEffect(Sound.BLOCK_ANVIL_LAND, 1.5f);  // success sound
            calculatedCharge = abilityData.getCharge() * getMaxStrengthDuration(level);
            final long strengthDuration = (long) (calculatedCharge * 1000L);

            championsManager.getEffects().addEffect(player, player, EffectTypes.STRENGTH, getName(), strengthLevel, strengthDuration, false);
            abilityData.setStrengthEffectTimeLeft(calculatedCharge);

            spawnParticlesForSkill(player.getLocation(), 2.5, 40, 0.9f);
        }

        // Strength effect sound
        soundEffectToPlay.play(player.getLocation());

        // Start cooldown when strength effect phase ends
        UtilServer.runTaskLater(champions, () -> {
            championsManager.getCooldowns().removeCooldown(player, getName(), true);
            championsManager.getCooldowns().use(player,
                    getName(),
                    getCooldown(level),
                    showCooldownFinished(),
                    true,
                    isCancellable(),
                    this::shouldDisplayActionBar);

            data.remove(player);  // ability over
        }, (long) calculatedCharge * 20L);
    }

    // Defensive Stance showed shield so we should too
    @Override
    public boolean isShieldInvisible() {
        return false;
    }

    // Defensive Stance showed shield so we should too
    @Override
    public boolean shouldShowShield(Player player) {
        return handRaisedTime.containsKey(player);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public void loadSkillConfig() {
        passiveChargePerSecond = getConfig("passiveChargePerSecond ", 0.5, Double.class);
        chargePerDamageTaken = getConfig("chargePerDamageTaken", 2.0, Double.class);
        blockDuration = getConfig("blockDuration", 3.0, Double.class);
        transferencePhaseDuration = getConfig("transferencePhaseDuration", 0.5, Double.class);
        strengthLevel = getConfig("strengthLevel", 1, Integer.class);
        maxStrengthDuration = getConfig("maxStrengthDuration", 3.0, Double.class);
        maxStrengthDurationIncreasePerLevel = getConfig("maxStrengthDurationIncreasePerLevel", 2.5, Double.class);
        noDamageAbsorbedMessageDuration = getConfig("noDamageAbsorbedMessageDuration", 2.0, Double.class);
    }
}
