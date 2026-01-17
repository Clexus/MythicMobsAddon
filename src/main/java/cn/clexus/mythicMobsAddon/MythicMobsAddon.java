package cn.clexus.mythicMobsAddon;

import cn.clexus.mythicMobsAddon.addons.placeholders.*;
import cn.clexus.mythicMobsAddon.addons.triggers.*;
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
        MythicBukkit mythicBukkit = MythicBukkit.inst();
        mythicBukkit.getPlaceholderManager().register("caster.relative", new CasterRelativeLocationPlaceholder());
        mythicBukkit.getPlaceholderManager().register("caster.pdc", new CasterPDCPlaceholder());
        mythicBukkit.getPlaceholderManager().register("caster.tickslived", new CasterTicksLivedPlaceholder());
        mythicBukkit.getPlaceholderManager().register("target.tickslived", new TargetTicksLivedPlaceholder());
        mythicBukkit.getPlaceholderManager().register("target.pdc", new TargetPDCPlaceholder());
        mythicBukkit.getPlaceholderManager().register("target.eyeheight", new TargetEyeHeightPlaceholder());
        mythicBukkit.getPlaceholderManager().register("caster.eyeheight", new CasterEyeHeightPlaceholder());
        mythicBukkit.getPlaceholderManager().register("target.height", new TargetHeightPlaceholder());
        mythicBukkit.getPlaceholderManager().register("caster.height", new CasterHeightPlaceholder());
    }

    @Override
    public void onLoad(){
        OnKillTrigger.register();
        OnBlockBreakTrigger.register();
        OnRegainHealthTrigger.register();
        OnPlayerInputTrigger.register();
        OnEntityPotionEffectTrigger.register();
        OnEntityEffectTickTrigger.register();
    }
}
