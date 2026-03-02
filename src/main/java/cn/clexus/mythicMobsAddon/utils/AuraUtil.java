package cn.clexus.mythicMobsAddon.utils;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.auras.Aura;

import java.util.Locale;

public class AuraUtil {
    private static final MythicBukkit b = MythicBukkit.inst();
    public static String processArgs(AbstractEntity entity, String string){
        String name = string.substring(0,string.indexOf('.'));
        String value = string.substring(string.indexOf('.') + 1);
        var auras = b.getSkillManager().getAuraManager().getAuraRegistry(entity).getAuras().get(name);
        if (auras == null || auras.isEmpty()){
            if(value.equals("type")){
                return "";
            }else{
                return "0";
            }
        }
        String type = auras.peek().getGroup().orElse("");
        int charge = 0, minCharge = Integer.MAX_VALUE, maxCharge = Integer.MIN_VALUE, duration = 0, minDuration = Integer.MAX_VALUE, maxDuration = Integer.MIN_VALUE, stack = 0;
        for (Aura.AuraTracker tracker : auras) {
            if(tracker.getChargesRemaining() < minCharge) minCharge = tracker.getChargesRemaining();
            if(tracker.getChargesRemaining() > maxCharge) maxCharge = tracker.getChargesRemaining();
            if(tracker.getTicksRemaining() < minDuration) minDuration = tracker.getTicksRemaining();
            if(tracker.getTicksRemaining() > maxDuration) maxDuration = tracker.getTicksRemaining();
            charge += tracker.getChargesRemaining();
            duration += tracker.getTicksRemaining();
            stack += tracker.getStacks();
        }
        return switch (value.toLowerCase(Locale.ROOT)){
            case "type" -> type;
            case "charge", "charges" -> ""+charge;
            case "charge-min", "min-charge", "min-charges" -> ""+minCharge;
            case "charge-max", "max-charge", "max-charges" -> ""+maxCharge;
            case "duration" -> ""+duration;
            case "duration-min", "min-duration" -> ""+minDuration;
            case "duration-max", "max-duration" -> ""+maxDuration;
            case "duration-millis" -> ""+(duration * 50);
            case "stack", "stacks" -> ""+stack;
            default -> "";
        };
    }
}
