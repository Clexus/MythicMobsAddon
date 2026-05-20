package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.meta.EmptyMetadata;

public class OnExpPickedUpTrigger {
    public static final SkillTrigger onExpPickedUp = SkillTrigger.create("ExpPickedUp", ExpPickedUpMeta.class, "XpPickedUp", "ExpPicked", "XpPicked");

    public static void register() {
        onExpPickedUp.register();
    }

    public static class ExpPickedUpMeta extends EmptyMetadata {}
}
