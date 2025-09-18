package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillTrigger;

public class OnKillTrigger {
    public static final SkillTrigger onKill = SkillTrigger.create("Kill");

    public static void register() {
        onKill.register();
    }
}

