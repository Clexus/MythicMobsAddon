package cn.clexus.mythicMobsAddon.addons.targeters;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.targeters.IEntitySelector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class TeamTargeter extends IEntitySelector {
    protected boolean sameTeam;
    protected String teamName;

    public TeamTargeter(SkillExecutor manager, MythicLineConfig mlc) {
        super(manager, mlc);
        this.teamName = mlc.getString(new String[]{"team", "t"}, null);
        this.sameTeam = mlc.getBoolean(new String[]{"sameteam", "sameTeam", "st"}, false);
    }

    @Override
    public Collection<AbstractEntity> getEntities(SkillMetadata skillMetadata) {
        var entities = new ArrayList<AbstractEntity>();
        if(!(teamName == null || teamName.isEmpty())) {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
            if(team != null) {
                for (var entry : team.getEntries()) {
                    UUID uuid;
                    Entity entity;
                    try{
                        uuid = UUID.fromString(entry);
                        entity = Bukkit.getEntity(uuid);
                    }catch(IllegalArgumentException e){
                        entity = Bukkit.getPlayer(entry);
                    }
                    if (entity != null && team.hasEntity(skillMetadata.getCaster().getEntity().getBukkitEntity()) == sameTeam) {
                        entities.add(BukkitAdapter.adapt(entity));
                    }
                }
            }
        }
        return entities;
    }
}
