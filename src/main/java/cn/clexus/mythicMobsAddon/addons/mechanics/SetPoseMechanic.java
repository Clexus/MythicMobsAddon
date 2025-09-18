package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.entity.Pose;

import java.io.File;

public class SetPoseMechanic extends SkillMechanic implements ITargetedEntitySkill {
    private Pose pose;
    private boolean fixed;
    public SetPoseMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.line = line;
        this.threadSafetyLevel = ThreadSafetyLevel.EITHER;

        this.fixed = mlc.getBoolean(new String[] { "fixed" , "f"}, false);
        this.pose = mlc.getEnum(new String[]{ "pose", "p"}, Pose.class, Pose.STANDING);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        target.getBukkitEntity().setPose(pose, fixed);
        return SkillResult.SUCCESS;
    }
}