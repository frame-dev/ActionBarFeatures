package ch.framedev.actionBarFeatures;

/*
 * ch.framedev.actionBarFeatures
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 07.01.2025 12:36
 */

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getServer;

public class VaultManager {

    private Economy economy;
    private final Plugin plugin;

    public VaultManager(Plugin plugin) {
        this.plugin = plugin;
        if (!setupEconomy()) {
            plugin.getLogger().severe("Vault dependency not found or Economy provider is missing!");
        } else {
            plugin.getLogger().info("Vault successfully hooked into Economy!");
        }
    }

    /**
     * Setup Vault's Economy integration
     *
     * @return true if successful, false otherwise
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault plugin not found on the server.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No registered Economy provider found in Vault.");
            return false;
        }

        economy = rsp.getProvider();
        return true;
    }

    /**
     * Get the Economy instance
     *
     * @return Economy instance if available, null otherwise
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Check if the Economy is properly set up
     *
     * @return true if the Economy is available, false otherwise
     */
    public boolean isEconomyAvailable() {
        return economy != null;
    }
}
