package cn.clexus.mythicMobsAddon.addons.targeters;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.api.skills.placeholders.PlaceholderInt;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.targeters.ILocationSelector;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLocationsOfBoundingBoxTargeter extends ILocationSelector {
    private PlaceholderInt amount;
    private PlaceholderDouble minScale;
    private PlaceholderDouble maxScale;

    public RandomLocationsOfBoundingBoxTargeter(SkillExecutor manager, MythicLineConfig mlc) {
        super(manager, mlc);
        this.amount = mlc.getPlaceholderInteger(new String[]{"amount", "a"}, 5);
        this.minScale = mlc.getPlaceholderDouble(new String[]{"minScale", "mins"}, 0);
        this.maxScale = mlc.getPlaceholderDouble(new String[]{"maxScale", "maxs"}, 1);
    }

    @Override
    public Collection<AbstractLocation> getLocations(SkillMetadata skillMetadata) {
        var random = ThreadLocalRandom.current();
        HashSet<AbstractLocation> locations = new HashSet<>();
        var context = PlaceholderContext.builder().meta(skillMetadata).caster(skillMetadata.getCaster()).build();
        int amount = this.amount.get(context);
        double minScale = this.minScale.get(context);
        double maxScale = this.maxScale.get(context);
        Entity entity = skillMetadata.getCaster().getEntity().getBukkitEntity();
        var box = entity.getBoundingBox();
        var world = entity.getWorld();
        for (int i = 0; i < amount; i++) {
            Location location = box.getMin().add(new Vector(random.nextDouble(minScale, maxScale) * box.getWidthX(), random.nextDouble(minScale, maxScale) * box.getHeight(), random.nextDouble(minScale, maxScale) * box.getWidthZ())).toLocation(world);
            locations.add(BukkitAdapter.adapt(location));
        }
        return locations;
    }
}
