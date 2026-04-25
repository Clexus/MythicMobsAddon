package cn.clexus.mythicMobsAddon.addons.placeholders;

import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.placeholders.segments.types.ResolvedPlaceholderSegment;
import io.lumine.mythic.core.skills.placeholders.types.GenericPlaceholder;
import io.lumine.mythic.core.skills.placeholders.types.GenericPlaceholderTypes.StringPlaceholder;
import io.lumine.mythic.core.utils.annotations.MythicPlaceholder;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@MythicPlaceholder(placeholder = "target.relative", usedPlaceholderArguments = 2)
public class TargetRelativeLocationPlaceholder extends GenericPlaceholder<String> implements StringPlaceholder {

    private final ResolvedPlaceholderSegment<PlaceholderString> axis;
    private final ResolvedPlaceholderSegment<PlaceholderString> values;

    public TargetRelativeLocationPlaceholder(GenericPlaceholder.GenericPlaceholderArguments metaContext) {
        super(metaContext);
        this.axis = getPlaceholderString(0);
        this.values = getPlaceholderString(1);
    }

    @Nullable
    @Override
    public String applyWithMetaKeywords(PlaceholderContext placeholderContext) {
        if (placeholderContext.entity() == null) {
            return "0";
        }

        String axisValue = axis.get(placeholderContext, PlaceholderString::get);
        String valuesValue = values.get(placeholderContext, PlaceholderString::get);
        if (axisValue == null) {
            axisValue = "";
        }
        if (valuesValue == null) {
            valuesValue = "";
        }

        String[] split = valuesValue.split(",");
        if (split.length != 3) {
            return "0";
        }

        double rx;
        double ry;
        double rz;
        try {
            rx = Double.parseDouble(split[0]);
            ry = Double.parseDouble(split[1]);
            rz = Double.parseDouble(split[2]);
        } catch (NumberFormatException ex) {
            return "0";
        }

        Location targetLoc = BukkitAdapter.adapt(placeholderContext.entity().getLocation());
        Location result = rotateRelative(targetLoc, rx, ry, rz);
        return switch (axisValue) {
            case "ox" -> String.valueOf(result.getX() - targetLoc.getX());
            case "oy" -> String.valueOf(result.getY() - targetLoc.getY());
            case "oz" -> String.valueOf(result.getZ() - targetLoc.getZ());
            case "ax" -> String.valueOf(result.getX());
            case "ay" -> String.valueOf(result.getY());
            case "az" -> String.valueOf(result.getZ());
            default -> "0";
        };
    }

    private static Location rotateRelative(Location origin, double rx, double ry, double rz) {
        Vector forward = origin.getDirection().normalize();
        Vector upWorld = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(upWorld).normalize();
        if (right.lengthSquared() < 0.0001) {
            right = new Vector(1, 0, 0);
        }
        Vector up = right.clone().crossProduct(forward).normalize();
        Vector offset = right.multiply(rx).add(up.multiply(ry)).add(forward.multiply(rz));
        return origin.clone().add(offset);
    }
}

