package me.mykindos.betterpvp.core.interaction.condition;

/**
 * Built-in interaction conditions.
 */
public final class InteractionConditions {

    private InteractionConditions() {
        // Utility class
    }

    /**
     * Condition that requires the actor to be on the ground.
     */
    public static final InteractionCondition ON_GROUND = (actor, context) ->
            actor.isOnGround()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You must be on the ground.");

    /**
     * Condition that requires the actor to be in the air.
     */
    public static final InteractionCondition IN_AIR = (actor, context) ->
            !actor.isOnGround()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You must be in the air.");

    /**
     * Condition that requires the actor to not be in liquid.
     */
    public static final InteractionCondition NOT_IN_LIQUID = (actor, context) ->
            !actor.isInLiquid()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You cannot use this in water or lava.");

    /**
     * Condition that requires the actor to be in liquid.
     */
    public static final InteractionCondition IN_LIQUID = (actor, context) ->
            actor.isInLiquid()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You must be in water or lava.");

    /**
     * Condition that requires the actor to be sneaking.
     */
    public static final InteractionCondition SNEAKING = (actor, context) ->
            actor.isSneaking()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You must be sneaking.");

    /**
     * Condition that requires the actor to not be sneaking.
     */
    public static final InteractionCondition NOT_SNEAKING = (actor, context) ->
            !actor.isSneaking()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You cannot be sneaking.");

    /**
     * Condition that requires the actor to be sprinting.
     */
    public static final InteractionCondition SPRINTING = (actor, context) ->
            actor.isSprinting()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You must be sprinting.");

    /**
     * Condition that requires the actor to not be silenced.
     */
    public static final InteractionCondition NOT_SILENCED = (actor, context) ->
            !actor.isSilenced()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You are silenced.");

    /**
     * Condition that requires the actor to not be stunned.
     */
    public static final InteractionCondition NOT_STUNNED = (actor, context) ->
            !actor.isStunned()
                    ? ConditionResult.success()
                    : ConditionResult.fail("You are stunned.");

    /**
     * Condition that requires the actor to be valid (online and alive).
     */
    public static final InteractionCondition VALID_ACTOR = (actor, context) ->
            actor.isValid()
                    ? ConditionResult.success()
                    : ConditionResult.fail("Invalid actor.");

    /**
     * Create a condition that requires the actor to have at least the specified amount of energy.
     *
     * @param amount the minimum energy required
     * @return an energy condition
     */
    public static InteractionCondition hasEnergy(double amount) {
        return (actor, context) ->
                actor.hasEnergy(amount)
                        ? ConditionResult.success()
                        : ConditionResult.fail("Not enough energy.");
    }

    /**
     * Create a condition that always passes.
     *
     * @return an always-passing condition
     */
    public static InteractionCondition always() {
        return (actor, context) -> ConditionResult.success();
    }

    /**
     * Create a condition that always fails with a message.
     *
     * @param message the failure message
     * @return an always-failing condition
     */
    public static InteractionCondition never(String message) {
        return (actor, context) -> ConditionResult.fail(message);
    }
}
