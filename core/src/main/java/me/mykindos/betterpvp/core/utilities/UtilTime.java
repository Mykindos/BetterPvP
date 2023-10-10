package me.mykindos.betterpvp.core.utilities;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

@Slf4j
public class UtilTime {

    /**
     * Check if a specified amount of time has elapsed from a certain point
     *
     * @param from     Time you are checking from
     * @param required Amount of time elapsed
     * @return Returns true if the amount of time has elapsed since the defined point
     */
    public static boolean elapsed(long from, long required) {
        return System.currentTimeMillis() - from > required;
    }

    public static double trim(double untrimmed, int d) {
        // Create a NumberFormat instance for the default locale
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

        if (numberFormat instanceof DecimalFormat) {
            // If the number format is a DecimalFormat, set the desired number of decimal places
            numberFormat.setMaximumFractionDigits(d);
        }

        // Format the untrimmed value using NumberFormat
        String formattedValue = numberFormat.format(untrimmed);

        try {
            // Parse the formatted value back to a double
            return numberFormat.parse(formattedValue).doubleValue();
        } catch (Exception e) {
            log.warn("Failed to parse value: " + formattedValue, e);
        }

        // Handle parsing errors gracefully
        return untrimmed;
    }

    public static double convert(double d, TimeUnit unit, int decPoint) {
        if (unit == TimeUnit.BEST) {
            if (d < 60000L) {
                unit = TimeUnit.SECONDS;
            } else if (d < 3600000L) {
                unit = TimeUnit.MINUTES;
            } else if (d < 86400000L) {
                unit = TimeUnit.HOURS;
            } else if (d < 31536000000L) {
                unit = TimeUnit.DAYS;
            } else {
                unit = TimeUnit.YEARS;
            }
        }
        if (unit == TimeUnit.SECONDS) {
            return trim(d / 1000.0D, decPoint);
        }
        if (unit == TimeUnit.MINUTES) {
            return trim(d / 60000.0D, decPoint);
        }
        if (unit == TimeUnit.HOURS) {
            return trim(d / 3600000.0D, decPoint);
        }
        if (unit == TimeUnit.DAYS) {
            return trim(d / 86400000.0D, decPoint);
        }
        if (unit == TimeUnit.YEARS) {
            return trim(d / 31536000000.0D, decPoint);
        }
        return trim(d, decPoint);
    }

    public static String getTimeUnit2(double d) {
        if (d < 60000L) {
            return "Seconds";
        } else if (d >= 60000L && d <= 3600000L) {
            return "Minutes";
        } else if (d >= 3600000L && d < 86400000L) {
            return "Hours";
        } else if (d >= 86400000L && d < 31536000000L) {
            return "Days";
        }
        return "Years";
    }


    public static String getTimeUnit(String unit) {
        return getTimeUnit(TimeUnit.getByShortVersion(unit));

    }

    public static String getTimeUnit(TimeUnit unit) {
        return switch (unit) {
            case SECONDS -> "Seconds";
            case MINUTES -> "Minutes";
            case HOURS -> "Hours";
            case DAYS -> "Days";
            case YEARS -> "Years";
            default -> "";
        };

    }

    public static String getTime(double d, TimeUnit unit, int decPoint) {
        return UtilTime.convert(d, TimeUnit.BEST, decPoint) + " "
                + UtilTime.getTimeUnit2(d);
    }

    public static String getTime2(double d, TimeUnit unit, int decPoint) {
        return UtilTime.convert(d, unit, decPoint) + " "
                + UtilTime.getTimeUnit(unit);
    }

    public enum TimeUnit {

        BEST("b"),
        YEARS("y"),
        DAYS("d"),
        HOURS("h"),
        MINUTES("m"),
        SECONDS("s");

        @Getter
        private final String shortVersion;

        TimeUnit(String shortVersion) {
            this.shortVersion = shortVersion;
        }

        public static TimeUnit getByShortVersion(String shortVersion) {
            return Arrays.stream(values()).filter(u -> u.shortVersion.equalsIgnoreCase(shortVersion)).findFirst().orElse(null);
        }
    }
}
