package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilFormat {

    public static final char COLOR_CHAR = '\u00A7';
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + String.valueOf(COLOR_CHAR) + "[0-9A-FK-ORX]");

    /**
     * Get whether two strings are similar to each other
     *
     * @param one The first string
     * @param two The second string
     * @return True if the strings are similar, false otherwise
     */
    public static boolean isSimilar(final String one, final String two) {
        final Map<CharSequence, Integer> query =
                Arrays.stream(one.split("")).collect(Collectors.toMap(c -> c, c -> 1, Integer::sum));
        final Map<CharSequence, Integer> online =
                Arrays.stream(two.split("")).collect(Collectors.toMap(c -> c, c -> 1, Integer::sum));
        return new CosineSimilarity().cosineSimilarity(query, online) > 0.6;
    }

    public static String formatNumber(int num) {
        return String.format("%,d", num);
    }

    public static String formatNumber(double num) {
        return formatNumber(num, 2);
    }

    public static String formatNumber(double num, int decimalPlaces) {
        return formatNumber(num, decimalPlaces, false);
    }

    public static String formatNumber(double num, int decimalPlaces, boolean trailiningZeroes) {
        Preconditions.checkArgument(decimalPlaces >= 0, "decimalPlaces cannot be negative");

        // Convert the rounded number to a string with specified decimal places
        @SuppressWarnings("MalformedFormatString") String formattedNumber = String.format("%." + decimalPlaces + "f", num);

        // Remove trailing zeros if forceDecimals is false
        if (!trailiningZeroes) {
            formattedNumber = formattedNumber.replaceAll("\\.?0*$", "");
        }

        return formattedNumber;
    }

    public static String cleanString(String string) {
        String modified = string.replace("_", " ");
        return WordUtils.capitalizeFully(modified).replace("_", " ");
    }

    public static String getOnlineStatus(String uuid) {
        return getOnlineStatus(UUID.fromString(uuid));
    }
    public static String getOnlineStatus(UUID uuid) {
        return Bukkit.getPlayer(uuid) == null ? "<red>" : "<green>";
    }

    /**
     * Since some plugins and Lunar client have an inbuilt 'ping when mentioned' feature, this was causing pings every time a player typed
     * This change prevents the ping from triggering off the players own messages, but still works when somebody else says their name
     * @param name The players name
     * @return The name with a ZWNJ character inserted
     */
    public static String spoofNameForLunar(String name) {
        return name.charAt(0) + "\u200C" + name.substring(1);
    }

    /**
     * Strips the given message of all color codes
     *
     * @param input String to strip of color
     * @return A copy of the input string, without any coloring
     */
    @Contract("!null -> !null; null -> null")
    @Nullable
    public static String stripColor(@Nullable final String input) {
        if (input == null) {
            return null;
        }

        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static String getRomanNumeral(int number) {
        if (1 <= number && number <= 3) {
            return "I".repeat(number);
        }
        if (number == 4) {
            return "IV";
        }
        if (5 <= number && number <= 8) {
            return "V" + "I".repeat(number - 5);
        }
        if (number == 9) {
            return "IX";
        }
        if (number == 10) {
            return "X";
        }
        return String.valueOf(number);
    }

    public static String hashWithSalt(String hostAddress, String salt) {
        try {
            String input = hostAddress + salt;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
