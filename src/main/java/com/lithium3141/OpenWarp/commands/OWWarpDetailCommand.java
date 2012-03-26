package com.lithium3141.OpenWarp.commands;

import com.lithium3141.OpenWarp.OWCommand;
import com.lithium3141.OpenWarp.Warp;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Prints information about a warp visible to a player.
 */
public class OWWarpDetailCommand extends OWCommand {

    /**
     * Create a new instance of the warp detail command. Used in command registration.
     *
     * @param plugin The plugin (generally an instance of OpenWarp) backing this command.
     */
    public OWWarpDetailCommand(JavaPlugin plugin) {
        super(plugin);

        this.setName("Warp detail");
        this.setArgRange(1, 1);
        this.setCommandUsage("/warp detail {NAME}");
        this.addCommandExample("/warp detail community");
        this.setPermission("openwarp.warp.detail", "Show warp information", PermissionDefault.TRUE);
        this.addKey("warp detail");
    }

    @Override
    public void runCommand(CommandSender sender, List<String> args) {
        if (args.size() == 0) {
            sender.sendMessage(ChatColor.YELLOW + this.getCommandUsage());
            return;
        }

        String warpName = args.get(0);

        Warp warp = this.getPlugin().getWarp(sender, warpName);
        if (warp == null) {
            sender.sendMessage(ChatColor.RED + warpName + ":" + ChatColor.WHITE + " No such warp");
        } else {
            ChatColor color = (warp.isPublic() ? ChatColor.GREEN : ChatColor.BLUE); // SUPPRESS CHECKSTYLE AvoidInlineConditionalsCheck
            sender.sendMessage(color + warpName + ":" + ChatColor.WHITE + " " + warp.getDetailString());
        }
    }

}
