package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
public class VanguardsMight extends ChannelSkill implements CooldownSkill, InteractSkill, DefensiveSkill, OffensiveSkill {

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

    // Probably can combine this with some of the existing methods in getActionBar but I don't feel like it
    private final DisplayComponent channelPhaseActionBar = new PermanentComponent(
            gamer -> {

                final Player player = gamer.getPlayer();
                final boolean isPlayerChanneling = data.containsKey(player) && data.get(player).getPhase().equals(AbilityPhase.CHANNELING);
                if (!(gamer.isOnline() && isPlayerChanneling && isHolding(player))) return null;

                final @Nullable VanguardsMightData abilityData = data.get(player);
                if (abilityData == null) return null;

                final int chargeAsPercentage = (int) (abilityData.getCharge() * 100);
                return Component.text(chargeAsPercentage )
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text("%").color(NamedTextColor.GRAY));
            }
    );

    private final DisplayComponent transferencePhaseActionBar = new PermanentComponent(
            gamer -> {
                final Player player = gamer.getPlayer();
                final boolean isPlayerTransferring = data.containsKey(player) && data.get(player).getPhase().equals(AbilityPhase.TRANSFERENCE);
                if (!(gamer.isOnline() && isPlayerTransferring && isHolding(player))) return null;

                final @Nullable VanguardsMightData abilityData = data.get(player);
                if (abilityData == null) return null;

                ProgressBar progressBar = ProgressBar.withLength(abilityData.getTransferenceCharge(), 25);
                return progressBar.build();
            }
    );

    private final DisplayComponent strengthEffectActionBar = new PermanentComponent(
            gamer -> {
                final Player player = gamer.getPlayer();
                final boolean isPlayerInStrengthEffectPhase = data.containsKey(player) && data.get(player).getPhase().equals(AbilityPhase.STRENGTH_EFFECT);
                if (!(gamer.isOnline() && isPlayerInStrengthEffectPhase && isHolding(player))) return null;

                final @Nullable VanguardsMightData abilityData = data.get(player);
                if (abilityData == null) return null;

                String timeLeftWithOneDecimalPlace = UtilFormat.formatNumber(abilityData.getStrengthEffectTimeLeft(), 1);
                return Component.text(timeLeftWithOneDecimalPlace)
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text("s").color(NamedTextColor.GRAY));
            }
    );

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
                "Block with a Sword to channel",
                "",
                "While channeling, absorb all damage",
                "damage, thus, charging this ability.",
                "",
                "Stop channeling to gain <effect>Strength</effect>",
                "for up to " + getValueString(this::getMaxStrengthDuration, level) + " seconds.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
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

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());

        data.put(player, new VanguardsMightData(0f, AbilityPhase.CHANNELING));

        // Add sound
        // Add activation particles
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

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !handRaisedTime.containsKey(gamer.getPlayer()) && isHolding(gamer.getPlayer());
    }

    /**
     * Deactivates the skill for the player, removing them from the active list and clearing their data.
     * Moves players to the transference {@link AbilityPhase} if they stop channelling or reach the maximum
     * block duration.
     */
    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> iterator = active.iterator();
        // todo: change the initial charging of the skill to use numbers instead of charge data
        // then change the transfer phase to use the charge data to go from like 0 to 1 and make the bar super long
        // it'll look really cool. OOO CHANGE THE COLOR TO LIKE GOLD OR PURPLE YEAH LIGHT PURPLE
        // and when you do particles, make them all purple and gold AYY THE LAKERS
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
            if (!abilityData.getPhase().equals(AbilityPhase.CHANNELING)) {
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
            }
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

        // update charge + play old defensive stance sound
        abilityData.setCharge(abilityData.getCharge() + (float) addedCharge);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 2.0F);

        event.setKnockback(false);
        event.setDamage(0);
    }

    /**
     * Initiates transferring the player's charge gained from channeling into duration for the strength effect
     * phase.
     * <p>
     * This phase acts as a tradeoff for this skill since the player is not mobile during this phase; however,
     * you become much more lethal after absorbing damage.
     */
    private void startTransferencePhase(Player player, VanguardsMightData abilityData) {
        abilityData.setPhase(AbilityPhase.TRANSFERENCE);
        handRaisedTime.remove(player);

        // If no damage was absorbed, skip the transference phase and go straight to the strength effect phase
        if (abilityData.getCharge() <= 0f) {
            startStrengthEffectPhase(player, abilityData);
            return;
        }

        // Once the transference phase ends, start the strength effect phase
        long transferencePhaseDurationInTicks = (long) (transferencePhaseDuration * 20L);
        UtilServer.runTaskLater(champions, () -> startStrengthEffectPhase(player, abilityData), transferencePhaseDurationInTicks);
    }

    private void startStrengthEffectPhase(Player player, VanguardsMightData abilityData) {
        abilityData.setPhase(AbilityPhase.STRENGTH_EFFECT);
    }

    @UpdateEvent
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

            AbilityPhase phase = abilityData.getPhase();

            if (phase.equals(AbilityPhase.TRANSFERENCE)) {
                updateActionBarForTransferencePhase(abilityData, player);

            } else if (phase.equals(AbilityPhase.STRENGTH_EFFECT)) {
                if (abilityData.alreadyAppliedStrengthEffectOrActionBar) {

                    // this update event is called every 50ms or every 0.05 seconds
                    abilityData.setStrengthEffectTimeLeft(abilityData.getStrengthEffectTimeLeft() - 0.05);
                    continue;
                }

                applyStrengthEffectAndAddToActionBar(abilityData, player, level);
                abilityData.alreadyAppliedStrengthEffectOrActionBar = true;
            }

        }
    }

    private void updateActionBarForTransferencePhase(@NotNull VanguardsMightData abilityData, @NotNull Player player) {
        final float addedCharge = (float) (transferencePhaseDuration / 5f);  // 0.5 seconds of transference phase gives 0.1 charge
        float newCharge = abilityData.getTransferenceCharge() + addedCharge;
        if (newCharge > 1f) {
            newCharge = 1f;  // Cap the charge at 1
        }

        abilityData.setTransferenceCharge(newCharge);
        ChargeData.playChargeSound(player, abilityData.getTransferenceCharge());
    }

    private void applyStrengthEffectAndAddToActionBar(@NotNull VanguardsMightData abilityData, @NotNull Player player, int level) {
        final Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();

        final double calculatedCharge;
        if (abilityData.getCharge() <= 0f) {
            calculatedCharge = 0.0;

            final TextComponent message = Component.text("No Damage Absorbed").color(NamedTextColor.DARK_RED);
            gamer.getActionBar().add(400, new TimedComponent(noDamageAbsorbedMessageDuration, true, gmr -> message));

            // todo: play failure sound
        } else {
            calculatedCharge = abilityData.getCharge() * getMaxStrengthDuration(level);
            final long strengthDuration = (long) (calculatedCharge * 1000L);

            championsManager.getEffects().addEffect(player, player, EffectTypes.STRENGTH, getName(), strengthLevel, strengthDuration, false);
            abilityData.setStrengthEffectTimeLeft(calculatedCharge);

            // todo: subtle particles and full charge sound
        }

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

    @Override
    public boolean isShieldInvisible() {
        return false;
    }

    @Override
    public boolean shouldShowShield(Player player) {
        return !championsManager.getCooldowns().hasCooldown(player, getName());
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
        chargePerDamageTaken = getConfig("chargePerDamageTaken", 2.0, Double.class);
        blockDuration = getConfig("blockDuration", 3.0, Double.class);
        transferencePhaseDuration = getConfig("transferencePhaseDuration", 0.5, Double.class);
        strengthLevel = getConfig("strengthLevel", 1, Integer.class);
        maxStrengthDuration = getConfig("maxStrengthDuration", 8.0, Double.class);
        maxStrengthDurationIncreasePerLevel = getConfig("maxStrengthDurationIncreasePerLevel", 1.0, Double.class);
        noDamageAbsorbedMessageDuration = getConfig("noDamageAbsorbedMessageDuration", 2.0, Double.class);
    }

    @Data
    private static class VanguardsMightData {
        private @NotNull Float charge;
        private @NotNull AbilityPhase phase;
        private Float transferenceCharge = 0f;
        private Boolean alreadyAppliedStrengthEffectOrActionBar = false;
        private Double strengthEffectTimeLeft = 0.0;
    }

    private enum AbilityPhase {
        CHANNELING,  // Player is channeling the skill
        TRANSFERENCE,  // Player is in the transference phase after channeling
        STRENGTH_EFFECT  // Player has gained strength effect after ability charge has transferred
    }
}
