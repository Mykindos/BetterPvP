package me.mykindos.betterpvp.core.item.purity.bias;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a purity-based bias configuration for item reforging.
 * Uses beta distribution parameters (alpha, beta) to generate biased random values
 * that favor either minimum or maximum stat rolls.
 * <p>
 * Beta distribution properties:
 * <ul>
 *      <li>Low alpha, high beta → favors minimum (distribution skewed toward 0)</li>
 *      <li>High alpha, low beta → favors maximum (distribution skewed toward 1)</li>
 *      <li>Equal alpha and beta → uniform distribution (no bias)</li>
 * </ul>
 */
@Getter
public class PurityReforgeBias {

    private final ItemPurity purity;
    private final double alpha;
    private final double beta;

    /**
     * Creates a new PurityReforgeBias with beta distribution parameters.
     *
     * @param purity The purity level this bias applies to
     * @param alpha  The alpha (α) parameter of the beta distribution (must be &gt; 0)
     * @param beta   The beta (β) parameter of the beta distribution (must be &gt; 0)
     * @throws IllegalArgumentException if alpha or beta is not positive
     */
    public PurityReforgeBias(@NotNull ItemPurity purity, double alpha, double beta) {
        this.purity = Objects.requireNonNull(purity, "Purity cannot be null");
        if (alpha <= 0 || beta <= 0) {
            throw new IllegalArgumentException(
                    "Beta distribution parameters must be positive (alpha=" + alpha + ", beta=" + beta + ")"
            );
        }
        this.alpha = alpha;
        this.beta = beta;
    }

    /**
     * Generates a biased random value between 0.0 and 1.0 using beta distribution.
     * This value represents where in the [min, max] range the stat should fall.
     * <p>
     * Return value interpretation:
     * - 0.0 = minimum stat value
     * - 0.5 = middle of range
     * - 1.0 = maximum stat value
     * <p>
     * The distribution is shaped by alpha and beta parameters:
     * - PITIFUL (α=0.3, β=3.0): mean ≈ 0.09 (9% toward max, 91% toward min)
     * - PERFECT (α=2.5, β=0.5): mean ≈ 0.83 (83% toward max, 17% toward min)
     *
     * @return A random value between 0.0 and 1.0 following beta(alpha, beta) distribution
     */
    public double generateBiasedRatio() {
        return sampleBetaDistribution(alpha, beta);
    }

    /**
     * Samples from a beta distribution using the relationship with gamma distribution.
     * <p>
     * Mathematical relationship: If X ~ Gamma(α, 1) and Y ~ Gamma(β, 1),
     * then X/(X+Y) ~ Beta(α, β)
     *
     * @param alpha The alpha parameter
     * @param beta  The beta parameter
     * @return A random value from Beta(alpha, beta)
     */
    private double sampleBetaDistribution(double alpha, double beta) {
        double x = sampleGammaDistribution(alpha, 1.0);
        double y = sampleGammaDistribution(beta, 1.0);
        return x / (x + y);
    }

    /**
     * Samples from a gamma distribution using Marsaglia and Tsang's method.
     * <p>
     * Reference: "A Simple Method for Generating Gamma Variables" (2000)
     * by George Marsaglia and Wai Wan Tsang
     * <p>
     * This is an efficient rejection sampling algorithm that works for shape ≥ 1.
     * For shape &lt; 1, we use the transformation property:
     * Gamma(α, θ) = Gamma(α+1, θ) × U^(1/α) where U ~ Uniform(0,1)
     *
     * @param shape The shape parameter (α, must be &gt; 0)
     * @param scale The scale parameter (θ, must be &gt; 0)
     * @return A random value from Gamma(shape, scale)
     */
    private double sampleGammaDistribution(double shape, double scale) {
        if (shape < 1.0) {
            // For shape < 1, use shape + 1 and then transform
            double result = sampleGammaDistribution(shape + 1.0, scale);
            return result * Math.pow(Math.random(), 1.0 / shape);
        }

        // Marsaglia and Tsang's method for shape >= 1
        double d = shape - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);

        while (true) {
            double x, v;
            do {
                x = randomGaussian();
                v = 1.0 + c * x;
            } while (v <= 0.0);

            v = v * v * v;
            x = x * x;

            double u = Math.random();

            // Quick acceptance check
            if (u < 1.0 - 0.0331 * x * x) {
                return d * v * scale;
            }

            // Precise acceptance check
            if (Math.log(u) < 0.5 * x + d * (1.0 - v + Math.log(v))) {
                return d * v * scale;
            }
        }
    }

    /**
     * Generates a random value from standard normal distribution (mean=0, variance=1)
     * using the Box-Muller transform.
     * <p>
     * Box-Muller Transform:
     * If U1, U2 ~ Uniform(0,1), then
     * Z = √(-2 ln U1) × cos(2π U2) ~ Normal(0, 1)
     *
     * @return A random value from N(0, 1)
     */
    private double randomGaussian() {
        double u1 = Math.random();
        double u2 = Math.random();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurityReforgeBias that = (PurityReforgeBias) o;
        return Double.compare(that.alpha, alpha) == 0
                && Double.compare(that.beta, beta) == 0
                && purity == that.purity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(purity, alpha, beta);
    }

    @Override
    public String toString() {
        return "PurityReforgeBias{" +
                "purity=" + purity +
                ", alpha=" + alpha +
                ", beta=" + beta +
                ", mean=" + String.format("%.2f", alpha / (alpha + beta)) +
                '}';
    }
}
