package ch.framedev.actionBarFeatures;

import ch.framedev.SpigotUtils;
import ch.framedev.yamlutils.FileConfiguration;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getScheduler;

public final class ActionBarFeatures extends JavaPlugin implements CommandExecutor, TabCompleter {

    private BukkitTask task;

    private final Set<OfflinePlayer> coordinatePlayers = new HashSet<>();
    private final Set<OfflinePlayer> biomePlayers = new HashSet<>();
    private final Set<OfflinePlayer> entitiesPlayers = new HashSet<>();
    private final Set<OfflinePlayer> dayTimePlayers = new HashSet<>();
    private final Set<OfflinePlayer> weatherPlayers = new HashSet<>();
    private final Set<OfflinePlayer> lightLevelPlayers = new HashSet<>();
    private final Set<OfflinePlayer> playedHoursPlayers = new HashSet<>();
    private final Set<OfflinePlayer> armorLevelPlayers = new HashSet<>();
    private final Set<OfflinePlayer> balancePlayers = new HashSet<>();

    private String colorForValue;
    private String spacer;

    private FileConfiguration config;
    private FileConfiguration playerSaveConfig;

    private VaultManager vaultManager;

    private static ActionBarFeatures instance;

    @Override
    public void onEnable() {
        instance = this;

        this.config = new FileConfiguration(getTempConfigFile(), new File(getDataFolder(), "config.yml"));

        this.config.set("version", getDescription().getVersion());
        this.config.save();

        this.playerSaveConfig = new FileConfiguration(new File(getDataFolder(), "playerSaveConfig.yml"));
        if (new File(getDataFolder(), "playerSaveConfig.yml").exists())
            this.playerSaveConfig.load();

        getScheduler().runTaskLater(this, this::manageScheduler, 60);

        Objects.requireNonNull(getCommand("actionbarfeatures")).setExecutor(this);
        Objects.requireNonNull(getCommand("actionbarfeatures")).setTabCompleter(this);

        this.colorForValue = config.getString("colorForValue");
        if (colorForValue == null)
            colorForValue = "§a";
        colorForValue = colorForValue.replace("&", "§");
        this.spacer = config.getString("spacers");
        if (spacer == null)
            spacer = "§4|";
        spacer = spacer.replace("&", "§");

        if (config.getBoolean("saveLoadData"))
            loadData();

        getScheduler().runTaskLater(this, () -> {
            vaultManager = new VaultManager(this);
        }, 60);

        getLogger().info("ActionBarFeatures has been enabled!");
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (config.getBoolean("saveLoadData"))
            saveData();

        // Clear all player sets
        this.coordinatePlayers.clear();
        this.biomePlayers.clear();
        this.entitiesPlayers.clear();
        this.dayTimePlayers.clear();
        this.weatherPlayers.clear();
        this.lightLevelPlayers.clear();
        this.playedHoursPlayers.clear();
        this.armorLevelPlayers.clear();
        this.balancePlayers.clear();

        getLogger().info("ActionBarFeatures has been disabled!");
    }

    public void saveData() {
        Map<String, Set<OfflinePlayer>> playerGroups = Map.of(
                "coordinates", coordinatePlayers,
                "biome", biomePlayers,
                "entities", entitiesPlayers,
                "dayTime", dayTimePlayers,
                "weather", weatherPlayers,
                "lightLevel", lightLevelPlayers,
                "playedHours", playedHoursPlayers,
                "armorLevel", armorLevelPlayers,
                "balance", balancePlayers
        );

        playerGroups.forEach((key, players) -> {
            List<String> playerNames = players.stream()
                    .map(OfflinePlayer::getName)
                    .collect(Collectors.toList());
            playerSaveConfig.set(key, playerNames);
        });

        playerSaveConfig.save();
    }

    public void loadData() {
        Map<String, Set<OfflinePlayer>> playerGroups = Map.of(
                "coordinates", coordinatePlayers,
                "biome", biomePlayers,
                "entities", entitiesPlayers,
                "dayTime", dayTimePlayers,
                "weather", weatherPlayers,
                "lightLevel", lightLevelPlayers,
                "playedHours", playedHoursPlayers,
                "armorLevel", armorLevelPlayers,
                "balance", balancePlayers
        );

        playerGroups.forEach((key, players) -> {
            List<String> playerNames = playerSaveConfig.containsKey(key)
                    ? playerSaveConfig.getStringList(key)
                    : new ArrayList<>();

            playerNames.forEach(playerName -> players.add(PlayerUtils.getOfflinePlayerByName(playerName)));
        });
    }

