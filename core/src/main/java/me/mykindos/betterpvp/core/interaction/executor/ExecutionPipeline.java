package me.mykindos.betterpvp.core.interaction.executor;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.event.InteractionPostExecuteEvent;
import me.mykindos.betterpvp.core.interaction.event.InteractionPreExecuteEvent;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handles the execution pipeline for interactions.
 * Sets up context, checks delays/multi-input, fires events, and executes the interaction.
 */
@CustomLog
@Singleton
public class ExecutionPipeline {

    /**
     * Execute an interaction with full lifecycle.
     * Returns the result for the caller to handle.
     *
     * @param request the execution request
     * @return the result, or empty if the interaction was delayed/consumed without execution
     */
    public Optional<InteractionResult> execute(@NotNull ExecutionRequest request) {
        InteractionContext context = request.context();
        InteractionState state = request.state();

        // Setup context if this is a new execution
        if (request.setupContext()) {
            setupContext(request, context);
        }

        // Check delays
        if (!checkDelays(request, context, state)) {
            return Optional.empty(); // Delay not passed
        }

        // Check multi-input requirements
        if (!checkMultiInput(request, state)) {
            return Optional.empty(); // Need more inputs
        }

        // Fire pre-execute event
        if (!firePreExecute(request, context)) {
            return Optional.empty(); // Cancelled
        }

        // Execute
        Interaction interaction = request.interaction();
        InteractionResult result = interaction.execute(request.actor(), context, request.itemInstance(), request.itemStack());

        // Fire post-execute event
        firePostExecute(request, context, result);

        return Optional.of(result);
    }

    /**
     * Set up the context for a new execution.
     */
    private void setupContext(@NotNull ExecutionRequest request, @NotNull InteractionContext context) {
        // If this is a root node and we're starting a new chain, reset chain data
        if (request.isNewChain() && request.node().isRoot()) {
            context.resetChain();
            context.set(InteractionContext.HELD_ITEM, request.itemInstance());
        }

        // Start a new execution - clears execution-scoped data and sets INTERACTION_START_TIME
        context.startExecution();
        context.set(InputMeta.FIRST_RUN, true);

        // Apply execution data setup if provided (e.g., TARGET, DAMAGE_EVENT, etc.)
        if (request.executionDataSetup() != null) {
            request.executionDataSetup().accept(context);
        }
    }

    /**
     * Check if delay requirements are met.
     *
     * @return true if delays are satisfied, false if the input should be consumed but not processed
     */
    private boolean checkDelays(@NotNull ExecutionRequest request, @NotNull InteractionContext context,
                                 @NotNull InteractionState state) {
        // Note: Root cooldown checks are done in the router/executor before calling the pipeline
        // This method only handles chained node delays

        // Check minimum delay for chained nodes - if not passed, consume the input but don't proceed
        if (request.node().hasMinimumDelay(context) && state.isInChain()
                && !state.hasMinimumDelayPassed(request.node().getMinimumDelayMillis(context))) {
            return false; // Input consumed but not processed
        }

        return true;
    }

    /**
     * Check if multi-input requirements are met.
     *
     * @return true if input requirements are satisfied, false if more inputs are needed
     */
    private boolean checkMultiInput(@NotNull ExecutionRequest request, @NotNull InteractionState state) {
        InteractionContext context = request.context();

        if (request.node().requiresMultipleInputs(context)) {
            int count = state.incrementInputCounter();
            if (count < request.node().getRequiredInputCount(context)) {
                state.touch();
                return false; // Need more inputs
            }
        }

        return true;
    }

    /**
     * Fire the pre-execute event.
     *
     * @return true if execution should continue, false if cancelled
     */
    private boolean firePreExecute(@NotNull ExecutionRequest request, @NotNull InteractionContext context) {
        InteractionPreExecuteEvent preEvent = new InteractionPreExecuteEvent(
                request.actor(), request.interaction(), request.input(), context,
                request.itemInstance(), request.itemStack());
        UtilServer.callEvent(preEvent);
        return !preEvent.isCancelled();
    }

    /**
     * Fire the post-execute event.
     */
    private void firePostExecute(@NotNull ExecutionRequest request, @NotNull InteractionContext context,
                                  @NotNull InteractionResult result) {
        InteractionPostExecuteEvent postEvent = new InteractionPostExecuteEvent(
                request.actor(), request.interaction(), request.input(), context, result,
                request.itemInstance(), request.itemStack());
        UtilServer.callEvent(postEvent);
    }
}
