package dev.blockruler.ruler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * One completed measurement: two axis-aligned block positions and the line
 * of blocks between them. Rulers persist until explicitly discarded.
 */
public record Ruler(BlockPos first, BlockPos second) {

    public static final StreamCodec<RegistryFriendlyByteBuf, Ruler> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Ruler::first,
            BlockPos.STREAM_CODEC, Ruler::second,
            Ruler::new);

    /**
     * Length-prefixed list of rulers, capped at the ruler limit so a
     * malicious packet cannot request a huge allocation.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, List<Ruler>> LIST_CODEC =
            new StreamCodec<>() {
                @Override
                public List<Ruler> decode(RegistryFriendlyByteBuf buf) {
                    int size = Math.min(buf.readVarInt(), 64);
                    List<Ruler> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        list.add(STREAM_CODEC.decode(buf));
                    }
                    return list;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, List<Ruler> list) {
                    buf.writeVarInt(list.size());
                    for (Ruler ruler : list) {
                        STREAM_CODEC.encode(buf, ruler);
                    }
                }
            };
}
