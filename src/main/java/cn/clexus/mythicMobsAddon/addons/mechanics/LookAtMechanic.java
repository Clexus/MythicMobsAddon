package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.ITargetedLocationSkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.io.File;

public class LookAtMechanic extends SkillMechanic implements ITargetedEntitySkill, ITargetedLocationSkill {
    protected LookAnchor lookAnchor;

    public LookAtMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        lookAnchor = mlc.getEnum(new String[]{"lookanchor", "la"}, LookAnchor.class, LookAnchor.EYES);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        var caster = skillMetadata.getCaster().getEntity().getBukkitEntity();
        var target = abstractEntity.getBukkitEntity();
        if (caster != null && target != null) {
            lookAt(caster, target.getLocation(), lookAnchor);
            return SkillResult.SUCCESS;
        }
        return SkillResult.INVALID_TARGET;
    }

    @Override
    public SkillResult castAtLocation(SkillMetadata skillMetadata, AbstractLocation abstractLocation) {
        var caster = skillMetadata.getCaster().getEntity().getBukkitEntity();
        if (caster != null) {
            lookAt(caster, abstractLocation.toPosition().toLocation(), lookAnchor);
            return SkillResult.SUCCESS;
        }
        return SkillResult.INVALID_TARGET;
    }

    private void lookAt(Entity caster, Location location, LookAnchor lookAnchor) {
        caster.lookAt(location, lookAnchor);
    }
}
