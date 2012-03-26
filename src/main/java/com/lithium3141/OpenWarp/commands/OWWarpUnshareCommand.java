package com.lithium3141.OpenWarp.commands;

import com.lithium3141.OpenWarp.OWCommand;
import com.lithium3141.OpenWarp.Warp;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Stop sharing a private warp with another player.
 */
public class OWWarpUnshareCommand extends OWCommand {

    /**
     * Create a new instance of the warp unshare command. Used in command registration.
     *
     * @param plugin The plugin (generally an instance of OpenWarp) backing this command.
     */
    public OWWarpUnshareCommand(JavaPlugin plugin) {
        super(plugin);

        this.setName("Warp unshare");
        this.setArgRange(2, 2);
        this.setCommandUsage("/warp unshare {warp} {player}");
        this.addCommandExample("/warp unshare MyWarp OtherPerson");
        this.setPermission("openwarp.warp.unshare", "Stop sharing a private warp with other users", PermissionDefault.TRUE);
        this.addKey("warp unshare");
    }

    @Override
    public void runCommand(CommandSender sender, List<String> args) {
        if (!this.checkPlayerSender(sender)) return; // SUPPRESS CHECKSTYLE NeedBracesCheck
        Player player = (Player) sender;

        String warpName = args.get(0);
        String sharePlayerName = args.get(1);

        Warp warp = this.getPlugin().getPrivateWarps().get(player.getName()).get(warpName);
        if (warp == null) {
            sender.sendMessage(ChatColor.RED + "Could not find private warp '" + warpName + "' - not sharing.");
            return;
        }

        warp.removeInvitee(sharePlayerName);

        Player sharePlayer = this.getPlugin().getServer().getPlayer(sharePlayerName);
        if (sharePlayer != null) {
            sharePlayer.sendMessage(ChatColor.GOLD + player.getName() + " has stopped sharing warp '" + warpName + "' with you.");
        }
    }

}
