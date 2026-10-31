package dev.blockruler.ruler;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class RulerMath {

    private RulerMath() {
    }

    /**
     * Check that the second point shares at most one axis of difference
     * with the first point (a straight line, no diagonals).
     */
    public static boolean isAxisAligned(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        int axes = 0;
        if (dx > 0) axes++;
        if (dy > 0) axes++;
        if (dz > 0) axes++;
        return axes <= 1;
    }

    /**
     * Every block position on the line between the two points, inclusive.
     * Only valid for axis-aligned pairs.
     */
    public static List<BlockPos> lineBetween(BlockPos a, BlockPos b) {
        List<BlockPos> blocks = new ArrayList<>();
        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int maxY = Math.max(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxZ = Math.max(a.getZ(), b.getZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }
        return blocks;
    }

    /**
     * The number of selected blocks: both endpoints and everything between,
     * so a ruler spanning N blocks of distance counts N+1 blocks.
     */
    public static int blockCount(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return dx + dy + dz + 1;
    }

    /** The block of the line closest to the given position. */
    public static BlockPos closestBlock(List<BlockPos> line, BlockPos to) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : line) {
            double dist = pos.distSqr(to);
            if (dist < bestDist) {
                bestDist = dist;
                best = pos;
            }
        }
        return best;
    }

    /**
     * Axis label for display: which single axis the line runs along
     * (an axis-aligned line always has exactly one when endpoints differ).
     */
    public static String axisName(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        if (dy > 0) return "Y";
        if (dx > 0) return "X";
        return "Z";
    }

    /** The line label: just the number of blocks. */
    public static String describe(int blockCount) {
        return blockCount + " blocks";
    }
}
