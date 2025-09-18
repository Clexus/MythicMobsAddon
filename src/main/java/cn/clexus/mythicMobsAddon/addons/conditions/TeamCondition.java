package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.core.skills.SkillCondition;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Team;

public class TeamCondition extends SkillCondition implements IEntityCondition {
    private final String teamName;

    public TeamCondition(MythicLineConfig config) {
        super(config.getLine());
        this.teamName = config.getString(new String[] { "team","t" });
    }

    @Override
    public boolean check(AbstractEntity abstractEntity) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
        if(team == null) {
            return false;
        }
        return team.hasEntity(abstractEntity.getBukkitEntity());
    }
}
