package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.ILocationCondition;
import io.lumine.mythic.core.skills.SkillCondition;
import org.bukkit.Material;

public class SimpleBlockTypeCondition extends SkillCondition implements ILocationCondition {

    private static String block;
    private final double distance;

    public SimpleBlockTypeCondition(MythicLineConfig mlc, double distance) {
        super(mlc.getLine());
        this.distance = distance;
        block = mlc.getString(new String[]{"types", "type", "t", "material", "mat", "m", "block", "b"}, "DIRT", this.conditionVar);
    }

    public boolean check(AbstractLocation location) {
        var newLoc = location.clone().subtract(0, distance, 0);
        var data = newLoc.getBlock().getBlockData();
        if (data == null) return false;
        Material material = data.getMaterial();
        return block.equalsIgnoreCase(material.toString());
    }
}

