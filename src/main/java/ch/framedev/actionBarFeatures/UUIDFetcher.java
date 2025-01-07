package ch.framedev.actionBarFeatures;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Modern UUIDFetcher for Mojang API
 * Fetches UUIDs and Player Names via Mojang API.
 * Compatible with Java 11+.
 */
public class UUIDFetcher {

    // ✅ Constants
    public static final long FEBRUARY_2015 = 1422748800000L;
    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s?at=%d";
    private static final String NAME_URL = "https://api.mojang.com/user/profiles/%s/names";

    // ✅ Gson Instance
    private static final Gson gson = new GsonBuilder().create();

    // ✅ Caches for Performance
    private static final Map<String, UUID> uuidCache = new ConcurrentHashMap<>();
    private static final Map<UUID, String> nameCache = new ConcurrentHashMap<>();

    // ✅ Thread Pool for Asynchronous Requests
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    // ------------------------------------------
    // ✅ ASYNCHRONOUS UUID FETCHING
    // ------------------------------------------
    public static void getUUID(String name, Consumer<UUID> action) {
        pool.execute(() -> action.accept(getUUID(name)));
    }

    public static void getUUIDAt(String name, long timestamp, Consumer<UUID> action) {
        pool.execute(() -> action.accept(getUUIDAt(name, timestamp)));
    }

    // ------------------------------------------
    // ✅ SYNCHRONOUS UUID FETCHING
    // ------------------------------------------
    public static UUID getUUID(String name) {
        return getUUIDAt(name, System.currentTimeMillis());
    }

    public static UUID getUUIDAt(String name, long timestamp) {
        name = name.toLowerCase();

        // Check Cache
        if (uuidCache.containsKey(name)) {
            return uuidCache.get(name);
        }

        try {
            URL url = URI.create(String.format(UUID_URL, name, timestamp / 1000)).toURL();
            HttpURLConnection connection = createConnection(url);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                JsonObject response = gson.fromJson(reader, JsonObject.class);

                if (response == null || !response.has("id")) {
                    throw new IllegalArgumentException("Invalid response from Mojang API");
                }

                String uuidString = response.get("id").getAsString();
                UUID uuid = formatUUID(uuidString);

                // Update Cache
                uuidCache.put(name, uuid);
                nameCache.put(uuid, name);

                return uuid;
            }
        } catch (IOException e) {
            System.err.println("Failed to fetch UUID for " + name + ": " + e.getMessage());
        }
        return null;
    }

    // ------------------------------------------
    // ✅ ASYNCHRONOUS NAME FETCHING
    // ------------------------------------------
    public static void getName(UUID uuid, Consumer<String> action) {
        pool.execute(() -> action.accept(getName(uuid)));
    }

    // ------------------------------------------
    // ✅ SYNCHRONOUS NAME FETCHING
    // ------------------------------------------
    public static String getName(UUID uuid) {
        // Check Cache
        if (nameCache.containsKey(uuid)) {
            return nameCache.get(uuid);
        }

        try {
            URL url = URI.create(String.format(NAME_URL, uuid.toString().replace("-", ""))).toURL();
            HttpURLConnection connection = createConnection(url);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                JsonArray nameHistory = gson.fromJson(reader, JsonArray.class);

                if (nameHistory == null || nameHistory.isEmpty()) {
                    throw new IllegalArgumentException("Invalid response from Mojang API");
                }

                JsonObject latestName = nameHistory.get(nameHistory.size() - 1).getAsJsonObject();
                String playerName = latestName.get("name").getAsString();

                // Update Cache
                nameCache.put(uuid, playerName);
                uuidCache.put(playerName.toLowerCase(), uuid);

                return playerName;
            }
        } catch (IOException e) {
            System.err.println("Failed to fetch Name for UUID " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    // ------------------------------------------
    // ✅ HELPER METHODS
    // ------------------------------------------

    private static HttpURLConnection createConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setReadTimeout(5000); // 5 seconds
        connection.setDoOutput(true);
        return connection;
    }

    private static UUID formatUUID(String uuidString) {
        return UUID.fromString(
                uuidString.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"
                )
        );
    }

    // ------------------------------------------
    // ✅ GRACEFUL SHUTDOWN
    // ------------------------------------------
    public static void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}