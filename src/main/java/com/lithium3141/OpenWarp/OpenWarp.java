package com.lithium3141.OpenWarp;

import com.lithium3141.OpenWarp.commands.*;
import com.lithium3141.OpenWarp.config.OWConfigurationManager;
import com.lithium3141.OpenWarp.listeners.OWEntityListener;
import com.lithium3141.OpenWarp.listeners.OWPlayerListener;
import com.lithium3141.OpenWarp.util.StringUtil;
import com.pneumaticraft.commandhandler.CommandHandler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main plugin class. Responsible for setting up plugin and handling
 * overall configs and player info.
 *
 * @author lithium3141
 */
public class OpenWarp extends JavaPlugin {

    // Logging info

    /**
     * Logger object for all Minecraft-based messages.
     */
    public static final Logger LOG = Logger.getLogger("Minecraft");

    /**
     * Prefix string for every log message output by this plugin.
     */
    public static final String LOG_PREFIX = "[OpenWarp] ";

    /**
     * Logger object for debug-level messages not sent to general Minecraft logging.
     */
    public static final Logger DEBUG_LOG = Logger.getLogger("OpenWarpDebug");

    // Global configuration variables

    /**
     * Object managing plugin configuration files and player information.
     */
    private OWConfigurationManager configurationManager;

    /**
     * Public warps tracked by this plugin. Maps warp names to their corresponding
     * Warp objects.
     */
    private Map<String, Warp> publicWarps = new HashMap<String, Warp>();

    /**
     * Private warps tracked by this plugin. Maps warp names to their corresponding
     * Warp objects for each player name.
     */
    private Map<String, Map<String, Warp>> privateWarps = new HashMap<String, Map<String, Warp>>();

    /**
     * Homes tracked by this plugin. Maps world names to their corresponding home
     * Location objects for each player name.
     */
    private Map<String, Map<String, Location>> homes = new HashMap<String, Map<String, Location>>();

    // Supported commands

    /**
     * Object managing commands and action dispatch for this plugin.
     */
    private CommandHandler commandHandler;

    // Per-player data

    /**
     * Object tracking individual player locations for history purposes.
     */
    private OWLocationTracker locationTracker;

    @Override
    public void onDisable() {
        this.configurationManager.saveAllConfigurations();

        LOG.info(LOG_PREFIX + "Disabled!");
    }

