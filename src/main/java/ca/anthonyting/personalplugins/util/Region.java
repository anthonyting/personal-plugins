package ca.anthonyting.personalplugins.util;

import org.bukkit.Location;

public class Region {
    private final int x;
    private final int z;
    private final int radius;

    public Region(Location location, int radius) {
        this.x = location.getBlockX();
        this.z = location.getBlockZ();
        this.radius = radius;
    }

    public boolean contains(Location location) {
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        return (x >= this.x - radius && x <= this.x + radius) && (z >= this.z - radius && z <= this.z + radius);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Region region = (Region) o;
        return x == region.x && z == region.z && radius == region.radius;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + z;
        result = 31 * result + radius;
        return result;
    }

    @Override
    public String toString() {
        return "Region{" +
                "x=" + x +
                ", z=" + z +
                ", radius=" + radius +
                '}';
    }
}
