package cn.clexus.mythicMobsAddon.addons.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.core.skills.SkillCondition;
import org.bukkit.Input;
import org.bukkit.entity.Player;

public class InputCondition extends SkillCondition implements IEntityCondition {
    private final Boolean isForward;
    private final Boolean isBackward;
    private final Boolean isLeft;
    private final Boolean isRight;
    private final Boolean isJump;
    private final Boolean isSneaking;
    private final Boolean isSprint;

    public InputCondition(MythicLineConfig config) {
        super(config.getLine());
        this.isForward = config.getBoolean("forward");
        this.isBackward = config.getBoolean("backward");
        this.isLeft = config.getBoolean("left");
        this.isRight = config.getBoolean("right");
        this.isJump = config.getBoolean("jump");
        this.isSneaking = config.getBoolean("sneak");
        this.isSprint = config.getBoolean("sprint");
    }

    @Override
    public boolean check(AbstractEntity entity) {
        if (entity.getBukkitEntity() instanceof Player player) {
            Input input = player.getCurrentInput();

            return (isForward == null || input.isForward() == isForward) &&
                    (isBackward == null || input.isBackward() == isBackward) &&
                    (isLeft == null || input.isLeft() == isLeft) &&
                    (isRight == null || input.isRight() == isRight) &&
                    (isJump == null || input.isJump() == isJump) &&
                    (isSneaking == null || input.isSneak() == isSneaking) &&
                    (isSprint == null || input.isSprint() == isSprint);
        }
        return false;
    }
}