    @Override
    public void onEnable() {
        // Create overall permission
        this.getServer().getPluginManager().addPermission(new Permission("openwarp.*", PermissionDefault.OP));
        Permission wildcardPerm = this.getServer().getPluginManager().getPermission("*");
        if (wildcardPerm != null) {
            wildcardPerm.getChildren().put("openwarp.*", true);
            wildcardPerm.recalculatePermissibles();
        }

        try {
            // Load configurations
            this.configurationManager = new OWConfigurationManager(this);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Start location tracking
        this.locationTracker = new OWLocationTracker();

        // Initialize debug log
        this.setupDebugLog();

        // Read warp names
        this.configurationManager.loadPublicWarps(this.publicWarps);

        // Read player names and create configurations for each
        try {
            this.configurationManager.loadPlayers();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Set up supported commands
        this.loadCommands();

        // Instantiate permission nodes for all relevant objects
        this.loadWarpPermissions();
//        this.loadHomePermissions();

        // Start listening for events
        this.loadListeners();

        LOG.info(LOG_PREFIX + "Enabled version " + this.getDescription().getVersion());
    }

    /**
     * Initialize the debugging log for output. The debug log is used for finer-grained
     * messages that do not need to go to the general output, and is generally written
     * to a File.
     */
    private void setupDebugLog() {
        boolean useDebug = this.configurationManager.readDebug();
        Level logLevel = (useDebug ? Level.FINEST : Level.OFF); // SUPPRESS CHECKSTYLE AvoidInlineConditionalsCheck

        DEBUG_LOG.setLevel(logLevel);
        OWDebugHandler debugHandler = new OWDebugHandler(new File(this.getDataFolder(), "debug.log"));
        debugHandler.setLevel(logLevel);
        DEBUG_LOG.addHandler(debugHandler);

        DEBUG_LOG.fine("Enabled debug log at " + (new Date()).toString());
    }

    /**
     * Create warp permission nodes for all loaded warps.
     */
    public void loadWarpPermissions() {
        PluginManager pm = this.getServer().getPluginManager();

        // Finagle a new permission for public warps
        Map<String, Boolean> publicWarpChildren = new HashMap<String, Boolean>();
        for (Warp publicWarp : this.getPublicWarps().values()) {
            String permString = "openwarp.warp.access.public." + publicWarp.getName();
            Permission publicWarpPermission = new Permission(permString, PermissionDefault.TRUE);
            publicWarpChildren.put(permString, true);
            pm.addPermission(publicWarpPermission);
        }
        Permission warpAccessPublicPerm = new Permission("openwarp.warp.access.public.*", PermissionDefault.TRUE, publicWarpChildren);
        pm.addPermission(warpAccessPublicPerm);

        // The same, for private warps
        Map<String, Boolean> privateWarpChildren = new HashMap<String, Boolean>();
        for (String playerName : this.getPrivateWarps().keySet()) {
            String permPrefix = "openwarp.warp.access.private." + playerName;
            privateWarpChildren.put(permPrefix + ".*", true);

            Map<String, Boolean> privateWarpSubchildren = new HashMap<String, Boolean>();
            for (Warp privateWarp : this.getPrivateWarps(playerName).values()) {
                String permString = permPrefix + "." + privateWarp.getName();
                Permission privateWarpPermission = new Permission(permString, PermissionDefault.TRUE);
                privateWarpSubchildren.put(permString, true);
                pm.addPermission(privateWarpPermission);
            }
            Permission warpAccessPrivateSubperm = new Permission(permPrefix + ".*", privateWarpSubchildren);
            pm.addPermission(warpAccessPrivateSubperm);
        }
        Permission warpAccessPrivatePerm = new Permission("openwarp.warp.access.private.*", PermissionDefault.TRUE, privateWarpChildren);
        pm.addPermission(warpAccessPrivatePerm);

        // Put the actual access perms in
        Map<String, Boolean> accessChildren = new HashMap<String, Boolean>() {
            {
                put("openwarp.warp.access.public.*", true);
                put("openwarp.warp.access.private.*", true);
            }
        };
        Permission warpAccessPerm = new Permission("openwarp.warp.access.*", PermissionDefault.TRUE, accessChildren);
        pm.addPermission(warpAccessPerm);

        // Also insert delete perms
        Map<String, Boolean> deletePublicChildren = new HashMap<String, Boolean>() {
            {
                put("openwarp.warp.delete.public.self", true);
                put("openwarp.warp.delete.public.other", true);
            }
        };
        Permission deletePublicPerm = new Permission("openwarp.warp.delete.public.*", PermissionDefault.TRUE, deletePublicChildren);
        pm.addPermission(deletePublicPerm);

        // Add public & private children of delete perm (which should already exist)
        Permission deletePerm = pm.getPermission("openwarp.warp.delete.*");
        if (deletePerm != null) {
            deletePerm.getChildren().put("openwarp.warp.delete.public.*", true);
            deletePerm.getChildren().put("openwarp.warp.delete.private.*", true);
            deletePerm.recalculatePermissibles();
        }

        // Make the primary access & delete perms a child of overall warp perms
        Permission warpPerm = pm.getPermission("openwarp.warp.*");
        if (warpPerm != null) {
            warpPerm.getChildren().put("openwarp.warp.access.*", true);
            warpPerm.getChildren().put("openwarp.warp.delete.*", true);
            warpPerm.recalculatePermissibles();
        } else {
            LOG.severe(LOG_PREFIX + "Error inserting warp access permissions. This is a bug!");
        }
    }

    /**
     * Initialize individual commands to be used by users of this plugin. An instance
     * of each command object must be created and registered with this plugin's
     * CommandHandler before it will have messages dispatched to it.
     */
    private void loadCommands() {
        this.commandHandler.registerCommand(new OWWarpCommand(this));
        this.commandHandler.registerCommand(new OWWarpListCommand(this));
        this.commandHandler.registerCommand(new OWWarpDetailCommand(this));
        this.commandHandler.registerCommand(new OWWarpSetCommand(this));
        this.commandHandler.registerCommand(new OWWarpDeleteCommand(this));
        this.commandHandler.registerCommand(new OWWarpShareCommand(this));
        this.commandHandler.registerCommand(new OWWarpUnshareCommand(this));
        this.commandHandler.registerCommand(new OWWarpBackCommand(this));
    }

    /**
     * Initialize listeners for in-game actions. An instance of each listener object
     * must be created and registered with the Bukkit server before it will respond
     * to events.
     */
    private void loadListeners() {
        OWPlayerListener playerListener = new OWPlayerListener(this);
        this.getServer().getPluginManager().registerEvents(playerListener, this);
        this.getServer().getPluginManager().registerEvents(playerListener, this);
        this.getServer().getPluginManager().registerEvents(playerListener, this);

        OWEntityListener entityListener = new OWEntityListener(this);
        this.getServer().getPluginManager().registerEvents(entityListener, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        DEBUG_LOG.fine("Command received. Name:" + command.getName() + " label:" + command.getLabel() + " arglabel:" + commandLabel);

        // Construct a trie key path from the command label and args
        List<String> keyPath = new ArrayList<String>();
        keyPath.add(command.getLabel().toLowerCase());
        for (int i = 0; i < args.length; i++) {
            keyPath.add(args[i]);
        }

        // Locate and run the best matching command from the key path
        return this.commandHandler.locateAndRunCommand(sender, keyPath);
    }

    /**
     * Get all public warps known to this plugin.
     *
     * @return A map of warp names to their corresponding Warp objects.
     */
    public Map<String, Warp> getPublicWarps() {
        return this.publicWarps;
    }

    /**
     * Get all private warps known to this plugin.
     *
     * @return A map of player names to a map of warp names to their corresponding Warp objects.
     */
    public Map<String, Map<String, Warp>> getPrivateWarps() {
        return this.privateWarps;
    }

    /**
     * Get private warps for a particular player.
     *
     * @param playerName The name of the player for which to get public warps.
     * @return A map of warp names to their corresponding Warp objects, or null if the given
     *         player is not known to this plugin.
     */
    public Map<String, Warp> getPrivateWarps(String playerName) {
        return this.getPrivateWarps().get(playerName);
    }

    /**
     * Get the location tracker for this plugin.
     *
     * @return The OWLocationTracker instance watching players for this plugin.
     */
    public OWLocationTracker getLocationTracker() {
        return this.locationTracker;
    }

    /**
     * Get the configuration handler for this plugin.
     *
     * @return The OWConfigurationManager instance handling on-disk configuration info for this plugin.
     */
    public OWConfigurationManager getConfigurationManager() {
        return this.configurationManager;
    }

    /**
     * Get the Warp, if any, matching the given name for the given sender.
     *
     * @param sender   The sender for whom to check for warps
     * @param warpName The name of the warp to find
     * @return In order of precedence: (1) the public warp with the given
     *         name, (2) the private warp belonging to the given sender,
     *         (3) the shared warp belonging to the given owner with the
     *         specified name, or (4) null.
     */
    public Warp getWarp(CommandSender sender, String warpName) {
        if (sender instanceof Player) {
            DEBUG_LOG.finer(((Player) sender).getName() + " requests warp '" + warpName + "'");
        }

        // First check public warps
        for (Entry<String, Warp> entry : this.getPublicWarps().entrySet()) {
            String name = entry.getKey();
            if (name.equalsIgnoreCase(warpName)) {
                return entry.getValue();
            }
        }

        // If no match, check private warps
        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (Entry<String, Warp> entry : this.getPrivateWarps().get(player.getName()).entrySet()) {
                String name = entry.getKey();
                if (name.equalsIgnoreCase(warpName)) {
                    return entry.getValue();
                }
            }
        }

        // If still no match, check shared warps
        if (warpName.contains(":") && (sender instanceof Player)) {
            String requester = ((Player) sender).getName();
            String[] parts = warpName.split(":");
            String recipient = parts[0];
            warpName = StringUtil.arrayJoin(Arrays.copyOfRange(parts, 1, parts.length), ":");
            DEBUG_LOG.finest("Checking shared warps; want player '" + recipient + "' and warp '" + warpName + "'");

            for (Entry<String, Map<String, Warp>> mapEntry : this.getPrivateWarps().entrySet()) {
                if (mapEntry.getKey().equalsIgnoreCase(recipient)) {
                    for (Entry<String, Warp> entry : mapEntry.getValue().entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(warpName)) {
                            Warp warp = entry.getValue();
                            if (warp.isInvited(requester)) {
                                return warp;
                            }
                        }
                    }
                }
            }
        }

        // If still no match, try to cast to coords
        if (sender instanceof Player) {
            Pattern coordPattern = Pattern.compile("(?:([a-zA-Z0-9_]+):)?(-?[0-9]+),(-?[0-9]+),(-?[0-9]+)"); // it burns us
            Matcher coordMatcher = coordPattern.matcher(warpName);
            if (coordMatcher.matches()) {
                String worldName = coordMatcher.group(1);
                int x = Integer.parseInt(coordMatcher.group(2));
                int y = Integer.parseInt(coordMatcher.group(3)); // SUPPRESS CHECKSTYLE MagicNumberCheck
                int z = Integer.parseInt(coordMatcher.group(4)); // SUPPRESS CHECKSTYLE MagicNumberCheck

                World world;
                if (worldName == null) {
                    world = ((Player) sender).getWorld();
                } else {
                    world = this.getServer().getWorld(worldName);
                }
                System.out.println("DEBUG: warping exact to world " + world.getName());
                if (world != null) {
                    return new Warp(this, "_EXACT", new Location(world, (double) x, (double) y, (double) z), ((Player) sender).getName());
                }
            }

        }

        // No match
        return null;
    }

    /**
     * Get the Warp, if any, matching the given Location for the given sender.
     *
     * @param sender   The sender for whom to check for warps
     * @param location The location of the warp to find
     * @return The matching warp, if found
     * @see #getWarp(CommandSender, String)
     */
    public Warp getWarp(CommandSender sender, Location location) {
        // First check public warps
        for (Entry<String, Warp> entry : this.getPublicWarps().entrySet()) {
            Location warpLoc = entry.getValue().getLocation();
            if (location.equals(warpLoc)) {
                return entry.getValue();
            }
        }

        // If no match, check private warps
        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (Entry<String, Warp> entry : this.getPrivateWarps().get(player.getName()).entrySet()) {
                Location warpLoc = entry.getValue().getLocation();
                if (location.equals(warpLoc)) {
                    return entry.getValue();
                }
            }
        }

        // If still no match, check shared warps
        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (Entry<String, Map<String, Warp>> mapEntry : this.getPrivateWarps().entrySet()) {
                String recipient = mapEntry.getKey();
                if (recipient.equals(player.getName())) {
                    continue;
                }
                for (Entry<String, Warp> entry : this.getPrivateWarps().get(recipient).entrySet()) {
                    Warp warp = entry.getValue();
                    if (location.equals(warp.getLocation()) && warp.isInvited(player)) {
                        return warp;
                    }
                }
            }
        }

        // No match
        return null;
    }

    public boolean hasPermission(CommandSender sender, String node, boolean isOpRequired) {
        if (!(sender instanceof Player)) {
            return true;
        } else {
            Player player = (Player) sender;
            if (player.hasPermission(node)) {
                return true;
            } else if (isOpRequired) {
                return player.isOp();
            } else {
                return false;
            }
        }
    }

    public boolean hasAnyPermission(CommandSender sender, List<String> allPermissionStrings, boolean opRequired) {
        for (String node : allPermissionStrings) {
            if (this.hasPermission(sender, node, opRequired)) {
                return true;
            }
        }
        return false;
    }
}
