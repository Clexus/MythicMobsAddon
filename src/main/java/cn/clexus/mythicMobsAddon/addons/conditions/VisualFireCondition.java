package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.SkillCondition;
import net.kyori.adventure.util.TriState;

import java.util.Locale;

public class VisualFireCondition extends SkillCondition implements IEntityCondition {
    private TriState state;
    public VisualFireCondition(MythicLineConfig config) {
        super(config.getLine());
        try{
            state = TriState.valueOf(config.getString(new String[]{"state", "s"}, "TRUE").toUpperCase(Locale.ROOT));
        }catch (IllegalArgumentException e){
            MythicLogger.errorCondition(this, "State can only be one of values below: TRUE, FALSE, NOT_SET");
        }
    }

    @Override
    public boolean check(AbstractEntity abstractEntity) {
        return abstractEntity.getBukkitEntity().getVisualFire() == this.state;
    }
}
