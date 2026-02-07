package cn.clexus.mythicMobsAddon;

import cn.clexus.mythicMobsAddon.addons.placeholders.*;
import cn.clexus.mythicMobsAddon.addons.triggers.*;
import cn.clexus.mythicMobsAddon.events.CrucibleEventsListener;
import cn.clexus.mythicMobsAddon.events.EventsListener;
import cn.clexus.mythicMobsAddon.events.MMOItemsEventsListener;
import cn.clexus.mythicMobsAddon.support.CrucibleSupport;
import cn.clexus.mythicMobsAddon.support.MMOItemsSupport;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;

public final class MythicMobsAddon extends JavaPlugin {

    public static MythicMobsAddon plugin;

    @Override
    public void onEnable() {
        plugin = this;
        Bukkit.getPluginManager().registerEvents(new EventsListener(), this);
        CrucibleSupport.init(this);
        MMOItemsSupport.init(this);
        if(CrucibleSupport.hasSupport()){
            Bukkit.getPluginManager().registerEvents(new CrucibleEventsListener(), this);
        }
        if(MMOItemsSupport.hasSupport()){
            Bukkit.getPluginManager().registerEvents(new MMOItemsEventsListener(), this);
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
        mythicBukkit.getPlaceholderManager().register("caster.aura", new CasterAuraPlaceholder());
        mythicBukkit.getPlaceholderManager().register("target.aura", new TargetAuraPlaceholder());
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, r->{
            r.registrar().register(Commands.literal("mmaddon")
                            .then(Commands.literal("moblist")
                                    .executes(ctx->{
                                        var allMobs = MythicBukkit.inst().getMobManager().getActiveMobs();
                                        LinkedHashMap<String, Integer> mobCount = new LinkedHashMap<>();
                                        allMobs.forEach(mob -> {
                                            String internalName = mob.getType().getInternalName();
                                            mobCount.put(internalName, mobCount.getOrDefault(internalName, 0) + 1);
                                        });
                                        mobCount.entrySet().stream()
                                                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                                                .forEach(entry -> {
                                                    ctx.getSource().getSender().sendRichMessage(entry.getKey() + ": " + entry.getValue());
                                                });
                                        return 1;
                                    })
                            )
                    .build()
            );
        });
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
