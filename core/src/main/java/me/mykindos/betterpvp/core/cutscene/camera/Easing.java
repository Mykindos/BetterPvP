package me.mykindos.betterpvp.core.cutscene.camera;

/**
 * How a camera crosses the gap between two shots.
 * <p>
 * The curve is applied to normalised progress, so a beat's travel time stays exactly what the author asked for and only
 * the distribution of movement across it changes. Every curve satisfies {@code apply(0) == 0} and {@code apply(1) == 1},
 * which is what lets a shot be re-timed without also re-checking that it still lands.
 */
public enum Easing {

    /** No travel at all: the camera is at the destination on the first tick of the beat. */
    CUT {
        @Override
        public double apply(double progress) {
            return 1;
        }
    },

    /** Constant speed. Reads as mechanical, which is occasionally the point. */
    LINEAR {
        @Override
        public double apply(double progress) {
            return progress;
        }
    },

    /** Slow to fast — the camera pulls away from where it was. */
    EASE_IN {
        @Override
        public double apply(double progress) {
            return progress * progress * progress;
        }
    },

    /** Fast to slow — the camera settles onto where it is going. */
    EASE_OUT {
        @Override
        public double apply(double progress) {
            final double inverse = 1 - progress;
            return 1 - inverse * inverse * inverse;
        }
    },

    /** Slow at both ends. The default for anything that has to look deliberate. */
    EASE_IN_OUT {
        @Override
        public double apply(double progress) {
            if (progress < 0.5) {
                return 4 * progress * progress * progress;
            }
            final double inverse = -2 * progress + 2;
            return 1 - inverse * inverse * inverse / 2;
        }
    };

    /**
     * @param progress how far through the travel, {@code 0..1}
     * @return how far along the path the camera should be, {@code 0..1}
     */
    public abstract double apply(double progress);
}
