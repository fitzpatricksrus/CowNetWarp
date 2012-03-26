package com.lithium3141.OpenWarp.listeners;

import com.lithium3141.OpenWarp.OpenWarp;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Entity listener for OpenWarp. Implements the <code>onEntityDeath</code>
 * method in order to update player location histories; in turn, this allows
 * OpenWarp to support players returning to death points with the <code>/back</code>
 * command.
 */
public class OWEntityListener implements Listener {

    /**
     * The OpenWarp instance backing this entity listener.
     */
    private OpenWarp plugin;

    /**
     * Create a new OWEntityListener backed by the given OpenWarp instance.
     *
     * @param ow The OpenWarp instance used for various Bukkit queries.
     */
    public OWEntityListener(OpenWarp ow) {
        this.plugin = ow;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            OpenWarp.DEBUG_LOG.fine("Player died.");
            Player player = (Player) (event.getEntity());
            this.plugin.getLocationTracker().setPreviousLocation(player, player.getLocation());
        }
    }
}
