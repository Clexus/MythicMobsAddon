package cn.clexus.mythicMobsAddon.events;

import cn.clexus.mythicMobsAddon.addons.triggers.OnBlockBreakTrigger;
import cn.clexus.mythicMobsAddon.addons.triggers.OnPlayerInputTrigger;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitPlayer;
import io.lumine.mythic.bukkit.utils.serialize.Position;
import io.lumine.mythic.core.players.PlayerData;
import io.lumine.mythic.core.skills.EventExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import io.lumine.mythic.core.skills.TriggeredSkill;
import io.lumine.mythiccrucible.MythicCrucible;
import io.lumine.mythiccrucible.items.ItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class CrucibleEventsListener implements Listener {
    MythicBukkit mythicBukkit = MythicBukkit.inst();
    EventExecutor eventExecutor = mythicBukkit.getSkillManager().getEventBus();
    private static final ItemManager manager = MythicCrucible.inst().getItemManager();
    @EventHandler(ignoreCancelled = true,
            priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        PlayerData data = new PlayerData(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        data.initialize(new BukkitPlayer(event.getPlayer()));
        Collection<SkillMechanic> mechanics = manager.getItem(event.getPlayer().getInventory().getItemInMainHand()).flatMap(item -> item.getSkills(OnBlockBreakTrigger.onBlockBreak)).orElse(null);
        if(mechanics != null) {
            TriggeredSkill ts = new TriggeredSkill(OnBlockBreakTrigger.onBlockBreak, data, new AbstractLocation(Position.of(event.getBlock())), new BukkitPlayer(event.getPlayer()), mechanics, true);
        }
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        OnPlayerInputTrigger.PlayerInputMeta meta = new OnPlayerInputTrigger.PlayerInputMeta(event);
        PlayerData playerData = new PlayerData(player.getUniqueId(), player.getName());
        playerData.initialize(new BukkitPlayer(player));
        SkillMetadata data = eventExecutor.buildSkillMetadata(OnPlayerInputTrigger.onPlayerInput, meta, playerData, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);
        TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
    }

    public static Queue<SkillMechanic> getAllMechanics(Player player, SkillTrigger<?> trigger) {
        Queue<SkillMechanic> mechanics = new LinkedList<>();

        // 主手
        manager.getItem(player.getInventory().getItemInMainHand())
                .flatMap(item -> item.getSkills(trigger))
                .ifPresent(mechanics::addAll);

        // 副手
        manager.getItem(player.getInventory().getItemInOffHand())
                .flatMap(item -> item.getSkills(trigger))
                .ifPresent(mechanics::addAll);

        // 护甲
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null) continue;
            manager.getItem(armor)
                    .flatMap(item -> item.getSkills(trigger))
                    .ifPresent(mechanics::addAll);
        }

        return mechanics;
    }


}
