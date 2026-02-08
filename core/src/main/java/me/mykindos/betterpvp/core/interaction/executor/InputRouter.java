package me.mykindos.betterpvp.core.interaction.executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.interaction.state.InteractionStateManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Routes inputs to the appropriate interaction chain node.
 * Consolidates input routing logic from InteractionExecutor and HoldTracker.
 */
@CustomLog
@Singleton
public class InputRouter {

    @Getter
    private final InteractionStateManager stateManager;

    @Inject
    public InputRouter(InteractionStateManager stateManager) {
        this.stateManager = stateManager;
    }

    /**
     * Result of routing an input to a target node.
     */
    public sealed interface RouteResult {
        /**
         * A target node was found for the input.
         *
         * @param node        the target node to execute
         * @param state       the interaction state
         * @param executionId the execution ID
         * @param isNewChain  whether this is starting a new chain (root node, not continuing)
         */
        record TargetFound(InteractionChainNode node, InteractionState state,
                           long executionId, boolean isNewChain) implements RouteResult {}

        /**
         * The input was consumed but no action should be taken.
         * This happens when there's an active chain but the input doesn't match any valid transition.
         */
        record Consumed() implements RouteResult {}

        /**
         * No matching root was found for this input.
         * The input should be ignored.
         */
        record NoMatch() implements RouteResult {}

        /**
         * Multiple roots were found for this input.
         * All should be executed (used for PASSIVE and similar inputs).
         *
         * @param nodes   the list of root nodes to execute
         * @param actorId the actor's unique ID
         * @param chain   the interaction chain
         */
        record MultipleTargets(List<InteractionChainNode> nodes, UUID actorId,
                               InteractionChain chain) implements RouteResult {}
    }

    /**
     * Route an input to the appropriate target node.
     *
     * @param actorId the actor's unique ID
     * @param chain   the interaction chain
     * @param input   the input to route
     * @return the routing result
     */
    public RouteResult route(@NotNull UUID actorId, @NotNull InteractionChain chain, @NotNull InteractionInput input) {
        // Check for active chain
        Optional<InteractionStateManager.ActiveChainInfo> activeChainOpt = stateManager.findActiveChain(actorId, chain);

        if (activeChainOpt.isPresent()) {
            InteractionStateManager.ActiveChainInfo activeChain = activeChainOpt.get();
            InteractionState state = activeChain.state();
            long executionId = activeChain.executionId();

            // Check for timeout
            if (state.hasTimedOut()) {
                cleanupTimedOut(actorId, chain, state, executionId);
                // Fall through to find new root
            } else {
                // Try to find matching child
                RouteResult childResult = findMatchingChild(actorId, chain, activeChain, input);
                if (childResult != null) {
                    return childResult;
                }

                // Try to start parallel chain
                RouteResult parallelResult = findParallelRoot(actorId, chain, activeChain, input);
                if (parallelResult != null) {
                    return parallelResult;
                }

                // Consume input, keep chain intact
                return new RouteResult.Consumed();
            }
        }

        // Find new root
        return findNewRoot(actorId, chain, input);
    }

    /**
     * Clean up a timed out chain state.
     */
    private void cleanupTimedOut(@NotNull UUID actorId, @NotNull InteractionChain chain,
                                  @NotNull InteractionState state, long executionId) {
        // Record chain completion for root cooldown tracking before removing
        InteractionChainNode timedOutRootNode = state.getRootNode();
        if (timedOutRootNode != null) {
            stateManager.recordChainCompletion(actorId, timedOutRootNode.getId());
        }
        stateManager.removeState(actorId, chain, executionId);
    }

    /**
     * Try to find a matching child node for the input.
     *
     * @return the route result if a child was found, null otherwise
     */
    private RouteResult findMatchingChild(@NotNull UUID actorId, @NotNull InteractionChain chain,
                                          @NotNull InteractionStateManager.ActiveChainInfo activeChain,
                                          @NotNull InteractionInput input) {
        InteractionState state = activeChain.state();
        InteractionChainNode currentNode = state.getCurrentNode();

        if (currentNode == null) {
            return null;
        }

        Optional<InteractionChainNode> childOpt = currentNode.findChild(input);
        if (childOpt.isPresent()) {
            return new RouteResult.TargetFound(childOpt.get(), state, activeChain.executionId(), false);
        }

        return null;
    }

