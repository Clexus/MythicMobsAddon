package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.SkillTriggerMetadata;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import org.bukkit.event.player.PlayerInputEvent;

public class OnPlayerInputTrigger {
    public static final SkillTrigger onPlayerInput = SkillTrigger.create("Input", PlayerInputMeta.class);

    public static void register() {
        onPlayerInput.register();
    }

    public static class PlayerInputMeta extends SkillTriggerMetadata {

        private final PlayerInputEvent event;

        public PlayerInputMeta(PlayerInputEvent event) {
            this.event = event;
        }

        @Override
        public void applyToSkillMetadata(SkillMetadata data) {
            data.getVariables().put("input", new StringVariable(String.valueOf(event.getInput())));
        }
    }
}
