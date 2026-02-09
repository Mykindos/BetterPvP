package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.BattoDoInteraction;
import me.mykindos.betterpvp.champions.item.ability.DashInteraction;
import me.mykindos.betterpvp.champions.item.ability.HardSlashInteraction;
import me.mykindos.betterpvp.champions.item.ability.SelectorInteraction;
import me.mykindos.betterpvp.champions.item.ability.VFXInteraction;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.interaction.timing.Timing;
import me.mykindos.betterpvp.core.interaction.utility.FilterInteraction;
import me.mykindos.betterpvp.core.interaction.utility.SoundInteraction;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.config.ConfigEntry;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntityFilters;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntitySelector;
import me.mykindos.betterpvp.core.utilities.model.selector.origin.EntityOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.ShapeEntitySelector;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

import static me.mykindos.betterpvp.core.interaction.context.InputMeta.DAMAGE_EVENT;

@ItemKey("champions:hinokami_katana")
@PluginAdapter("ModelEngine")
@Singleton
public class KatanaHinokami extends WeaponItem implements Reloadable {

    private final Config config;
    private final ConfigEntry<Double> dashDelay;
    private final ConfigEntry<Double> battoDoCooldown;
    private final DashInteraction dashInteraction;
    private final HardSlashInteraction hardSlashInteraction;
    private final BattoDoInteraction battoDoInteraction;

    private double primaryAttackReach;

    @Inject
    private KatanaHinokami(Champions champions, EffectManager effectManager, CooldownManager cooldownManager) {
        super(champions, "Hinokami Katana", Item.model("hinokami_katana"), ItemRarity.MYTHICAL, List.of(Group.MELEE));
        this.config = Config.item(champions, this);

        // Config entries
        this.dashDelay = new ConfigEntry<>(config, "dash.cooldown", Double.class, 0.6d);
        this.battoDoCooldown = new ConfigEntry<>(config, "batto_do.cooldown", Double.class, 10.0d);

        // Interactions with forked config
        this.dashInteraction = new DashInteraction(cooldownManager, effectManager, config.fork("dash"));
        this.hardSlashInteraction = new HardSlashInteraction(cooldownManager, config.fork("hard-slash"));
        this.battoDoInteraction = new BattoDoInteraction(config.fork("batto_do"));

        addBaseComponent(buildInteractions(cooldownManager));
    }

    private InteractionContainerComponent buildInteractions(CooldownManager cooldownManager) {
        // Utility interactions
        final SoundInteraction slashSFX = SoundInteraction.of(
                new SoundEffect(Sound.ENTITY_BLAZE_BURN, 2f, 1.4f),
                new SoundEffect(Sound.ENTITY_GHAST_SHOOT, 2f, 1.4f)
        );
        final FilterInteraction meleeDamageFilter = FilterInteraction.meleeDamage();

        // VFX interactions
        final VFXInteraction slashFX = createSlashVFX();
        final VFXInteraction hitFX = createHitVFX();
        final VFXInteraction hardSlashFX = createHardSlashVFX();
        final VFXInteraction battoDoFX = createBattoDoVFX();

        // Selector for primary attack
        final SelectorInteraction slashSelector = new SelectorInteraction(this::slashDamage);
        slashSelector.setSelector(this::getPrimarySelector);

        // Batto-Do prepare interaction
        final BattoDoInteraction.PrepareInteraction prepareInteraction =
                new BattoDoInteraction.PrepareInteraction(cooldownManager, battoDoCooldown);

        return InteractionContainerComponent.builder()
                // Dash (Right Click)
                .root(InteractionInputs.RIGHT_CLICK, dashInteraction,
                        Timing.ZERO, Timing.fromConfig(dashDelay))

                // Primary Attack (Left Click)
                .hiddenRoot(InteractionInputs.LEFT_CLICK, slashFX,
                        Timing.millis(1), Timing.fromAttackSpeed(DamageCause.DEFAULT_DELAY))
                .then(slashSFX, slashSelector)

                // Damage FX (on melee hit)
                .hiddenRoot(InteractionInputs.DAMAGE_DEALT, meleeDamageFilter)
                .onComplete(hitFX)

                // Hard Slash (Swap Hand)
                .root(InteractionInputs.SWAP_HAND, hardSlashInteraction)
                .onComplete(hardSlashFX)

                // Batto-Do Ultimate (Hold Sneak)
                .root(InteractionInputs.SNEAK_START, battoDoInteraction.getDisplayName(), battoDoInteraction.getDisplayDescription(), Interaction.EMPTY)
                .chain(InteractionInputs.NONE, Timing.seconds(8), prepareInteraction)
                .chain(InteractionInputs.SNEAK_END, Timing.ZERO, battoDoFX)
                .chain(InteractionInputs.NONE, Timing.ZERO, battoDoInteraction)
                .up().up()
                .chain(InteractionInputs.SNEAK_START, Timing.ZERO, Interaction.EMPTY)

                .build();
    }

