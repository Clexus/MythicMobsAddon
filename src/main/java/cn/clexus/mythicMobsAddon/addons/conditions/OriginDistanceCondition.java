package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.conditions.ISkillMetaComparisonCondition;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.utils.numbers.RangedDouble;
import io.lumine.mythic.core.skills.SkillCondition;

public class OriginDistanceCondition extends SkillCondition implements ISkillMetaComparisonCondition {
    protected RangedDouble distance;
    private final PlaceholderString strDistance;

    public OriginDistanceCondition(MythicLineConfig mlc) {
        super(mlc.getLine());
        this.strDistance = mlc.getPlaceholderString(new String[]{"distance", "d"}, this.conditionVar);
        if (this.strDistance.isStatic()) {
            this.distance = new RangedDouble(this.strDistance.get(), true);
        } else {
            this.distance = null;
        }
    }

    public boolean check(SkillMetadata meta, AbstractEntity target) {
        AbstractLocation origin = meta.getOrigin();
        if (!origin.getWorld().equals(target.getWorld())) {
            return false;
        } else {
            double diffSq = origin.distanceSquared(target.getLocation());
            if (this.distance == null) {
                RangedDouble distance = new RangedDouble(this.strDistance.get(meta, target), true);
                return distance.equals(diffSq);
            } else {
                return this.distance.equals(diffSq);
            }
        }
    }
}
