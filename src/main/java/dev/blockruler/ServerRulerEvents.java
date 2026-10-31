package dev.blockruler;

import dev.blockruler.network.RulerNetwork;
import dev.blockruler.ruler.RulerState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Server-side lifecycle handling for ruler mode state.
 */
@EventBusSubscriber(modid = BlockRulerMod.MODID)
public final class ServerRulerEvents {

    private ServerRulerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RulerState state = RulerState.get(RulerState.SERVER, player.getUUID());
            // Fresh session: ruler mode starts off; sync so the client mirror is clean.
            state.active = false;
            state.clearSelection();
            state.rulers.clear();
            RulerNetwork.sendState(player, state);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Drop the per-player state; no sync needed since the player is gone.
        RulerState.SERVER.remove(event.getEntity().getUUID());
    }
}
