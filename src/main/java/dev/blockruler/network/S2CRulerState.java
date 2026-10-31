package dev.blockruler.network;

import dev.blockruler.ruler.Ruler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * The server's authoritative ruler state, pushed to the client whenever it
 * changes: mode flag, pending first point and the full list of rulers
 * (which persist even outside ruler mode).
 */
public record S2CRulerState(
        boolean active,
        int selectedSlot,
        boolean hasFirst,
        BlockPos first,
        List<Ruler> rulers) implements CustomPacketPayload {

    public static final Type<S2CRulerState> TYPE = new Type<>(RulerNetwork.loc("ruler_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRulerState> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, S2CRulerState::active,
            ByteBufCodecs.VAR_INT, S2CRulerState::selectedSlot,
            ByteBufCodecs.BOOL, S2CRulerState::hasFirst,
            BlockPos.STREAM_CODEC, S2CRulerState::first,
            Ruler.LIST_CODEC, S2CRulerState::rulers,
            S2CRulerState::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
