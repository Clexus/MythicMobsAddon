package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillTrigger;

public class OnBlockBreakTrigger {
    public static final SkillTrigger onBlockBreak = SkillTrigger.create("RealBlockBreak");

    public static void register() {
        onBlockBreak.register();
    }
}
