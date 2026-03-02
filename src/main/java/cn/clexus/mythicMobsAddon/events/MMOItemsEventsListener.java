package cn.clexus.mythicMobsAddon.events;

import cn.clexus.mythicMobsAddon.addons.conditions.HasMMOItemCondition;
import cn.clexus.mythicMobsAddon.addons.conditions.HoldingMMOItemCondition;
import cn.clexus.mythicMobsAddon.addons.mechanics.OnMmoAttackMechanic;
import cn.clexus.mythicMobsAddon.addons.mechanics.OnMmoDamagedMechanic;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
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

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event) {
        String m = event.getMechanicName();
        if (eq(m, "onmmoattack")) {
            event.register(new OnMmoAttackMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "onmmodamaged", "onmmodamage")) {
            event.register(new OnMmoDamagedMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        }
    }
}
