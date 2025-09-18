package cn.clexus.mythicMobsAddon;

import cn.clexus.mythicMobsAddon.addons.placeholders.CasterRelativeLocationPlaceholder;
import cn.clexus.mythicMobsAddon.addons.triggers.OnBlockBreakTrigger;
import cn.clexus.mythicMobsAddon.addons.triggers.OnKillTrigger;
import cn.clexus.mythicMobsAddon.addons.triggers.OnPlayerInputTrigger;
import cn.clexus.mythicMobsAddon.addons.triggers.OnRegainHealthTrigger;
import cn.clexus.mythicMobsAddon.events.CrucibleEventsListener;
import cn.clexus.mythicMobsAddon.events.EventsListener;
import cn.clexus.mythicMobsAddon.support.CrucibleSupport;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MythicMobsAddon extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new EventsListener(), this);
        CrucibleSupport.init(this);
        if(CrucibleSupport.hasSupport()){
            Bukkit.getPluginManager().registerEvents(new CrucibleEventsListener(), this);
        }
        MythicBukkit.inst().getPlaceholderManager().register("caster.relative", new CasterRelativeLocationPlaceholder());
    }

    @Override
    public void onLoad(){
        OnKillTrigger.register();
        OnBlockBreakTrigger.register();
        OnRegainHealthTrigger.register();
        OnPlayerInputTrigger.register();
    }
}
