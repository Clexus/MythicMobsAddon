package cn.clexus.mythicMobsAddon.support;

import cn.clexus.mythicMobsAddon.MythicMobsAddon;
import org.bukkit.plugin.Plugin;

public class CrucibleSupport {
    private static boolean hasSupport = false;
    public static boolean hasSupport(){
        return hasSupport;
    }
    public static void init(MythicMobsAddon plugin) {
        try{
            Plugin Crucible = plugin.getServer().getPluginManager().getPlugin("MythicCrucible");
            if (Crucible == null) {
                return;
            }
            hasSupport = true;
        }catch (Exception e) {
            hasSupport = false;
        }
    }
}
