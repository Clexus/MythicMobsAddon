package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import net.kyori.adventure.util.TriState;

import java.io.File;
import java.util.Locale;

public class SetVisualFireMechanic extends SkillMechanic implements ITargetedEntitySkill {
    private TriState state;

    public SetVisualFireMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);

        this.state = TriState.TRUE;
        try{
            state = TriState.valueOf(mlc.getString(new String[]{"state", "s"}, "TRUE").toUpperCase(Locale.ROOT));
        }catch (IllegalArgumentException e){
            MythicLogger.errorMechanicConfig(this, mlc, "State can only be one of values below: TRUE, FALSE, NOT_SET");
        }
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        abstractEntity.getBukkitEntity().setVisualFire(this.state);
        return SkillResult.SUCCESS;
    }
}
