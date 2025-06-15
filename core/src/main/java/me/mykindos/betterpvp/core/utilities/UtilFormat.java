package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.text.WordUtils;
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
     * Determines whether two given strings are similar based on their cosine similarity.
     *
     * @param one the first input string
     * @param two the second input string
     * @return true if the cosine similarity of the two strings is greater than 0.6, false otherwise
     */
    public static boolean isSimilar(final String one, final String two) {
        final Map<CharSequence, Integer> query =
                Arrays.stream(one.split("")).collect(Collectors.toMap(c -> c, c -> 1, Integer::sum));
        final Map<CharSequence, Integer> online =
                Arrays.stream(two.split("")).collect(Collectors.toMap(c -> c, c -> 1, Integer::sum));
        return new CosineSimilarity().cosineSimilarity(query, online) > 0.6;
    }

    /**
     * Formats a given integer into a string with commas as thousand separators.
     *
     * @param num the integer number to be formatted
     * @return a string representing the formatted number with thousand separators
     */
    public static String formatNumber(int num) {
        return String.format("%,d", num);
    }

    /**
     * Formats a number to a string representation with a default of two decimal places.
     *
     * @param num the number to be formatted
     * @return the formatted number as a string with two decimal places
     */
    public static String formatNumber(double num) {
        return formatNumber(num, 2);
    }

    /**
     * Formats a number to the specified number of decimal places.
     * Utilizes a default configuration where trailing zeroes are not retained.
     *
     * @param num the number to format
     * @param decimalPlaces the number of decimal places to include in the formatted output
     * @return the formatted number as a string
     */
    public static String formatNumber(double num, int decimalPlaces) {
        return formatNumber(num, decimalPlaces, false);
    }

    /**
     * Formats a number to a specified number of decimal places, optionally including or excluding trailing zeroes.
     *
     * @param num the number to format
     * @param decimalPlaces the number of decimal places to include; must be non-negative
     * @param trailingZeroes whether to include trailing zeroes in the formatted result
     * @return the formatted number as a string
     * @throws IllegalArgumentException if the specified decimalPlaces is negative
     */
    public static String formatNumber(double num, int decimalPlaces, boolean trailingZeroes) {
        Preconditions.checkArgument(decimalPlaces >= 0, "decimalPlaces cannot be negative");

        // Convert the rounded number to a string with specified decimal places
        @SuppressWarnings("MalformedFormatString") String formattedNumber = String.format("%." + decimalPlaces + "f", num);

        // Remove trailing zeros if forceDecimals is false
        if (!trailingZeroes) {
            formattedNumber = formattedNumber.replaceAll("\\.?0*$", "");
        }

        return formattedNumber;
    }

    /**
     * Cleans and formats the provided string by replacing underscores with spaces
     * and capitalizing each word fully.
     *
     * @param string the input string to be cleaned and formatted
     * @return the cleaned and fully capitalized string with underscores replaced by spaces
     */
    public static String cleanString(String string) {
        String modified = string.replace("_", " ");
        return WordUtils.capitalizeFully(modified).replace("_", " ");
    }

    /**
     * Determines the online status of a player by their UUID in string format.
     * Returns a color-coded string based on whether the player is online or offline.
     *
     * @param uuid The string representation of the player's UUID.
     * @return A string indicating the player's online status.
     *         Returns "<red>" if the player is offline, and "<green>" if the player is online.
     */
    public static String getOnlineStatus(String uuid) {
        return getOnlineStatus(UUID.fromString(uuid));
    }
    /**
     * Determines the online status of a player based on their UUID.
     *
     * @param uuid the UUID of the player whose online status is to be checked
     * @return a string indicating the player's online status; returns "<red>" if the player is offline
     *         and "<green>" if the player is online
     */
    public static String getOnlineStatus(UUID uuid) {
        return Bukkit.getPlayer(uuid) == null ? "<red>" : "<green>";
    }

    /**
     * Modifies the given name by inserting a zero-width non-joiner (U+200C)
     * after the first character in the string.
     *
     * @param name the original name to be modified
     * @return the modified name with a zero-width non-joiner inserted,
     *         or may throw an exception if the input is null or empty
     */
    public static String spoofNameForLunar(String name) {
        return name.charAt(0) + "\u200C" + name.substring(1);
    }

    /**
     * Removes color codes from the provided string. Color codes are typically used
     * for formatting purposes and will be stripped out by this method.
     *
     * @param input the string from which to remove color codes, or null
     * @return the input string without color codes, or null if the input is null
     */
    @Contract("!null -> !null; null -> null")
    @Nullable
    public static String stripColor(@Nullable final String input) {
        if (input == null) {
            return null;
        }

        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Converts the given integer number to its Roman numeral representation.
     *
     * @param number the integer number to be converted to a Roman numeral.
     *               Must be a positive integer (1-10) for standard Roman numeral conversion.
     * @return a String representing the Roman numeral equivalent of the input number if within the range 1-10.
     *         Returns the string representation of the input number if it is outside this range.
     */
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

    /**
     * Generates a SHA-256 hash of the given host address concatenated with the provided salt.
     *
     * @param hostAddress the input string representing the host address
     * @param salt the salt string to be combined with the host address for hashing
     * @return the generated SHA-256 hash as a hexadecimal string
     */
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

    /**
     * Determines the appropriate indefinite article ("a" or "an") for the given word.
     * The choice is based on whether the word starts with a vowel or consonant sound.
     *
     * @param word the word for which the indefinite article is determined; must not be null or empty
     * @return "an" if the word starts with a vowel sound (a, e, i, o, u); "a" otherwise; empty string if the input is null or empty
     */
    public static String getIndefiniteArticle(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }

        char firstChar = Character.toLowerCase(word.charAt(0));
        if (firstChar == 'a' || firstChar == 'e' || firstChar == 'i' || firstChar == 'o' || firstChar == 'u') {
            return "an";
        } else {
            return "a";
        }
    }

    /**
     * Wraps the given text to a specified width, ensuring that words are not split across lines.
     * The text is split at whitespace boundaries, and lines are wrapped to the specified width.
     */
    public static Component[] wrapText(String text, int width) {
        Preconditions.checkArgument(width > 0, "Width must be greater than 0");
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        StringBuilder wrappedText = new StringBuilder();

        for (String word : words) {
            if (line.length() + word.length() + 1 > width) {
                wrappedText.append(line.toString().trim()).append("\n");
                line.setLength(0);
            }
            line.append(word).append(" ");
        }
        if (line.length() > 0) {
            wrappedText.append(line.toString().trim());
        }

        return Arrays.stream(wrappedText.toString().split("\n"))
                .map(Component::text)
                .toArray(Component[]::new);
    }
}
