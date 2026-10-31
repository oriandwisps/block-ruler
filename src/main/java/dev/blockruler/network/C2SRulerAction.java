package dev.blockruler.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2SRulerAction(int action, int slot) implements CustomPacketPayload {

    public static final int ACTION_TOGGLE = 0;
    public static final int ACTION_SLOT = 1;
    public static final int ACTION_PLACE = 2;
    public static final int ACTION_DISCARD = 3;
    public static final int ACTION_DISCARD_ALL = 4;

    public static final Type<C2SRulerAction> TYPE = new Type<>(RulerNetwork.loc("ruler_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRulerAction> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, C2SRulerAction::action,
            ByteBufCodecs.VAR_INT, C2SRulerAction::slot,
            C2SRulerAction::new);

    public static C2SRulerAction toggle() {
        return new C2SRulerAction(ACTION_TOGGLE, 0);
    }

    public static C2SRulerAction slot(int slot) {
        return new C2SRulerAction(ACTION_SLOT, slot);
    }

    public static C2SRulerAction place() {
        return new C2SRulerAction(ACTION_PLACE, 0);
    }

    public static C2SRulerAction discard() {
        return new C2SRulerAction(ACTION_DISCARD, 0);
    }

    public static C2SRulerAction discardAll() {
        return new C2SRulerAction(ACTION_DISCARD_ALL, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
