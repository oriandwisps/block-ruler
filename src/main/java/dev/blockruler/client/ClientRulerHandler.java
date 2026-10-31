package dev.blockruler.client;

import dev.blockruler.network.S2CRulerState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side handling of ruler state updates pushed by the server.
 */
public final class ClientRulerHandler {

    private ClientRulerHandler() {
    }

    public static void handleS2C(S2CRulerState payload, IPayloadContext context) {
        ClientRulerState.apply(payload);
    }
}
