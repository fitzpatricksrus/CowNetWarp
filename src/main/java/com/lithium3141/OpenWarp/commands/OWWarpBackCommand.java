package com.lithium3141.OpenWarp.commands;

import com.lithium3141.OpenWarp.OWCommand;
import com.lithium3141.OpenWarp.OWLocationTracker;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * List all warps visible to a player. Separates public from private warps.
 */
public class OWWarpBackCommand extends OWCommand {

    /**
     * Create a new instance of the warp list command. Used in command registration.
     *
     * @param plugin The plugin (generally an instance of OpenWarp) backing this command.
     */
    public OWWarpBackCommand(JavaPlugin plugin) {
        super(plugin);

        this.setName("back");
        this.setArgRange(0, 0); // SUPPRESS CHECKSTYLE MagicNumberCheck
        this.setCommandUsage("/back");
        this.addCommandExample("/back");
        this.setPermission("openwarp.warp.back", "Warp to last teleport location", PermissionDefault.TRUE);
        this.addKey("back");
        this.addKey("back", 0, 0);
    }

    @Override
    public void runCommand(CommandSender sender, List<String> args) {
        String playerName = sender.getName();

        OWLocationTracker tracker = getPlugin().getLocationTracker();
        Location loc = tracker.getPreviousLocation(playerName);
        if (loc != null) {
            ((Player) sender).teleport(loc);
            tracker.clearPreviousLocation(playerName);
        }
    }
}
