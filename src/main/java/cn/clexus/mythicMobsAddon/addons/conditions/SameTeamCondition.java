package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.conditions.IEntityComparisonCondition;
import io.lumine.mythic.api.skills.conditions.ISkillMetaComparisonCondition;
import io.lumine.mythic.core.skills.SkillCondition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class SameTeamCondition extends SkillCondition implements ISkillMetaComparisonCondition ,IEntityComparisonCondition {

    public SameTeamCondition(MythicLineConfig config) {
        super(config.getLine());
    }

    @Override
    public boolean check(AbstractEntity abstractEntity, AbstractEntity abstractEntity1) {
        Entity entity = abstractEntity.getBukkitEntity();
        Entity entity1 = abstractEntity1.getBukkitEntity();
        return Bukkit.getScoreboardManager().getMainScoreboard().getTeams().stream().anyMatch(team -> team.hasEntity(entity) && team.hasEntity(entity1));

    }

    @Override
    public boolean check(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        Entity caster = skillMetadata.getCaster().getEntity().getBukkitEntity();
        Entity target = abstractEntity.getBukkitEntity();
        return Bukkit.getScoreboardManager().getMainScoreboard().getTeams().stream().anyMatch(team -> team.hasEntity(caster) && team.hasEntity(target));
    }
}
