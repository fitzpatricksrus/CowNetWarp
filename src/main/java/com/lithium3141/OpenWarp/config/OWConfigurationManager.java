package com.lithium3141.OpenWarp.config;

import com.lithium3141.OpenWarp.OpenWarp;
import com.lithium3141.OpenWarp.Warp;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Container class for OpenWarp configuration information. Manages both the
 * global configuration and individual player configuration files; responsible
 * for load and save operations.
 *
 * @author lithium3141
 */
public class OWConfigurationManager {

    /**
     * The OpenWarp instance backing this configuration manager.
     */
    private OpenWarp plugin;

    /**
     * The filename to use for global plugin configuration.
     */
    public static final String MASTER_CONFIG_FILENAME = "config.yml";

    /**
     * The filename to use for public warps.
     */
    public static final String PUBLIC_WARP_CONFIG_FILENAME = "warps.yml";

    /**
     * The YAML key for lists of player names.
     */
    public static final String PLAYER_NAMES_LIST_KEY = "players";

    /**
     * The YAML key for lists of warps.
     */
    public static final String WARPS_LIST_KEY = "warps";

    /**
     * The YAML key for debug information.
     */
    public static final String DEBUG_KEY = "debug";

    /**
     * The Configuration object representing global plugin configuration.
     */
    private YamlConfiguration configuration;

    /**
     * The set of player configuration objects, mapped to by player names.
     */
    private Map<String, OWPlayerConfiguration> playerConfigs = new HashMap<String, OWPlayerConfiguration>();

    /**
     * The Configuration object representing public warp information.
     */
    private YamlConfiguration publicWarpsConfig;

    /**
     * Create a new OWConfigurationManager backed by the given OpenWarp instance.
     * Sets up data folders on-disk and loads (creating if necessary) the global
     * configuration and public warp files.
     *
     * @param ow The OpenWarp instance backing this OWConfigurationManager.
     */
    public OWConfigurationManager(OpenWarp ow) throws IOException, InvalidConfigurationException {
        this.plugin = ow;

        // Set up configuration folder if necessary
        this.plugin.getDataFolder().mkdirs();

        // Get configuration file (even if nonexistent)
        this.configuration = new YamlConfiguration();
        this.configuration.load(getMasterConfigFile());

        this.publicWarpsConfig = new YamlConfiguration();
        this.publicWarpsConfig.load(getWarpConfigFile());
    }

    private File getMasterConfigFile() {
        return new File(this.plugin.getDataFolder(), MASTER_CONFIG_FILENAME);
    }

    private File getWarpConfigFile() {
        return new File(this.plugin.getDataFolder(), PUBLIC_WARP_CONFIG_FILENAME);
    }

    /**
     * Save all configuration files currently loaded, including global
     * warp and quota configurations and configurations for each player.
     * Calls #saveGlobalConfiguration() and #savePlayerConfiguration(String)
     * internally.
     *
     * @see #saveGlobalConfiguration()
     * @see #savePlayerConfiguration(String)
     */
    public void saveAllConfigurations() {
        OpenWarp.DEBUG_LOG.fine("Writing ALL OpenWarp configuration files");
        if (this.configuration != null) {
            this.saveGlobalConfiguration();

            // Save player-specific data
            for (String playerName : this.playerConfigs.keySet()) {
                this.savePlayerConfiguration(playerName);
            }
        }
    }

