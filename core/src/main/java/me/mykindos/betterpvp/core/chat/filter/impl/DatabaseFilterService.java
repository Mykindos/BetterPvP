package me.mykindos.betterpvp.core.chat.filter.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.database.Database;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private final OkHttpClient httpClient = new OkHttpClient();

    @Inject
    public DatabaseFilterService(Core core, Database database) {
        this.database = database;
        loadFilteredWords();
        UtilServer.runTaskTimerAsync(core, this::loadFilteredWords, 20L * 60, 20L * 60 * 10);
    }

    private void loadFilteredWords() {
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
            String alternation = words.stream()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));
            combinedPattern = Pattern.compile(alternation, Pattern.CASE_INSENSITIVE);
            log.info("Successfully compiled filter pattern with {} words.", words.size()).submit();
        } else {
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
            return pattern.matcher(message).find();
        });
    }

    @Override
    public CompletableFuture<String> filterMessage(String message) {
        return CompletableFuture.supplyAsync(() -> {
            Pattern pattern = combinedPattern;
            if (pattern == null) return message;

            Matcher matcher = pattern.matcher(message);
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            while (matcher.find()) {
                sb.append(message, lastEnd, matcher.start());
                sb.append("*".repeat(matcher.end() - matcher.start()));
                lastEnd = matcher.end();
            }
            sb.append(message.substring(lastEnd));
            return sb.toString();
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
}
