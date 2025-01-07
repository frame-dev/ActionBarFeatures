package ch.framedev.actionBarFeatures;



/*
 * ch.framedev.essentialsmini.utils
 * =============================================
 * This File was Created by FrameDev.
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 03.01.2025 20:35
 */

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerUtils {

    @SuppressWarnings("deprecation")
    public static OfflinePlayer getOfflinePlayerByName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty!");
        }

        OfflinePlayer player = null;

        if (Bukkit.getOnlineMode()) {
            UUID uuid = UUIDFetcher.getUUID(playerName);
            if (uuid != null) {
                player = Bukkit.getOfflinePlayer(uuid);
            }
        }

        // Fallback to name-based lookup in case UUID fetch fails
        if (player == null) {
            player = Bukkit.getOfflinePlayer(playerName);
        }

        return player;
    }

    public static int getArmorLevel(Player player) {
        int armorPoints = 0;

        // Loop through each armor slot
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                switch (armorPiece.getType()) {
                    case LEATHER_HELMET:
                    case LEATHER_CHESTPLATE:
                    case LEATHER_LEGGINGS:
                    case LEATHER_BOOTS:
                        armorPoints += 1; break;

                    case GOLDEN_HELMET:
                    case GOLDEN_CHESTPLATE:
                    case GOLDEN_LEGGINGS:
                    case GOLDEN_BOOTS:
                        armorPoints += 2; break;

                    case CHAINMAIL_HELMET:
                    case CHAINMAIL_CHESTPLATE:
                    case CHAINMAIL_LEGGINGS:
                    case CHAINMAIL_BOOTS:
                        armorPoints += 2; break;

                    case IRON_HELMET:
                    case IRON_CHESTPLATE:
                    case IRON_LEGGINGS:
                    case IRON_BOOTS:
                        armorPoints += 3; break;

                    case DIAMOND_HELMET:
                    case DIAMOND_CHESTPLATE:
                    case DIAMOND_LEGGINGS:
                    case DIAMOND_BOOTS:
                        armorPoints += 4; break;

                    case NETHERITE_HELMET:
                    case NETHERITE_CHESTPLATE:
                    case NETHERITE_LEGGINGS:
                    case NETHERITE_BOOTS:
                        armorPoints += 5; break;

                    default:
                        break;
                }
            }
        }

        return armorPoints;
    }
}