    /**
     * Save global configuration data, including the files <tt>warps.yml</tt>
     * and <tt>config.yml</tt> in the primary OpenWarp directory. Writes all
     * YAML nodes into those files from current in-memory sets. Currently does
     * no checking about whether a write is necessary.
     */
    public void saveGlobalConfiguration() {
        OpenWarp.DEBUG_LOG.fine("Writing OpenWarp global configuration file");

        if (this.configuration != null) {
            // Save overall configuration
            OpenWarp.DEBUG_LOG.fine("Writing global player name list with " + this.playerConfigs.keySet().size() + " elements");
            this.configuration.set(PLAYER_NAMES_LIST_KEY, new ArrayList<String>(this.playerConfigs.keySet()));
            try {
                this.configuration.save(getMasterConfigFile());
            } catch (IOException ex) {
                OpenWarp.LOG.warning(OpenWarp.LOG_PREFIX + "Couldn't save player list; continuing...");
            }

            // Save public warps
            Map<String, Object> warps = new HashMap<String, Object>();
            for (Entry<String, Warp> entry : this.plugin.getPublicWarps().entrySet()) {
                warps.put(entry.getKey(), entry.getValue().getConfigurationMap());
            }

            this.publicWarpsConfig.set(WARPS_LIST_KEY, warps);
            try {
                this.publicWarpsConfig.save(getWarpConfigFile());
            } catch (IOException ex) {
                OpenWarp.LOG.warning(OpenWarp.LOG_PREFIX + "Couldn't save public warp list; continuing...");
            }

            // Save flags
//            this.configuration.set(MULTIWORLD_HOMES_KEY, this.configuration.getBoolean(DEBUG_KEY, false));
            this.configuration.set(DEBUG_KEY, this.configuration.getBoolean(DEBUG_KEY, false));
        }
    }

    /**
     * Save the player-specific configuration files for the given player.
     * Writes files <tt>general.yml</tt>, <tt>quota.yml</tt>, and <tt>warps.yml</tt>
     * into the OpenWarp subdirectory named for the player. Writes all YAML nodes
     * into those files from current in-memory sets. Currently does no checking about
     * whether such a write is necessary.
     *
     * @param playerName The name of the player for whom to save configuration data.
     */
    public void savePlayerConfiguration(String playerName) {
        OpenWarp.DEBUG_LOG.fine("Writing OpenWarp player configuration file (" + playerName + ")");

        if (this.configuration != null) {
            OWPlayerConfiguration config = this.playerConfigs.get(playerName);

            if (config != null && !config.save()) {
                OpenWarp.LOG.warning(OpenWarp.LOG_PREFIX + " - Couldn't save configuration for player " + config.getPlayerName() + "; continuing...");
            }
        }
    }


    /**
     * Load public warps into the given map. Mutates the `target` argument.
     *
     * @param target The map into which to load new Warp objects.
     */
    public void loadPublicWarps(Map<String, Warp> target) {
        this.loadWarps(this.publicWarpsConfig, target);
    }

    /**
     * Load warp information from the given Configuration into the given Map.
     * Mutates the `target` argument.
     *
     * @param config The Configuration from which to read warps
     * @param target The Map into which to place Warp instances
     */
    public void loadWarps(Configuration config, Map<String, Warp> target) {
        ConfigurationSection section = config.getConfigurationSection(WARPS_LIST_KEY);
        Set<String> keys = section.getKeys(false);
        if (keys != null) {
            for (String key : keys) {
                ConfigurationSection node = section.getConfigurationSection(key);
                Warp warp = new Warp(this.plugin, key, node);
                target.put(warp.getName(), warp);
            }
        }
    }

    /**
     * Register a player with the OpenWarp plugin. Create a new
     * OWPlayerConfiguration instance for the given Player if no such
     * configuration exists yet.
     *
     * @param playerName The player to register
     * @see OWPlayerConfiguration
     */
    public void registerPlayerName(String playerName) throws IOException, InvalidConfigurationException {
        if (this.playerConfigs.get(playerName) == null) {
            OWPlayerConfiguration playerConfig = new OWPlayerConfiguration(this.plugin, playerName);
            playerConfig.load();
            this.playerConfigs.put(playerName, playerConfig);
        }
    }

    /**
     * Load player information from disk, creating OWPlayerConfiguration instances
     * for each.
     */
    public void loadPlayers() throws IOException, InvalidConfigurationException {
        List<String> playerNames = this.configuration.getStringList(PLAYER_NAMES_LIST_KEY);
        for (String playerName : playerNames) {
            this.registerPlayerName(playerName);
        }
    }

    /**
     * Read the debug flag from disk.
     *
     * @return Whether this instance of OpenWarp should enable debug logging.
     */
    public boolean readDebug() {
        return this.configuration.getBoolean(DEBUG_KEY, false);
    }
}
