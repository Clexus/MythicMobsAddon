package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.SkillTriggerMetadata;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import org.bukkit.event.player.PlayerInputEvent;

import java.util.HashMap;
import java.util.Map;

public class OnPlayerInputTrigger {
    public static final SkillTrigger onPlayerInput = SkillTrigger.create("Input", PlayerInputMeta.class);

    public static void register() {
        onPlayerInput.register();
    }

    public static class PlayerInputMeta extends SkillTriggerMetadata {

        private final PlayerInputEvent event;
        Map<String,Boolean> inputs = new HashMap<>(Map.of(
                "forward", false,
                "backward", false,
                "left", false,
                "right", false,
                "jump", false,
                "sneak", false,
                "sprint", false
        ));

        public PlayerInputMeta(PlayerInputEvent event) {
            this.event = event;
            inputs.replace("backward", event.getInput().isBackward());
            inputs.replace("forward", event.getInput().isForward());
            inputs.replace("left", event.getInput().isLeft());
            inputs.replace("right", event.getInput().isRight());
            inputs.replace("jump", event.getInput().isJump());
            inputs.replace("sneak", event.getInput().isSneak());
            inputs.replace("sprint", event.getInput().isSprint());
        }

        @Override
        public void applyToSkillMetadata(SkillMetadata data) {
            StringBuilder inputStr = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : inputs.entrySet()) {
                if (entry.getValue()) {
                    inputStr.append(entry.getKey()).append(",");
                }
            }
            if (!inputStr.isEmpty()) {
                inputStr.setLength(inputStr.length() - 1);
            }else{
                inputStr.append("none");
            }
            data.getVariables().put("input", new StringVariable(inputStr.toString()));
            data.getVariables().put("forward", new StringVariable(String.valueOf(inputs.get("forward"))));
            data.getVariables().put("backward", new StringVariable(String.valueOf(inputs.get("backward"))));
            data.getVariables().put("left", new StringVariable(String.valueOf(inputs.get("left"))));
            data.getVariables().put("right", new StringVariable(String.valueOf(inputs.get("right"))));
            data.getVariables().put("jump", new StringVariable(String.valueOf(inputs.get("jump"))));
            data.getVariables().put("sneak", new StringVariable(String.valueOf(inputs.get("sneak"))));
            data.getVariables().put("sprint", new StringVariable(String.valueOf(inputs.get("sprint"))));
        }
    }
}
