package com.lithium3141.OpenWarp.util;

import com.lithium3141.OpenWarp.OpenWarp;
import com.lithium3141.OpenWarp.Warp;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.regex.Pattern;

/**
 * Multiverse 2 destination type for OpenWarp. Allows users of both Multiverse
 * and OpenWarp to use Multiverse commands to move to OpenWarp warps.
 */
public class OpenWarpDestination {

    /**
     * The OpenWarp instance backing this destination.
     */
    private OpenWarp plugin;

    /**
     * The Warp represented by this destination.
     */
    private Warp warp;

    /**
     * The name of the Warp represented by this destination.
     */
    private String warpName;

    /**
     * The identifier used by Multiverse's destination labeling system for OpenWarp warps.
     */
    public static final String IDENTIFIER = "ow";

    public String getIdentifier() {
        return IDENTIFIER;
    }

    public boolean isThisType(JavaPlugin owningPlugin, String dest) {
        return Pattern.matches("^ow:([a-zA-Z0-9_]+)(?::([a-zA-Z0-9_]+))?$", dest);
    }

    public Location getLocation(Entity e) {
        if (e instanceof CommandSender) {
            Warp w = this.plugin.getWarp((CommandSender) e, this.warpName);
            if (w != null) {
                return w.getLocation();
            }
        }
        return null;
    }

    public boolean isValid() {
        return this.plugin.getWarp(null, this.warpName) != null;
    }

    public void setDestination(JavaPlugin owningPlugin, String dest) {
        // If this class exists, then this plugin MUST exist!
        this.plugin = (OpenWarp) plugin.getServer().getPluginManager().getPlugin("OpenWarp");
        try {
            this.warpName = dest.substring(dest.indexOf(":") + 1);
        } catch (IndexOutOfBoundsException e) {
            this.warpName = "";
        }
    }

    public String getType() {
        Warp w = this.plugin.getWarp(null, this.warpName);
        if (w != null) {
            if (w.isPublic()) {
                return "Public OpenWarp";
            }
            return "Private OpenWarp";
        }
        return "Invalid OpenWarp";
    }

    public String getName() {
        Warp w = this.plugin.getWarp(null, this.warpName);
        if (w != null) {
            return w.getName();
        }
        return "Invalid OpenWarp";
    }

    public String getRequiredPermission() {
        Warp w = this.plugin.getWarp(null, this.warpName);
        String permString = "";
        if (w != null) {
            if (w.isPublic()) {
                permString = "openwarp.warp.access.public." + w.getName();
            } else {
                permString = "openwarp.warp.access.private." + w.getOwner() + "." + w.getName();
            }
        }
        return permString;
    }

    public Vector getVelocity() {
        return new Vector();
    }

    /**
     * Check whether to use Multiverse's safe teleporter.
     *
     * @return True.
     */
    public boolean useSafeTeleporter() {
        Warp w = this.plugin.getWarp(null, this.warpName);
        if (w != null) {
            return warp.useSafeTeleporter();
        }
        return true;
    }

}
