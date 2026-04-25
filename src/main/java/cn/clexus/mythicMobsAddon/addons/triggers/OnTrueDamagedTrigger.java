package cn.clexus.mythicMobsAddon.addons.triggers;

import cn.clexus.mythicMobsAddon.events.EntityTrueDamageEvent;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.SkillTriggerMetadata;
import io.lumine.mythic.core.skills.variables.types.StringVariable;

public class  OnTrueDamagedTrigger {
    public static final SkillTrigger onTrueDamaged = SkillTrigger.create("TrueDamaged", EntityTrueDamagedMeta.class, "TrueDamage");

    public static void register() {
        onTrueDamaged.register();
    }

    public static class EntityTrueDamagedMeta extends SkillTriggerMetadata {

        private final EntityTrueDamageEvent event;

        public EntityTrueDamagedMeta(EntityTrueDamageEvent event) {
            this.event = event;
        }

        @Override
        public void applyToSkillMetadata(SkillMetadata data) {
            data.getVariables().put("amount", new StringVariable(String.valueOf(event.getDamage())));
            data.getVariables().put("damage-amount", new StringVariable(String.valueOf(event.getDamage())));
            data.getVariables().put("damager", new StringVariable(event.getDamager().getName()));
        }
    }
}
