package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.core.skills.SkillCondition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Sittable;

public class SitCondition extends SkillCondition implements IEntityCondition {
    private boolean sit;
    public SitCondition(MythicLineConfig config) {
        super(config.getLine());
        this.sit = config.getBoolean(new String[]{ "sitting","sit", "s"}, true);
    }

    @Override
    public boolean check(AbstractEntity abstractEntity) {
        Entity entity = abstractEntity.getBukkitEntity();
        if(entity instanceof Sittable sittable) {
            return sit == sittable.isSitting();
        }else return false;
    }
}
