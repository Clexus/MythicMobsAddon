package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.core.skills.SkillCondition;
import org.bukkit.entity.Pose;

public class PoseCondition extends SkillCondition implements IEntityCondition {
    private Pose pose;
    public PoseCondition(MythicLineConfig config) {
        super(config.getLine());
        this.pose = config.getEnum(new String[]{ "pose", "p"}, Pose.class, Pose.STANDING);
    }

    @Override
    public boolean check(AbstractEntity abstractEntity) {
        return abstractEntity.getBukkitEntity().getPose() == this.pose;
    }
}