    /**
     * Try to find a valid root for starting a parallel chain.
     *
     * @return the route result if a parallel root was found, null otherwise (input should be consumed)
     */
    private RouteResult findParallelRoot(@NotNull UUID actorId, @NotNull InteractionChain chain,
                                          @NotNull InteractionStateManager.ActiveChainInfo activeChain,
                                          @NotNull InteractionInput input) {
        InteractionState state = activeChain.state();

        List<InteractionChainNode> roots = chain.findRoots(input);
        if (roots.isEmpty()) {
            return null; // No root, input will be consumed
        }

        // For parallel chains, we only consider the first unoccupied root
        for (InteractionChainNode potentialRoot : roots) {
            // Check if the existing active chain was started by the same root
            // If so, skip this root and try the next
            InteractionChainNode existingRootNode = state.getRootNode();
            if (existingRootNode != null && existingRootNode.getId() == potentialRoot.getId()) {
                continue; // Same root in progress, try next
            }

            // Different root - check if there's already an active chain for the potential root
            Optional<InteractionStateManager.ActiveChainInfo> existingChainForRoot =
                    stateManager.findActiveChainByRoot(actorId, chain, potentialRoot.getId());

            if (existingChainForRoot.isPresent()) {
                // Check if the existing chain for this root can accept the input
                RouteResult result = handleExistingRootChain(actorId, chain, existingChainForRoot.get(), potentialRoot, input);
                if (result != null) {
                    return result;
                }
                continue; // Try next root
            }

            // No existing chain for this root - start a new parallel chain execution
            return createNewChain(actorId, chain, potentialRoot);
        }

        return null; // All roots occupied or same root in progress
    }

    /**
     * Handle an existing chain for a root node.
     */
    private RouteResult handleExistingRootChain(@NotNull UUID actorId, @NotNull InteractionChain chain,
                                                 @NotNull InteractionStateManager.ActiveChainInfo existingChain,
                                                 @NotNull InteractionChainNode potentialRoot,
                                                 @NotNull InteractionInput input) {
        InteractionState existingState = existingChain.state();

        // Check if this chain has timed out
        if (existingState.hasTimedOut()) {
            cleanupTimedOut(actorId, chain, existingState, existingChain.executionId());
            // Create a new chain for this root
            return createNewChain(actorId, chain, potentialRoot);
        }

        InteractionChainNode existingCurrentNode = existingState.getCurrentNode();

        // Check if the existing chain can accept this input as a child
        if (existingCurrentNode != null) {
            Optional<InteractionChainNode> childOpt = existingCurrentNode.findChild(input);
            if (childOpt.isPresent()) {
                return new RouteResult.TargetFound(childOpt.get(), existingState, existingChain.executionId(), false);
            }
        }

        // Existing chain can't accept this input - consume but don't start new
        return null;
    }

    /**
     * Find a new root for this input.
     */
    private RouteResult findNewRoot(@NotNull UUID actorId, @NotNull InteractionChain chain,
                                     @NotNull InteractionInput input) {
        List<InteractionChainNode> roots = chain.findRoots(input);
        if (roots.isEmpty()) {
            return new RouteResult.NoMatch();
        }

        // For single root, return it directly
        if (roots.size() == 1) {
            return createNewChain(actorId, chain, roots.getFirst());
        }

        // For multiple roots, return a MultipleTargets result
        return new RouteResult.MultipleTargets(roots, actorId, chain);
    }

    /**
     * Create a new chain state for the target root node.
     */
    private RouteResult.TargetFound createNewChain(@NotNull UUID actorId, @NotNull InteractionChain chain,
                                                    @NotNull InteractionChainNode targetNode) {
        long executionId = stateManager.nextExecutionId();
        InteractionState state = stateManager.createState(actorId, chain, executionId);
        state.setRootNode(targetNode);
        return new RouteResult.TargetFound(targetNode, state, executionId, true);
    }

    /**
     * Check if a root cooldown has passed.
     *
     * @param actorId  the actor's unique ID
     * @param node     the root node to check
     * @param context  the interaction context (may be null)
     * @return true if the cooldown has passed or if no cooldown is required
     */
    public boolean hasRootCooldownPassed(@NotNull UUID actorId, @NotNull InteractionChainNode node,
                                          @NotNull InteractionContext context) {
        if (!node.hasMinimumDelay(context)) {
            return true;
        }
        return stateManager.hasRootCooldownPassed(actorId, node.getId(), node.getMinimumDelayMillis(context));
    }

    /**
     * Remove a state if root cooldown fails.
     *
     * @param actorId     the actor's unique ID
     * @param chain       the chain
     * @param executionId the execution ID
     */
    public void removeState(@NotNull UUID actorId, @NotNull InteractionChain chain, long executionId) {
        stateManager.removeState(actorId, chain, executionId);
    }
}
