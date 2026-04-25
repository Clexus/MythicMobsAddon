package cn.clexus.mythicMobsAddon;

import cn.clexus.mythicMobsAddon.addons.mechanics.TempAttributeModifierMechanic;
import cn.clexus.mythicMobsAddon.addons.triggers.*;
import cn.clexus.mythicMobsAddon.events.CrucibleEventsListener;
import cn.clexus.mythicMobsAddon.events.EventsListener;
import cn.clexus.mythicMobsAddon.events.MMOItemsEventsListener;
import cn.clexus.mythicMobsAddon.support.CrucibleSupport;
import cn.clexus.mythicMobsAddon.support.MMOItemsSupport;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.CustomComponentRegistry;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class MythicMobsAddon extends JavaPlugin {

    public static MythicMobsAddon plugin;
    private CustomComponentRegistry componentRegistry;

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
        componentRegistry = new CustomComponentRegistry(this, new ArrayList<>())
                .registerCustomComponent(
                        CustomComponentRegistry.MythicComponentType.PLACEHOLDER,
                        "cn.clexus.mythicMobsAddon.addons.placeholders"
                );
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
        OnTrueDamagedTrigger.register();
        OnWitchThrowPotionTrigger.register();
        OnWitchConsumePotionTrigger.register();
    }

    @Override
    public void onDisable() {
        TempAttributeModifierMechanic.clearScheduledRemovals();
    }

    public CustomComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }
}
