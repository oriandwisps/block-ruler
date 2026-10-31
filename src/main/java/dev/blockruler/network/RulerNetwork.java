package dev.blockruler.network;

import dev.blockruler.BlockRulerMod;
import dev.blockruler.client.ClientRulerHandler;
import dev.blockruler.ruler.RulerState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BlockRulerMod.MODID)
public final class RulerNetwork {

    private RulerNetwork() {
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(BlockRulerMod.MODID, path);
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(C2SRulerAction.TYPE, C2SRulerAction.CODEC, ServerRulerHandler::handleC2S);
        registrar.playToClient(S2CRulerState.TYPE, S2CRulerState.CODEC, ClientRulerHandler::handleS2C);
    }

    public static void sendState(ServerPlayer player, RulerState state) {
        BlockPos first = state.hasFirst ? new BlockPos(state.firstX, state.firstY, state.firstZ) : BlockPos.ZERO;
        PacketDistributor.sendToPlayer(
                player,
                new S2CRulerState(
                        state.active,
                        state.selectedSlot,
                        state.hasFirst,
                        first,
                        new java.util.ArrayList<>(state.rulers)));
    }
}
