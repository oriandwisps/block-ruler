package dev.blockruler.ruler;

import dev.blockruler.network.RulerNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Server-side ruler actions: placing points, completing rulers and
 * discarding them. The server is authoritative; every change is synced.
 */
public final class RulerLogic {

    private RulerLogic() {
    }

    /** The block the player is looking at, or null if nothing in reach. */
    public static BlockPos targetedBlock(ServerPlayer player) {
        BlockHitResult hit = Item.getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.NONE);
        return hit.getType() == HitResult.Type.MISS ? null : hit.getBlockPos();
    }

    /** Toggle ruler mode on/off. */
    public static void toggle(ServerPlayer player, RulerState state) {
        state.active = !state.active;
        if (state.active) {
            player.sendSystemMessage(Component.translatable("message.blockruler.on"));
        } else {
            // Leaving ruler mode keeps the rulers; only the pending
            // selection in progress is dropped.
            state.clearSelection();
            player.sendSystemMessage(Component.translatable("message.blockruler.off"));
        }
        RulerNetwork.sendState(player, state);
    }

    /** Right-click: place a measurement point. */
    public static void placePoint(ServerPlayer player, RulerState state) {
        BlockPos pos = targetedBlock(player);
        if (pos == null) {
            return;
        }

        if (!state.hasFirst) {
            state.setFirst(pos);
            RulerNetwork.sendState(player, state);
            return;
        }

        BlockPos first = state.firstPos();
        if (!RulerMath.isAxisAligned(first, pos)) {
            return; // not synced: keep the first point, let the player retry
        }

        // Start a brand new ruler from this block instead of creating a
        // zero-length one when clicking the same block twice.
        if (first.equals(pos)) {
            state.setFirst(pos);
            RulerNetwork.sendState(player, state);
            return;
        }

        if (!state.hasRoomForRuler()) {
            player.sendSystemMessage(
                    Component.translatable("message.blockruler.limit", RulerState.MAX_RULERS)
                            .withStyle(ChatFormatting.RED));
            state.clearSelection();
            RulerNetwork.sendState(player, state);
            return;
        }

        state.rulers.add(new Ruler(first, pos));
        state.clearSelection();
        player.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.4F);
        RulerNetwork.sendState(player, state);
    }

    /**
     * Left click: discard the pending selection, or the ruler whose line
     * the player is currently looking at.
     */
    public static void discard(ServerPlayer player, RulerState state) {
        if (state.hasFirst) {
            state.clearSelection();
        } else {
            BlockPos pos = targetedBlock(player);
            Ruler target = null;
            if (pos != null) {
                for (Ruler ruler : state.rulers) {
                    if (RulerMath.lineBetween(ruler.first(), ruler.second()).contains(pos)) {
                        target = ruler;
                        break;
                    }
                }
            }
            if (target == null) {
                return; // not looking at a ruler, nothing to discard
            }
            state.rulers.remove(target);
        }
        player.sendSystemMessage(
                Component.translatable("message.blockruler.discarded").withStyle(ChatFormatting.RED));
        RulerNetwork.sendState(player, state);
    }

    /** Crouch + left click: discard everything. */
    public static void discardAll(ServerPlayer player, RulerState state) {
        if (!state.hasFirst && state.rulers.isEmpty()) {
            return;
        }
        state.clearSelection();
        state.rulers.clear();
        player.sendSystemMessage(
                Component.translatable("message.blockruler.discarded_all").withStyle(ChatFormatting.RED));
        RulerNetwork.sendState(player, state);
    }
}
