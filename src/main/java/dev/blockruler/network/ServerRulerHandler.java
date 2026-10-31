package dev.blockruler.network;

import dev.blockruler.ruler.RulerLogic;
import dev.blockruler.ruler.RulerState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side handling of client ruler actions: toggling ruler mode,
 * changing the selected ruler hotbar slot, placing points and discarding.
 */
public final class ServerRulerHandler {

    private ServerRulerHandler() {
    }

    public static void handleC2S(C2SRulerAction payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        RulerState state = RulerState.get(RulerState.SERVER, player.getUUID());

        switch (payload.action()) {
            case C2SRulerAction.ACTION_TOGGLE -> RulerLogic.toggle(player, state);
            case C2SRulerAction.ACTION_SLOT -> {
                if (!state.active) return;
                int slot = payload.slot();
                if (slot >= 0 && slot < 9) {
                    state.selectedSlot = slot;
                }
                RulerNetwork.sendState(player, state);
            }
            case C2SRulerAction.ACTION_PLACE -> {
                if (!state.active) return;
                RulerLogic.placePoint(player, state);
            }
            case C2SRulerAction.ACTION_DISCARD -> {
                if (!state.active) return;
                RulerLogic.discard(player, state);
            }
            case C2SRulerAction.ACTION_DISCARD_ALL -> {
                if (!state.active) return;
                RulerLogic.discardAll(player, state);
            }
            default -> { }
        }
    }
}
