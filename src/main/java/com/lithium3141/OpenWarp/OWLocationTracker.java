package com.lithium3141.OpenWarp;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class responsible for tracking player location changes. Used in part to
 * implement the <code>/back</code> and various warp stack functions.
 */
public class OWLocationTracker {
    /**
     * Previous locations for each player. Maps a player's name to a single Location.
     */
    private Map<String, Location> previousLocations = new HashMap<String, Location>();

    /**
     * Create a new location tracker for the given OpenWarp instance.
     */
    public OWLocationTracker() {
    }

    /**
     * Get the last recorded location of the player with the given name. This location
     * is updated whenever the player teleports or dies.
     *
     * @param playerName The name of the player for whom to get the last known location.
     * @return The Location of the given player immediately before their most recent
     *         teleport or death, or <code>null</code> if no such location is known.
     */
    public Location getPreviousLocation(String playerName) {
        Location loc = this.previousLocations.get(playerName);
        if (loc != null) {
            return loc;
        } else {
            return null;
        }
    }

    /**
     * Set the last recorded location of the given Player. Calls
     * #setPreviousLocation(String, Location) internally.
     *
     * @param player   The Player for whom to update the location.
     * @param location The Location to record as "previous location."
     */
    public void setPreviousLocation(Player player, Location location) {
        setPreviousLocation(player.getName(), location);
    }

    /**
     * Set the last recorded location of the given player.
     *
     * @param playerName The player for whom to update the location.
     * @param location   The Location to record as "previous location."
     */
    public void setPreviousLocation(String playerName, Location location) {
        this.previousLocations.put(playerName, location);
    }

    /**
     * Remove the last known location for the given player.
     *
     * @param playerName The player for whom to clear the last known location.
     */
    public void clearPreviousLocation(String playerName) {
        this.previousLocations.remove(playerName);
    }
}
