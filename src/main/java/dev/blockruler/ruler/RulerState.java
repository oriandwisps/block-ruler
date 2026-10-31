package dev.blockruler.ruler;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player ruler mode state, kept separately per logical side:
 * - Client side: the UI reads this to render the cyan hotbar, particles and
 *   floating block-count text.
 * - Server side: tracks which players are in ruler mode, their pending
 *   selection and their persistent rulers.
 */
public class RulerState {

    public static final int MAX_RULERS = 10;

    public static final Map<UUID, RulerState> CLIENT = new HashMap<>();
    public static final Map<UUID, RulerState> SERVER = new HashMap<>();

    public boolean active;
    public int selectedSlot;

    // The selection currently being measured (not yet a ruler).
    public boolean hasFirst;
    public int firstX, firstY, firstZ;

    // Completed rulers, persistent until discarded.
    public final List<Ruler> rulers = new ArrayList<>();

    public static RulerState get(Map<UUID, RulerState> map, UUID id) {
        return map.computeIfAbsent(id, k -> new RulerState());
    }

    public BlockPos firstPos() {
        return new BlockPos(firstX, firstY, firstZ);
    }

    public void setFirst(BlockPos pos) {
        hasFirst = true;
        firstX = pos.getX();
        firstY = pos.getY();
        firstZ = pos.getZ();
    }

    public void clearSelection() {
        hasFirst = false;
        firstX = firstY = firstZ = 0;
    }

    public boolean hasRoomForRuler() {
        return rulers.size() < MAX_RULERS;
    }
}
