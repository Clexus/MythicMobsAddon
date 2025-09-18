package cn.clexus.mythicMobsAddon.addons.targeters;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.targeters.IEntitySelector;

import java.util.Collection;
import java.util.List;

public class SourceOwner extends IEntitySelector {
    protected int depth;

    public SourceOwner(SkillExecutor manager, MythicLineConfig mlc) {
        super(manager, mlc);
        this.depth = mlc.getInteger(new String[]{"depth", "d"}, Integer.MAX_VALUE);
    }

    @Override
    public Collection<AbstractEntity> getEntities(SkillMetadata skillMetadata) {
        AbstractEntity current = skillMetadata.getCaster().getEntity();

        for (int i = 0; i < depth; i++) {
            ActiveMob mob = MythicBukkit.inst().getMobManager().getMythicMobInstance(current);
            if (mob == null || !mob.getOwner().isPresent()) {
                break;
            }
            current = mob.getOwner().get();
        }

        return List.of(current);
    }

}
