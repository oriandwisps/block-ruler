package dev.blockruler.client;

import dev.blockruler.ModRegistry;
import dev.blockruler.network.S2CRulerState;
import dev.blockruler.ruler.Ruler;
import dev.blockruler.ruler.RulerState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side mirror of the ruler state. The single {@link #INSTANCE} is
 * updated from server packets and read by the rendering and tick handlers.
 * The ruler list persists even when ruler mode is off.
 */
public final class ClientRulerState {

    private ClientRulerState() {
    }

    public static final ClientRulerState INSTANCE = new ClientRulerState();

    public boolean active;
    public int selectedSlot;
    public boolean hasFirst;
    public BlockPos first;
    public final List<Ruler> rulers = new ArrayList<>();

    public static void apply(S2CRulerState payload) {
        INSTANCE.active = payload.active();
        INSTANCE.selectedSlot = payload.selectedSlot();
        INSTANCE.hasFirst = payload.hasFirst();
        INSTANCE.first = payload.hasFirst() ? payload.first() : null;
        INSTANCE.rulers.clear();
        INSTANCE.rulers.addAll(payload.rulers());
    }

    public static boolean isActive() {
        return INSTANCE.active;
    }

    /**
     * The fixed contents of the ruler hotbar: slot 0 is the Block Ruler item,
     * the rest stay empty (locked).
     */
    public static ItemStack rulerHotbarStack(int slot) {
        return switch (slot) {
            case 0 -> ModRegistry.RULER.get().getDefaultInstance();
            default -> ItemStack.EMPTY;
        };
    }
}
