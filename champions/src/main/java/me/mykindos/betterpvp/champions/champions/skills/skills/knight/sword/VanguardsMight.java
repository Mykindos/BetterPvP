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
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(
            gmr -> {

                final Player player = gmr.getPlayer();
                final boolean isPlayerChanneling = data.containsKey(player) && data.get(player).getPhase().equals(AbilityPhase.CHANNELING);
                return gmr.isOnline() && isPlayerChanneling && isHolding(player);
            },
            gmr -> data.get(gmr.getPlayer()).getChargeData()
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

        final ChargeData chargeData = new ChargeData(0);
        data.put(player, new VanguardsMightData(chargeData, AbilityPhase.CHANNELING));

        // Add sound
        // Add activation particles
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
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

        // update charge + play sound
        abilityData.chargeData.setCharge(abilityData.getChargeData().getCharge() + (float) addedCharge);
        abilityData.getChargeData().tickSound(player);

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

        // Safe to assume that this is greater than 0
        int level = getLevel(player);

        // Once the transference phase ends, start the strength effect phase
        long transferencePhaseDurationInTicks = (long) (transferencePhaseDuration * 20L);
        UtilServer.runTaskLater(champions, () -> startStrengthEffectPhase(player, abilityData), transferencePhaseDurationInTicks);

        // todo: run a task timer and play a sound that pitches up over time to signify charging

        final Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        final TextComponent message = Component.text("Transferring charge...").color(NamedTextColor.LIGHT_PURPLE);

        // Nice transition message; after this message plays, the cooldown will finally start
        gamer.getActionBar().add(400, new TimedComponent(transferencePhaseDuration, true, gmr -> message));
    }

    private void startStrengthEffectPhase(Player player, VanguardsMightData abilityData) {
        abilityData.setPhase(AbilityPhase.STRENGTH_EFFECT);
    }

    @UpdateEvent
    public void updateActionBarForStrengthEffectPhase() {
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

            if (!abilityData.getPhase().equals(AbilityPhase.STRENGTH_EFFECT)) {
                continue;
            }

            final double calculatedCharge;
            if (abilityData.getChargeData().getCharge() <= 0f) {
                calculatedCharge = 0.0;

                final Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
                final TextComponent message = Component.text("No Damage Absorbed").color(NamedTextColor.DARK_RED);
                gamer.getActionBar().add(400, new TimedComponent(noDamageAbsorbedMessageDuration, true, gmr -> message));

                // todo: play failure sound
            } else {
                calculatedCharge = abilityData.getChargeData().getCharge() * getMaxStrengthDuration(level);
                final long strengthDuration = (long) (calculatedCharge * 1000L);

                championsManager.getEffects().addEffect(player, player, EffectTypes.STRENGTH, getName(), strengthLevel, strengthDuration, false);

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
        transferencePhaseDuration = getConfig("transferencePhaseDuration", 2.0, Double.class);
        strengthLevel = getConfig("strengthLevel", 1, Integer.class);
        maxStrengthDuration = getConfig("maxStrengthDuration", 5.0, Double.class);
        maxStrengthDurationIncreasePerLevel = getConfig("maxStrengthDurationIncreasePerLevel", 1.0, Double.class);
        noDamageAbsorbedMessageDuration = getConfig("noDamageAbsorbedMessageDuration", 2.0, Double.class);
    }

    @Data
    private class VanguardsMightData {
        private @NotNull ChargeData chargeData;
        private @NotNull AbilityPhase phase;
    }

    private enum AbilityPhase {
        CHANNELING,  // Player is channeling the skill
        TRANSFERENCE,  // Player is in the transference phase after channeling
        STRENGTH_EFFECT  // Player has gained strength effect after ability charge has transferred
    }
}
