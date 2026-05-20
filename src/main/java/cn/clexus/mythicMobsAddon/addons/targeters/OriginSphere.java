package cn.clexus.mythicMobsAddon.addons.targeters;

import com.google.common.collect.Lists;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.adapters.AbstractVector;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.api.skills.placeholders.PlaceholderInt;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.targeters.ILocationSelector;
import io.lumine.mythic.core.utils.RandomUtil;

import java.util.Collection;

public class OriginSphere extends ILocationSelector {
    private final PlaceholderDouble radius;
    private final PlaceholderDouble yOffset;
    private final PlaceholderInt points;
    private final boolean exact;

    public OriginSphere(SkillExecutor manager, MythicLineConfig mlc) {
        super(manager, mlc);
        this.radius = mlc.getPlaceholderDouble(new String[]{"radius", "r"}, 2.0F);
        this.points = mlc.getPlaceholderInteger(new String[]{"points", "p"}, 32);
        this.yOffset = mlc.getPlaceholderDouble(new String[]{"yoffset", "y"}, 0.0F);
        this.exact = mlc.getBoolean(new String[]{"exact", "e"}, false);
    }

    public Collection<AbstractLocation> getLocations(SkillMetadata data) {
        Collection<AbstractLocation> targets = Lists.newArrayList();
        AbstractLocation location = data.getOrigin().clone();
        location.add(0.0F, this.yOffset.get(data), 0.0F);
        if (this.exact) {
            targets = this.generateExactSphere(location, this.radius.get(data), this.points.get(data));
        } else {
            for(int i = 0; i < this.points.get(data); ++i) {
                AbstractVector vector = RandomUtil.getRandomVector().multiply(this.radius.get(data));
                targets.add(location.clone().add(vector));
            }
        }

        return targets;
    }

    private Collection<AbstractLocation> generateExactSphere(AbstractLocation center, double radius, int points) {
        Collection<AbstractLocation> targets = Lists.newArrayList();
        double increment = Math.PI * ((double)3.0F - Math.sqrt(5.0F));

        for(int i = 0; i < points; ++i) {
            double y = (double)1.0F - (double)i / (double)(points - 1) * (double)2.0F;
            double radiusAtY = Math.sqrt((double)1.0F - y * y);
            double theta = (double)i * increment;
            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;
            targets.add(center.clone().add(x * radius, y * radius, z * radius));
        }

        return targets;
    }
}
