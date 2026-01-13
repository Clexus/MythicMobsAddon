package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;

import java.io.File;
import java.util.Locale;

public class SetTicksLivedMechanic extends SkillMechanic implements ITargetedEntitySkill {
    private final int ticks;

    public SetTicksLivedMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);

        this.ticks = mlc.getInteger(new String[]{"ticks", "t"}, 0);
        Locale locale = Locale.getDefault();
        if(locale.equals(Locale.CHINA)){
            MythicLogger.errorMechanicConfig(this, mlc,"SetTicksLived技能中的ticks必须大于0");
        }else{
            MythicLogger.errorMechanicConfig(this, mlc,"ticks in SetTicksLived mechanic must be greater than 0");
        }
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {

        abstractEntity.getBukkitEntity().setTicksLived(this.ticks);
        return SkillResult.SUCCESS;
    }
}
