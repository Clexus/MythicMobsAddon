package cn.clexus.mythicMobsAddon.support;

import cn.clexus.mythicMobsAddon.MythicMobsAddon;
import org.bukkit.plugin.Plugin;

public class MMOItemsSupport {
    private static boolean hasSupport = false;
    public static boolean hasSupport(){
        return hasSupport;
    }
    public static void init(MythicMobsAddon plugin) {
        try{
            Plugin plg = plugin.getServer().getPluginManager().getPlugin("MMOItems");
            if (plg == null) {
                return;
            }
            hasSupport = true;
        }catch (Exception e) {
            hasSupport = false;
        }
    }
}
