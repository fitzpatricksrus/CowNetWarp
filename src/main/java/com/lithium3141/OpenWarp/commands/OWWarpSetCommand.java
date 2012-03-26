package com.lithium3141.OpenWarp.commands;

import com.lithium3141.OpenWarp.OWCommand;
import com.lithium3141.OpenWarp.OpenWarp;
import com.lithium3141.OpenWarp.Warp;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Create a new warp or move an existing warp. Places the warp at
 * the player's current location. Private warp names may overlap
 * between different players. This command will fail if the setting
 * player is at their warp quota for the specified warp type.
 */
public class OWWarpSetCommand extends OWCommand {

    /**
     * Create a new instance of the warp set command. Used in command registration.
     *
     * @param plugin The plugin (generally an instance of OpenWarp) backing this command.
     */
    public OWWarpSetCommand(JavaPlugin plugin) {
        super(plugin);

        this.setName("Warp set");
        this.setArgRange(1, 2);
        this.setCommandUsage("/warp set {NAME} [public|private]");
        this.addCommandExample("/warp set community public");
        this.setPermission("openwarp.warp.set", "Create a new warp", PermissionDefault.OP);
        this.addKey("warp set");
        this.addKey("setwarp");
    }

    @Override
    public void runCommand(CommandSender sender, List<String> args) {
        if (!this.checkPlayerSender(sender)) return; // SUPPRESS CHECKSTYLE NeedBracesCheck

        // Grab player info
        Player player = (Player) sender;
        Location playerLoc = player.getLocation();

        // Find warp type
        String warpType;
        if (args.size() >= 2) {
            warpType = args.get(1);
        } else {
            warpType = "private";
        }

        if (!warpType.equals("public") && !warpType.equals("private")) {
            player.sendMessage(ChatColor.YELLOW + this.getCommandUsage());
            return;
        }

        // See if warp exists already - this affects quota checking
        OpenWarp.DEBUG_LOG.fine("Checking for warp '" + args.get(0) + "' in " + warpType + " warps for player " + player.getName());
        boolean warpExists = false;
        if (warpType.equals("public")) {
            if (this.getPlugin().getPublicWarps().get(args.get(0)) != null) {
                warpExists = true;
            }
        } else if (warpType.equals("private")) {
            if (this.getPlugin().getPrivateWarps().get(player.getName()).get(args.get(0)) != null) {
                warpExists = true;
            }
        }
        OpenWarp.DEBUG_LOG.fine("warp exists: " + warpExists);

        //hey jf - you might want to check quota here

        // Create and set warp
        Warp warp = new Warp(this.getPlugin(), args.get(0), playerLoc, player.getName());
        String successMsg = (warpExists ? "Moved" : "Created new"); // SUPPRESS CHECKSTYLE AvoidInlineConditionalsCheck
        if (warpType.equals("public")) {
            this.getPlugin().getPublicWarps().put(warp.getName(), warp);
            player.sendMessage(ChatColor.AQUA + "Success: " + ChatColor.WHITE + successMsg + " public warp '" + warp.getName() + "'");
            this.getPlugin().getConfigurationManager().saveGlobalConfiguration();
        } else if (warpType.equals("private")) {
            this.getPlugin().getPrivateWarps().get(player.getName()).put(warp.getName(), warp);
            player.sendMessage(ChatColor.AQUA + "Success: " + ChatColor.WHITE + successMsg + " private warp '" + warp.getName() + "'");
            this.getPlugin().getConfigurationManager().savePlayerConfiguration(player.getName());
        }

        // Create permission for warp
        String permString = "";
        if (warpType.equals("public")) {
            permString = "openwarp.warp.access.public." + warp.getName();
        } else if (warpType.equals("private")) {
            permString = "openwarp.warp.access.private." + warp.getOwner() + "." + warp.getName();
        }
        Permission accessPerm = new Permission(permString, PermissionDefault.TRUE);
        PluginManager pm = this.getPlugin().getServer().getPluginManager();
        if (pm.getPermission(permString) == null) {
            pm.addPermission(accessPerm);
            Permission parentPerm = pm.getPermission("openwarp.warp.access." + warpType + ".*");
            parentPerm.getChildren().put(permString, true);
            accessPerm.recalculatePermissibles();
            parentPerm.recalculatePermissibles();
            for (Player p : this.getPlugin().getServer().getOnlinePlayers()) {
                p.recalculatePermissions();
            }
        }
    }

}
