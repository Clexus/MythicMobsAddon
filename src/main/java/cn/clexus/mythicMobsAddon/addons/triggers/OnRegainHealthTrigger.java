package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.SkillTriggerMetadata;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class OnRegainHealthTrigger {
    public static final SkillTrigger onRegainHealth = SkillTrigger.create("RegainHealth", EntityRegainHealthMeta.class, "Heal");

    public static void register() {
        onRegainHealth.register();
    }

    public static class EntityRegainHealthMeta extends SkillTriggerMetadata {

        private final EntityRegainHealthEvent event;

        public EntityRegainHealthMeta(EntityRegainHealthEvent event) {
            this.event = event;
        }

        @Override
        public void applyToSkillMetadata(SkillMetadata data) {
            data.getVariables().put("amount", new StringVariable(String.valueOf(event.getAmount())));
            data.getVariables().put("reason", new StringVariable(String.valueOf(event.getRegainReason())));
        }
    }
}
