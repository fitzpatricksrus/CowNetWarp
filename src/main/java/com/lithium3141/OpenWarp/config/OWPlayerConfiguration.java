package com.lithium3141.OpenWarp.config;

import com.lithium3141.OpenWarp.OpenWarp;
import com.lithium3141.OpenWarp.Warp;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Configuration for a single player. Encapsulates all on-disk info
 * for this player, including warps, quotas, etc.
 *
 * @author lithium3141
 */
public class OWPlayerConfiguration {

    // Configuration filenames
    /**
     * File containing warp listings and information for the player.
     */
    public static final String WARP_CONFIG_FILENAME = "warps.yml";

    /**
     * The OpenWarp instance backing this player configuration.
     */
    private OpenWarp plugin;

    /**
     * The player name for whom this object holds configuration data.
     */
    private String playerName;

    /**
     * The directory holding configuration data for this object.
     */
    private File configFolder;

    /**
     * The Configuration object containing player-specific warp info.
     */
    private YamlConfiguration warpConfig;

    /**
     * Construct a new player configuration for the given player name.
     *
     * @param ow   The OpenWarp instance handling this player configuration
     * @param name The player to handle configuration for
     */
    public OWPlayerConfiguration(OpenWarp ow, String name) {
        this.plugin = ow;
        this.playerName = name;
    }

    /**
     * Construct a new player configuration for the given player.
     *
     * @param ow     The OpenWarp instance handling this player configuration
     * @param player The player to handle configuration for
     */
    public OWPlayerConfiguration(OpenWarp ow, Player player) {
        this.plugin = ow;
        this.playerName = player.getName();
    }

    /**
     * Get the name of the player for which this class handles configuration.
     *
     * @return the player name for this configuration.
     */
    public String getPlayerName() {
        return this.playerName;
    }

    /**
     * Load this player configuration from disk.
     */
    public void load() throws IOException, InvalidConfigurationException {
        // Locate configs
        this.configFolder = new File(this.plugin.getDataFolder(), this.playerName);
        this.configFolder.mkdirs();

        // Build config objects from files
        this.warpConfig = new YamlConfiguration();

        // Load configs
        this.warpConfig.load(getWarpConfigFile());

        // Warps
        if (this.plugin.getPrivateWarps().get(this.playerName) == null) {
            this.plugin.getPrivateWarps().put(this.playerName, new HashMap<String, Warp>());
        }
        this.plugin.getConfigurationManager().loadWarps(this.warpConfig, this.plugin.getPrivateWarps().get(this.playerName));
    }

    /**
     * Save this player configuration to disk.
     *
     * @return true if this player configuration was saved successfully
     *         or skipped; false on error.
     */
    public boolean save() {
        // Warps
        Map<String, Warp> playerWarps = this.plugin.getPrivateWarps(this.playerName);

        Map<String, Object> configWarps = new HashMap<String, Object>();
        for (Entry<String, Warp> entry : playerWarps.entrySet()) {
            configWarps.put(entry.getKey(), entry.getValue().getConfigurationMap());
        }
        this.warpConfig.set(OWConfigurationManager.WARPS_LIST_KEY, configWarps);

        try {
            this.warpConfig.save(getWarpConfigFile());
            return true;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return false;
        }
    }

    private File getWarpConfigFile() {
        return new File(this.configFolder, WARP_CONFIG_FILENAME);
    }
}
