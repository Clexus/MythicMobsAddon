package cn.clexus.mythicMobsAddon.events;

import cn.clexus.mythicMobsAddon.addons.conditions.*;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static cn.clexus.mythicMobsAddon.events.EventsListener.eq;

public class MMOItemsEventsListener implements Listener {
    @EventHandler
    public void onMythicConditionLoad(MythicConditionLoadEvent event) {
        String name = event.getConditionName();
        if (eq(name, "holdingmmo", "holdingmmoitem")) {
            event.register(new HoldingMMOItemCondition(event.getConfig()));
        } else if (eq(name, "hasmmo", "hasmmoitem")) {
            event.register(new HasMMOItemCondition(event.getConfig()));
        }
    }
}