    private VFXInteraction createSlashVFX() {
        final VFXInteraction vfx = new VFXInteraction("lsi_kaen_slash", 15L);
        vfx.setLocationMutator((actor, context) ->
                actor.getEntity().getLocation().add(actor.getEntity().getLocation().getDirection().multiply(0.5)));
        vfx.setModelConsumer(model ->
                ModelEngineHelper.randomAnimation(model, "slash_1", "slash_2", "slash_3", "slash_4"));
        return vfx;
    }

    private VFXInteraction createHitVFX() {
        final VFXInteraction vfx = new VFXInteraction("lsi_kaen_hit", 10L);
        vfx.setLocationMutator((actor, context) -> {
            final Entity damagee = context.get(DAMAGE_EVENT).orElseThrow().getDamagee();
            final Vector direction = damagee.getLocation().toVector().subtract(actor.getLocation().toVector()).normalize();
            return damagee.getLocation().add(direction.multiply(-0.5));
        });
        vfx.setModelConsumer(model ->
                ModelEngineHelper.randomAnimation(model, "hit_1", "hit_2", "hit_3", "hit_4"));
        return vfx;
    }

    private VFXInteraction createHardSlashVFX() {
        final VFXInteraction vfx = new VFXInteraction("lsi_kaen_hard_slash", 15L);
        vfx.setModelConsumer(model -> {
            model.setScale(1.2);
            ModelEngineHelper.playAnimation(model, "animation");
        });
        return vfx;
    }

    private VFXInteraction createBattoDoVFX() {
        final VFXInteraction vfx = new VFXInteraction("lsi_kaen_batto", 20L);
        vfx.setModelConsumer(model -> {
            model.setScale(1.0);
            ModelEngineHelper.playAnimation(model, "animation", 0.2f);
        });
        return vfx;
    }

    private void slashDamage(InteractionActor actor, InteractionContext context, LivingEntity target) {
        actor.getEntity().attack(target);
    }

    private EntitySelector<LivingEntity> getPrimarySelector(LivingEntity entity) {
        final AttributeInstance attribute = entity.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        double reach = primaryAttackReach <= 0
                ? (attribute != null ? attribute.getValue() : 3.0)
                : primaryAttackReach;
        return ShapeEntitySelector.arc(new EntityOrigin(entity, true), reach, 90.0, 180.0)
                .withFilter(EntityFilters.combatEnemies().and(EntityFilters.lineOfSight()));
    }

    @Override
    public void reload() {
        super.reload();

        this.battoDoCooldown.fetch();
        this.dashDelay.fetch();

        this.primaryAttackReach = this.config.getConfig("primary-attack.reach-override", 0.0, Double.class);

        this.dashInteraction.loadConfig();
        this.hardSlashInteraction.loadConfig();
        this.battoDoInteraction.loadConfig();
    }
}