    public File getTempConfigFile() {
        try (InputStream inputStream = getResource("config.yml")) {
            if (inputStream == null) {
                getLogger().severe("Could not find config.yml in the plugin resources!");
                return null;
            }

            // Create a temporary file
            File tempFile = File.createTempFile("config_", ".yml");
            tempFile.deleteOnExit(); // Ensure file is deleted on JVM exit

            // Copy contents from the resource to the temp file
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile;
        } catch (IOException e) {
            getLogger().severe("Failed to create a temporary config file: " + e.getMessage());
            return null;
        }
    }

    public static ActionBarFeatures getInstance() {
        return instance;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("actionbarfeatures")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(getPrefix() + "§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("actionbarfeatures.use")) {
            player.sendMessage(getPrefix() + "§cYou do not have permission to use this command.");
            return true;
        }

        // Handle single argument commands
        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "showcoordinates" -> toggleCoordinateDisplay(player);
                case "showbiome" -> toggleBiomeDisplay(player);
                case "showentities" -> toggleEntitiesDisplay(player);
                case "showdaytime" -> toggleDayTimeDisplay(player);
                case "showweather" -> toggleWeatherDisplay(player);
                case "showlightlevel" -> toggleLightLevelDisplay(player);
                case "showplayedhours" -> togglePlayedHoursDisplay(player);
                case "showarmorlevel" -> toggleArmorLevelDisplay(player);
                case "showbalance" -> toggleBalanceDisplay(player);
                case "info" -> showInfo(player);
                case "all" -> {
                    toggleCoordinateDisplay(player);
                    toggleBiomeDisplay(player);
                    toggleEntitiesDisplay(player);
                    toggleDayTimeDisplay(player);
                    toggleWeatherDisplay(player);
                    toggleLightLevelDisplay(player);
                    togglePlayedHoursDisplay(player);
                }
                case "default" -> handleDefaultConfig(player);
                default -> player.sendMessage(getPrefix() + "§cUsage: /actionbarfeatures " +
                        "<showcoordinates|showbiome|showentities|showdaytime|showweather|showlightlevel|showplayedhours|showarmorlevel|showbalance" +
                        "|info|all|default>");
            }
            return true;
        }

        // Handle two-argument commands
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "daytime" -> handleDayTimeConfig(player, args[1]);
                case "sleep" -> handleBooleanConfig(player, "displaySleep", args[1], "Sleeping");
                case "spaces" -> handleBooleanConfig(player, "spaces", args[1], "Spaces");
                default -> player.sendMessage(getPrefix() + "§cUsage: /actionbarfeatures <daytime|sleep|spaces>");
            }
            return true;
        }

        // Default usage message
        displayCommands(player);
        return true;
    }

    private void displayCommands(Player player) {
        player.sendMessage(getPrefix() + "§cUsage: /actionbarfeatures <command>");
        player.sendMessage(getPrefix() + "§7Available Commands:");
        player.sendMessage(getPrefix() + "§e- showcoordinates§7: Displays the player's current coordinates.");
        player.sendMessage(getPrefix() + "§e- showbiome§7: Displays the biome the player is currently in.");
        player.sendMessage(getPrefix() + "§e- showentities§7: Displays the number of entities in the player's world.");
        player.sendMessage(getPrefix() + "§e- showdaytime§7: Displays the time of day in the player's world.");
        player.sendMessage(getPrefix() + "§e- showweather§7: Displays the current weather in the player's world.");
        player.sendMessage(getPrefix() + "§e- showlightlevel§7: Displays the light level of the block the player is standing on.");
        player.sendMessage(getPrefix() + "§e- showplayedhours§7: Displays the Played Hours.");
        player.sendMessage(getPrefix() + "§e- showarmorlevel§7: Display the Armor Level of the Player");
        player.sendMessage(getPrefix() + "§e- showbalance§7: Displays the player's balance.");
        player.sendMessage(getPrefix() + "§e- info§7: Displays plugin information.");
        player.sendMessage(getPrefix() + "§e- all§7: Toggles all features on or off.");
        player.sendMessage(getPrefix() + "§e- daytime§7: Set the display of the day time to text or number.");
        player.sendMessage(getPrefix() + "§e- default§7: Toggles all default features on or off.");
        player.sendMessage(getPrefix() + "§e- spaces§7: Enable or disable spaces between the features.");
    }

    // Handle 'default' subcommand
    private void handleDefaultConfig(Player player) {
        Map<String, Runnable> actions = Map.of(
                "coordinates", () -> toggleCoordinateDisplay(player),
                "biome", () -> toggleBiomeDisplay(player),
                "entities", () -> toggleEntitiesDisplay(player),
                "daytime", () -> toggleDayTimeDisplay(player),
                "weather", () -> toggleWeatherDisplay(player),
                "lightlevel", () -> toggleLightLevelDisplay(player),
                "playedhours", () -> togglePlayedHoursDisplay(player),
                "armorlevel", () -> toggleArmorLevelDisplay(player),
                "balance", () -> toggleBalanceDisplay(player)
        );

        getDefaults().forEach(value -> {
            Runnable action = actions.get(value.toLowerCase());
            if (action != null) action.run();
        });
    }

    // Handle 'daytime' subcommand
    private void handleDayTimeConfig(Player player, String value) {
        if (value.equalsIgnoreCase("text") || value.equalsIgnoreCase("number")) {
            config.set("dayTime", value);
            config.save();
            player.sendMessage(getPrefix() + "§aDay Time display is now set to §6" + value + "§a.");
        } else {
            player.sendMessage(getPrefix() + "§cUsage: /actionbarfeatures daytime <text|number>");
        }
        player.sendMessage(getPrefix() + "§cPlease Reload the Server!");
        config.load();
    }

    // Handle 'sleep' and 'spaces' boolean configuration
    private void handleBooleanConfig(Player player, String configKey, String value, String featureName) {
        switch (value.toLowerCase()) {
            case "enable" -> {
                config.set(configKey, true);
                config.save();
                player.sendMessage(getPrefix() + "§a" + featureName + " is now §6enabled§a.");
            }
            case "disable" -> {
                config.set(configKey, false);
                config.save();
                player.sendMessage(getPrefix() + "§a" + featureName + " is now §cdisabled.");
            }
            default ->
                    player.sendMessage(getPrefix() + "§cUsage: /actionbarfeatures " + configKey + " <enable|disable>");
        }
        player.sendMessage(getPrefix() + "§cPlease Reload the Server!");
        config.load();
    }

    public List<String> getDefaults() {
        return getConfig().getStringList("default");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("actionbarfeatures")) {
            return null; // Not our command, let others handle it
        }

        List<String> suggestions = getSuggestions(args);
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "daytime" -> suggestions.addAll(List.of("text", "number"));
                case "sleep", "spaces" -> suggestions.addAll(List.of("enable", "disable"));
            }
        }

        // Return filtered suggestions based on current input
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }

    private static @NotNull List<String> getSuggestions(String[] args) {
        List<String> suggestions = new ArrayList<>();

        // First argument suggestions
        if (args.length == 1) {
            suggestions.add("showcoordinates");
            suggestions.add("showbiome");
            suggestions.add("showentities");
            suggestions.add("showdaytime");
            suggestions.add("showweather");
            suggestions.add("showlightlevel");
            suggestions.add("showplayedhours");
            suggestions.add("showarmorlevel");
            suggestions.add("showbalance");
            suggestions.add("all");
            suggestions.add("default");
            suggestions.add("info");
            suggestions.add("daytime");
            suggestions.add("sleep");
            suggestions.add("spaces");
        }
        return suggestions;
    }

    private void showInfo(Player player) {
        String prefix = getPrefixAndSetup(player);

        // Command Descriptions
        Map<String, String> commands = Map.ofEntries(
                Map.entry("showcoordinates", "Displays the player's current coordinates."),
                Map.entry("showbiome", "Displays the biome the player is currently in."),
                Map.entry("showentities", "Displays the number of entities in the player's world."),
                Map.entry("showdaytime", "Displays the time of day in the player's world."),
                Map.entry("showweather", "Displays the current weather in the player's world."),
                Map.entry("showlightlevel", "Displays the light level of the block the player is standing on."),
                Map.entry("showplayedhours", "Displays the Played Hours."),
                Map.entry("showarmorlevel", "Displays the armor Level of the Player."),
                Map.entry("showbalance", "Displays the current balance of the Player."),
                Map.entry("all", "Toggles all features on or off."),
                Map.entry("daytime", "Set the display of the day time to text or number."),
                Map.entry("sleep", "Enable or disable to Display can sleep shown as (true or false)."),
                Map.entry("default", "Toggles all default features on or off."),
                Map.entry("spaces", "Enable or disable spaces between the features.")
        );

        commands.forEach((cmd, desc) ->
                player.sendMessage(prefix + "§a- " + cmd + ": " + desc)
        );

        // Feature Counts
        Map<String, Integer> featureCounts = Map.of(
                "(C) Coordinates", coordinatePlayers.size(),
                "(B) Biomes", biomePlayers.size(),
                "(E) Entities", entitiesPlayers.size(),
                "(T) Day Time", dayTimePlayers.size(),
                "(W) Weather", weatherPlayers.size(),
                "(L) Light Level", lightLevelPlayers.size(),
                "(PH) Played Hours", playedHoursPlayers.size(),
                "(AL) Armor Level", armorLevelPlayers.size(),
                "(B) Balance", balancePlayers.size()
        );

        featureCounts.forEach((label, count) ->
                player.sendMessage(prefix + "§a- " + label + ": §b" + count)
        );
    }

    private @NotNull String getPrefixAndSetup(Player player) {
        String prefix = getPrefix();

        // General Information
        List<String> generalInfo = Arrays.asList(
                "§aActionBarFeatures is a plugin that displays various information in the action bar.",
                "§aUsage: /actionbarfeatures <showcoordinates|showbiome|showentities|showdaytime|showweather|showlightlevel|showplayedhours|showarmorlevel|showbalance|all|daytime|sleep|default|spaces>"
        );

        generalInfo.forEach(info -> player.sendMessage(prefix + info));
        return prefix;
    }

    private void toggleCoordinateDisplay(Player player) {
        if (coordinatePlayers.contains(player)) {
            coordinatePlayers.remove(player);
            player.sendMessage(getPrefix() + "§aCoordinate display is now §cdisabled §ain the action bar.");
        } else {
            coordinatePlayers.add(player);
            player.sendMessage(getPrefix() + "§aCoordinate display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void toggleBiomeDisplay(Player player) {
        if (biomePlayers.contains(player)) {
            biomePlayers.remove(player);
            player.sendMessage(getPrefix() + "§aBiome display is now §cdisabled §ain the action bar.");
        } else {
            biomePlayers.add(player);
            player.sendMessage(getPrefix() + "§aBiome display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void toggleEntitiesDisplay(Player player) {
        if (entitiesPlayers.contains(player)) {
            entitiesPlayers.remove(player);
            player.sendMessage(getPrefix() + "§aEntity count display is now §cdisabled §ain the action bar.");
        } else {
            entitiesPlayers.add(player);
            player.sendMessage(getPrefix() + "§aEntity count display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void toggleDayTimeDisplay(Player player) {
        if (dayTimePlayers.contains(player)) {
            dayTimePlayers.remove(player);
            player.sendMessage(getPrefix() + "§aDayTime display is now §cdisabled §ain the action bar.");
        } else {
            dayTimePlayers.add(player);
            player.sendMessage(getPrefix() + "§aDayTime display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void toggleWeatherDisplay(Player player) {
        if (weatherPlayers.contains(player)) {
            weatherPlayers.remove(player);
            player.sendMessage(getPrefix() + "§aWeather display is now §cdisabled §ain the action bar.");
        } else {
            weatherPlayers.add(player);
            player.sendMessage(getPrefix() + "§aWeather display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void toggleLightLevelDisplay(Player player) {
        if (lightLevelPlayers.contains(player)) {
            lightLevelPlayers.remove(player);
            player.sendMessage(getPrefix() + "§aLightLevel display is now §cdisabled §ain the action bar.");
        } else {
            lightLevelPlayers.add(player);
            player.sendMessage(getPrefix() + "§aLightLevel display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void togglePlayedHoursDisplay(Player player) {
        if (playedHoursPlayers.contains(player)) {
            playedHoursPlayers.remove(player);
            player.sendMessage(getPrefix() + "§aPlayed Hour's display is now §cdisabled §ain the action bar.");
        } else {
            playedHoursPlayers.add(player);
            player.sendMessage(getPrefix() + "§aPlayed Hour's display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void toggleArmorLevelDisplay(Player player) {
        if (armorLevelPlayers.contains(player)) {
            armorLevelPlayers.remove(player);
            player.sendMessage(getPrefix() + "§aArmorLevel display is now §cdisabled §ain the action bar.");
        } else {
            armorLevelPlayers.add(player);
            player.sendMessage(getPrefix() + "§aArmorLevel display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void toggleBalanceDisplay(Player player) {
        if (balancePlayers.contains(player)) {
            balancePlayers.remove(player);
            player.sendMessage(getPrefix() + "§aBalance display is now §cdisabled §ain the action bar.");
        } else {
            balancePlayers.add(player);
            player.sendMessage(getPrefix() + "§aBalance display is now §6enabled §ain the action bar.");
        }
        manageScheduler();
    }

    private void manageScheduler() {
        if (coordinatePlayers.isEmpty() && biomePlayers.isEmpty() && entitiesPlayers.isEmpty() && dayTimePlayers.isEmpty() && weatherPlayers.isEmpty()
                && lightLevelPlayers.isEmpty() && playedHoursPlayers.isEmpty() && armorLevelPlayers.isEmpty() && balancePlayers.isEmpty()) {
            if (task != null) {
                task.cancel();
                task = null;
            }
        } else if (task == null) {
            startScheduler();
        }
    }

    private void startScheduler() {
        task = getScheduler().runTaskTimer(this, () -> getActionBarMessage(getConfig().getBoolean("spaces")), 0L, 20L);
    }

    private String getKey(String key) {
        String defaultString = "§b%Key%§e";
        String keyString = config.getString("keyColor");
        if (keyString == null)
            keyString = defaultString;
        keyString = keyString.replace("&", "§");
        keyString = keyString.replace("%Key%", key);
        return keyString;
    }

    private void getActionBarMessage(boolean spaces) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!player.isOnline()) return;

            StringBuilder actionBarMessage = new StringBuilder();
            AtomicInteger activeConditions = new AtomicInteger();

            String defaultText = config.getString("defaultText");
            if (defaultText == null)
                defaultText = "§6[§e(§a%s§e)§a%s§6]";
            defaultText = defaultText.replace("&", "§");

            // Helper function to append sections dynamically
            String finalDefaultText = defaultText;
            BiConsumer<String, String> appendSection = (label, value) -> {
                if (activeConditions.getAndIncrement() > 0)
                    actionBarMessage.append(spaces ? " " + spacer + " " : spacer);
                actionBarMessage.append(String.format(finalDefaultText, label, value));
            };

            // Coordinates
            if (coordinatePlayers.contains(player)) {
                String coordinateText = config.getString("coordinateFormat");
                if (coordinateText == null)
                    coordinateText = "§6X:%s§6Y:%s§6Z:%s";
                coordinateText = coordinateText.replace("&", "§");
                coordinateText = spaces ? " " + coordinateText : coordinateText;
                Location loc = player.getLocation();
                appendSection.accept(getKey("C"), String.format(coordinateText,
                        spaces ? colorForValue + " %.0f " : colorForValue + "%.0f",
                        spaces ? colorForValue + " %.0f " : colorForValue + "%.0f",
                        spaces ? colorForValue + " %.0f" : colorForValue + "%.0f"
                ).formatted(loc.getX(), loc.getY(), loc.getZ()));
            }

            // Biome
            if (biomePlayers.contains(player)) {
                String biome = toCamelUppercase(player.getLocation().getBlock().getBiome().getKey().getKey());
                appendSection.accept(getKey("B"), spaces ? colorForValue + " " + biome : colorForValue + biome);
            }

            // Entities
            if (entitiesPlayers.contains(player)) {
                int entityCount = player.getWorld().getEntities().size();
                appendSection.accept(getKey("E"), spaces ? colorForValue + " " + String.valueOf(entityCount) : colorForValue + String.valueOf(entityCount));
            }

            // Daytime
            if (dayTimePlayers.contains(player)) {
                String time = Objects.requireNonNull(getConfig().getString("dayTime")).equalsIgnoreCase("text")
                        ? getTimeName(player.getWorld().getTime())
                        : String.valueOf(player.getWorld().getTime());

                if (getConfig().getBoolean("displaySleep")) {
                    boolean canSleep = canPlayerSleep(player);
                    appendSection.accept(getKey("T"), String.format("%s%s%s",
                            spaces ? " " + colorForValue : colorForValue,
                            time,
                            spaces ? " §6/" + colorForValue + canSleep : "§6/" + colorForValue + canSleep));
                } else {
                    appendSection.accept(getKey("T"), spaces ? " " + colorForValue + time : colorForValue + time);
                }
            }

            if (weatherPlayers.contains(player)) {
                String weather = player.getWorld().isThundering() ? "Thunderstorm" : player.getWorld().hasStorm() ? "Rain" : "Sun";
                appendSection.accept(getKey("W"), spaces ? " " + colorForValue + weather : colorForValue + weather);
            }

            if (lightLevelPlayers.contains(player)) {
                int lightLevel = player.getLocation().getBlock().getLightLevel();
                appendSection.accept(getKey("L"), spaces ? " §a" + lightLevel : "§a" + lightLevel);
            }

            if (playedHoursPlayers.contains(player)) {
                double playedHours = new SpigotUtils().calculateHours(player);
                String formattedNumbers = new BigDecimal(playedHours).setScale(3, RoundingMode.HALF_UP).toString();
                appendSection.accept(getKey("PH"), spaces ? " " + colorForValue + formattedNumbers + "H" : colorForValue + formattedNumbers);
            }

            if (armorLevelPlayers.contains(player)) {
                int armorPoints = PlayerUtils.getArmorLevel(player);
                appendSection.accept(getKey("AL"), spaces ? " " + colorForValue + armorPoints : colorForValue + armorPoints);
            }

            if (balancePlayers.contains(player)) {
                if (vaultManager != null && vaultManager.isEconomyAvailable()) {
                    double balance = vaultManager.getEconomy().getBalance(player);
                    appendSection.accept(getKey("B"), spaces ? " " + colorForValue + balance + vaultManager.getEconomy().currencyNamePlural()
                            : colorForValue + balance + vaultManager.getEconomy().currencyNamePlural());
                } else {
                    appendSection.accept(getKey("B"), spaces? " " + colorForValue + "Vault not found" : colorForValue + "Vault not found");
                }
            }

            // Send the action bar message if not empty
            if (!actionBarMessage.isEmpty()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent(actionBarMessage.toString())
                );
            }
        });
    }

    private String toCamelUppercase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Split by spaces, underscores, or hyphens
        String[] words = input.split("[\\s_\\-]+");
        StringBuilder camelCase = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                // Capitalize the first letter and append the rest in lowercase
                camelCase.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase());
            }
        }

        return camelCase.toString();
    }

    private String getTimeName(long time) {
        time = time % 24000; // Ensure time stays in the 0-23999 range

        if (time >= 0 && time < 6000) {
            return "Morning";
        } else if (time >= 6000 && time < 12000) {
            return "Noon";
        } else if (time >= 12000 && time < 18000) {
            return "Evening";
        } else if (time >= 18000) {
            return "Night";
        } else {
            return "Unknown Time";
        }
    }

    public String getPrefix() {
        return "§8[§6§lAction§bBar§aFeatures§r§8] §6»§r ";
    }

    private boolean canPlayerSleep(Player player) {
        long time = player.getWorld().getTime(); // Get world time in ticks

        // Check if it's night or during a thunderstorm
        boolean isNight = (time >= 12800 && time < 23000); // Nighttime sleeping hours
        boolean isThunderstorm = player.getWorld().isThundering();
        return isNight || isThunderstorm;
    }
}