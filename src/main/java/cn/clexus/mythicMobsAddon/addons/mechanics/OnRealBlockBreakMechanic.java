//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.adapters.BukkitTriggerMetadata;
import io.lumine.mythic.bukkit.utils.Events;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import io.lumine.mythic.core.skills.targeters.ILocationSelector;
import io.lumine.mythic.core.utils.annotations.MythicField;
import io.lumine.mythic.core.utils.annotations.MythicMechanic;
import org.bukkit.event.block.BlockBreakEvent;

import java.io.File;
import java.util.List;
import java.util.Optional;

@MythicMechanic(
        author = "Ashijin",
        name = "onrealblockbreak",
        aliases = {"onrealblockbreak"},
        description = "Applies an aura to the target that triggers a skill when they break a block"
)
public class OnRealBlockBreakMechanic extends Aura implements ITargetedEntitySkill {
    @MythicField(
            name = "onRealBreak",
            aliases = {"orb"},
            version = "4.13",
            description = "Skill to execute if the target hits something"
    )
    protected Optional<Skill> onBreakSkill = Optional.empty();
    protected String onBreakSkillName;
    @MythicField(
            name = "blocktype",
            aliases = {"blocktypes", "bt", "t", "material", "materials", "mat", "m", "blocks", "block", "b"},
            description = "Blocks to add to the Vein",
            defValue = "STONE"
    )
    protected PlaceholderString blockType;
    @MythicField(
            name = "cancelEvent",
            aliases = {"ce"},
            defValue = "false",
            version = "4.13",
            description = "Whether or not to cancel the event that triggered the aura"
    )
    protected boolean cancelEvent;
    @MythicField(
            name = "dropItem",
            aliases = {"drop", "allowDrop"},
            defValue = "true",
            description = "Whether or not to drop the item"
    )
    protected boolean dropItems;

    public OnRealBlockBreakMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onBreakSkillName = mlc.getString(new String[]{"onrealbreak"});
        this.blockType = mlc.getPlaceholderString(new String[]{"blocktypes", "blocktypes", "bt", "t", "material", "materials", "m", "blocks", "block", "b"}, (String)null, new String[0]);
        this.cancelEvent = mlc.getBoolean(new String[]{"cancelevent", "ce", "cancel"}, false);
        this.dropItems = mlc.getBoolean(new String[]{"dropItem", "drop", "allowDrop"}, true);
        this.getManager().queueSecondPass(() -> {
            if (this.onBreakSkillName != null) {
                this.onBreakSkill = this.getManager().getSkill(file, this, this.onBreakSkillName);
            }

        });
    }

    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        new Tracker(data, target);
        return SkillResult.SUCCESS;
    }

    private class Tracker extends Aura.AuraTracker implements IParentSkill, Runnable {
        public Tracker(SkillMetadata data, AbstractEntity entity) {
            super(entity, data);
            this.start();
        }

        public void auraStart() {
            this.registerAuraComponent(Events.subscribe(BlockBreakEvent.class).filter((event) -> event.getPlayer().getUniqueId().equals(this.entity.get().getUniqueId()) && !event.isCancelled()).filter((event) -> {
                if (OnRealBlockBreakMechanic.this.blockType != null) {
                    PlaceholderContext context = PlaceholderContext.builder().meta(this.skillMetadata.deepClone()).entity(this.entity.get()).build();
                    List<String> wantedBlocks = List.of(OnRealBlockBreakMechanic.this.blockType.get(context).split(","));
                    return ILocationSelector.blockMatches(event.getBlock().getType(), wantedBlocks);
                } else {
                    return true;
                }
            }).handler((event) -> {
                SkillMetadata meta = this.skillMetadata.deepClone();
                AbstractLocation target = BukkitAdapter.adapt(event.getBlock().getLocation()).add(0.5F, 0.5F, 0.5F);
                meta.setLocationTarget(target);
                BukkitTriggerMetadata.apply(meta, event);
                if (OnRealBlockBreakMechanic.this.blockType != null) {
                    PlaceholderContext context = PlaceholderContext.builder().meta(meta).entity(this.entity.get()).build();
                    List<String> wantedBlocks = List.of(OnRealBlockBreakMechanic.this.blockType.get(context).split(","));
                    if (!ILocationSelector.blockMatches(event.getBlock().getType(), wantedBlocks)) {
                        return;
                    }
                }

                if (this.executeAuraSkill(OnRealBlockBreakMechanic.this.onBreakSkill, meta)) {
                    this.consumeCharge();
                    if (OnRealBlockBreakMechanic.this.cancelEvent) {
                        event.setCancelled(true);
                    } else if (!OnRealBlockBreakMechanic.this.dropItems) {
                        event.setDropItems(false);
                    }
                }

            }));
            this.executeAuraSkill(OnRealBlockBreakMechanic.this.onStartSkill, this.skillMetadata);
        }
    }
}
