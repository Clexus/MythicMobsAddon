package cn.clexus.mythicMobsAddon.utils;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.auras.Aura;

public class AuraUtil {
    private static final MythicBukkit b = MythicBukkit.inst();
    public static String processArgs(AbstractEntity entity, String string){
        String name = string.substring(0,string.indexOf('.'));
        String value = string.substring(string.indexOf('.') + 1);
        Aura.AuraTracker tracker = b.getSkillManager().getAuraManager().getAuraRegistry(entity).getAuras().get(name).peek();
        if(tracker == null) return "null";
        return switch (value){
            case "type" -> tracker.getGroup().orElse("");
            case "charge", "charges" -> ""+tracker.getChargesRemaining();
            case "duration" -> ""+tracker.getTicksRemaining();
            case "duration-millis" -> ""+(tracker.getTicksRemaining() * 50);
            case "stack", "stacks" -> ""+tracker.getStacks();
            default -> "";
        };
    }
}
