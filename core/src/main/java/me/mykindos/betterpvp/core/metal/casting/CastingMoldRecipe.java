package me.mykindos.betterpvp.core.metal.casting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.smelting.Alloy;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a casting mold recipe that defines which alloys can be used to fill
 * a specific casting mold type and what the resulting filled mold should be.
 */
@Getter
@RequiredArgsConstructor
public class CastingMoldRecipe {
    
    private final @NotNull CastingMold baseMold;
    private final int requiredMillibuckets;
    private final @NotNull Map<Alloy, BaseItem> acceptedAlloys;
    
    /**
     * Creates a new casting mold recipe.
     * @param baseMold The base casting mold that can be filled
     * @param requiredMillibuckets The amount of liquid alloy required to fill this mold
     */
    public CastingMoldRecipe(@NotNull CastingMold baseMold, int requiredMillibuckets) {
        this.baseMold = baseMold;
        this.requiredMillibuckets = requiredMillibuckets;
        this.acceptedAlloys = new HashMap<>();
    }
    
    /**
     * Adds an accepted alloy and its resulting filled mold.
     * @param alloy The alloy type that can be used to fill this mold
     * @param result The filled casting mold that results from using this alloy
     * @return This recipe instance for method chaining
     */
    public CastingMoldRecipe addAcceptedAlloy(@NotNull Alloy alloy, @NotNull BaseItem result) {
        acceptedAlloys.put(alloy, result);
        return this;
    }
    
    /**
     * Checks if the given alloy can be used to fill this mold.
     * @param alloy The alloy to check
     * @return true if the alloy is accepted, false otherwise
     */
    public boolean acceptsAlloy(@NotNull Alloy alloy) {
        return acceptedAlloys.containsKey(alloy);
    }
    
    /**
     * Gets the result item for the given alloy.
     * @param alloy The alloy type
     * @return The filled casting mold result, or empty if the alloy is not accepted
     */
    public @NotNull Optional<BaseItem> getResult(@NotNull Alloy alloy) {
        return Optional.ofNullable(acceptedAlloys.get(alloy));
    }
    
    /**
     * Checks if this recipe can be used with the given mold.
     * @param mold The mold to check
     * @return true if the mold matches this recipe's base mold
     */
    public boolean matches(@NotNull BaseItem mold) {
        return baseMold.equals(mold);
    }
} 