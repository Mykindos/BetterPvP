package me.mykindos.betterpvp.core.chat.filter.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CHAT_FILTER;

@CustomLog
@Singleton
public class DatabaseFilterService implements IFilterService {

    private static final String GAMERSAFER_URL = "https://raw.githubusercontent.com/GamerSafer/word-blocklist/main/full-word-list.csv";
    private final Database database;
    private volatile Pattern combinedPattern = null;
    private final Set<String> filteredWords = ConcurrentHashMap.newKeySet();
    private final OkHttpClient httpClient = new OkHttpClient();

    @Inject
    public DatabaseFilterService(Core core, Database database) {
        this.database = database;
        loadFilteredWords();
        UtilServer.runTaskTimerAsync(core, this::loadFilteredWords, 20L * 60, 20L * 60 * 10);
    }

    public void loadFilteredWords() {
        log.info("Loading filtered words from database...").submit();

        DSLContext dsl = database.getDslContext();
        List<String> words = dsl.select(CHAT_FILTER.WORD)
                .from(CHAT_FILTER)
                .fetch(CHAT_FILTER.WORD);

        if (words.isEmpty()) {
            log.info("Database filter table is empty, fetching from GamerSafer...").submit();
            words = fetchFromGamerSafer();
            if (!words.isEmpty()) {
                saveToDatabase(words);
            }
        }

        if (!words.isEmpty()) {
            filteredWords.clear();
            filteredWords.addAll(words);

            String alternation = words.stream()
                    .map(UtilFormat::normalize)
                    .filter(word -> word != null && !word.isEmpty())
                    .distinct()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));
            combinedPattern = Pattern.compile(alternation, Pattern.CASE_INSENSITIVE);
            log.info("Successfully compiled filter pattern with {} words.", words.size()).submit();
        } else {
            filteredWords.clear();
            combinedPattern = null;
            log.info("No filtered words loaded.").submit();
        }
    }

    private List<String> fetchFromGamerSafer() {
        Request request = new Request.Builder().url(GAMERSAFER_URL).build();
        List<String> words = new ArrayList<>();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to load GamerSafer filtered words: {}", response.code()).submit();
                return words;
            }

            String content = response.body().string();
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.isBlank()) continue;
                String word = line.split(",")[0].trim().toLowerCase();
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        } catch (IOException e) {
            log.error("Error loading GamerSafer filtered words", e).submit();
        }
        return words;
    }

    private void saveToDatabase(List<String> words) {
        log.info("Saving {} GamerSafer words to database...", words.size()).submit();
        
        long now = System.currentTimeMillis();
        
        // Execute in batches to avoid huge query issues
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            int batchSize = 1000;
            for (int i = 0; i < words.size(); i += batchSize) {
                int end = Math.min(i + batchSize, words.size());
                List<String> batch = words.subList(i, end);
                
                var batchInsert = ctx.insertInto(CHAT_FILTER,
                        CHAT_FILTER.WORD,
                        CHAT_FILTER.CREATED_AT);
                for (String word : batch) {
                    batchInsert = batchInsert.values(word, now);
                }
                batchInsert.onConflict(DSL.field("word")).doNothing().execute();
            }
            log.info("Finished saving GamerSafer words to database.").submit();
        });
    }

    @Override
    public CompletableFuture<Boolean> isFiltered(String message) {
        return CompletableFuture.supplyAsync(() -> {
            Pattern pattern = combinedPattern;
            if (pattern == null) return false;

            if (pattern.matcher(message).find()) {
                return true;
            }

            // Also check normalized message
            String normalized = UtilFormat.normalize(message);
            return pattern.matcher(normalized).find();
        });
    }

    @Override
    public CompletableFuture<Boolean> addFilteredWord(String word) {
        if (word == null || word.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        String lowercaseWord = word.toLowerCase().trim();

        return database.getAsyncDslContext().executeAsync(ctx -> {
            boolean alreadyExists = ctx.selectCount()
                    .from(CHAT_FILTER)
                    .where(CHAT_FILTER.WORD.eq(lowercaseWord))
                    .fetchOne(0, int.class) > 0;

            if (alreadyExists) {
                return false;
            }

            ctx.insertInto(CHAT_FILTER, CHAT_FILTER.WORD, CHAT_FILTER.CREATED_AT)
                    .values(lowercaseWord, System.currentTimeMillis())
                    .execute();
            
            loadFilteredWords();
            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> removeFilteredWord(String word) {
        if (word == null || word.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        String lowercaseWord = word.toLowerCase().trim();

        return database.getAsyncDslContext().executeAsync(ctx -> {
            int deleted = ctx.deleteFrom(CHAT_FILTER)
                    .where(CHAT_FILTER.WORD.eq(lowercaseWord))
                    .execute();

            if (deleted > 0) {
                loadFilteredWords();
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<String> filterMessage(String message) {
        return CompletableFuture.supplyAsync(() -> {
            Pattern pattern = combinedPattern;
            if (pattern == null) return message;

            char[] resultChars = message.toCharArray();

            // First pass: Direct regex match on original string
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                for (int i = matcher.start(); i < matcher.end(); i++) {
                    resultChars[i] = '*';
                }
            }

            // Second pass: Normalized check for bypasses
            String normalized = UtilFormat.normalize(message);
            Matcher normalizedMatcher = pattern.matcher(normalized);
            while (normalizedMatcher.find()) {
                int[] originalIndices = UtilFormat.getOriginalIndices(message, normalizedMatcher.start(), normalizedMatcher.end());
                for (int i = originalIndices[0]; i < originalIndices[1]; i++) {
                    resultChars[i] = '*';
                }
            }

            return new String(resultChars);
        });
    }

    @Override
    public CompletableFuture<Component> filterMessage(Component message) {
        return CompletableFuture.supplyAsync(() -> {
            if (message instanceof TextComponent textComponent) {
                String plainText = textComponent.content();
                String filteredText = filterMessage(plainText).join();
                if (!plainText.equals(filteredText)) {
                    return Component.text(filteredText).style(textComponent.style())
                            .children(textComponent.children());
                }
            }
            return message;
        });
    }

    @Override
    public Set<String> getFilteredWords() {
        return new HashSet<>(filteredWords);
    }
}
