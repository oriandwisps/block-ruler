package dev.blockruler.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blockruler.BlockRulerMod;
import dev.blockruler.ruler.Ruler;
import dev.blockruler.ruler.RulerMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * Renders the block count of every ruler as floating text in the world.
 * The label sits above the ruler's block closest to the viewer, so it
 * follows the player along the line without leaving the selection.
 */
@EventBusSubscriber(modid = BlockRulerMod.MODID, value = Dist.CLIENT)
public final class RulerTextRenderer {

    private RulerTextRenderer() {
    }

    private static final float TEXT_SCALE = 0.025F;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        List<Ruler> rulers = ClientRulerState.INSTANCE.rulers;
        if (rulers.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        for (Ruler ruler : rulers) {
            List<BlockPos> line = RulerMath.lineBetween(ruler.first(), ruler.second());
            BlockPos anchor = RulerMath.closestBlock(line, player.blockPosition());

            int count = RulerMath.blockCount(ruler.first(), ruler.second());
            String label = RulerMath.describe(count);

            double dist = Math.sqrt(anchor.distToCenterSqr(camX, camY, camZ));

            poseStack.pushPose();
            poseStack.translate(anchor.getX() + 0.5 - camX, anchor.getY() + 1.6 - camY, anchor.getZ() + 0.5 - camZ);
            // Face the camera, flip so the text is not mirrored, then
            // scale with distance like an entity name tag.
            poseStack.mulPose(camera.rotation());
            float scale = TEXT_SCALE * Math.max(1.0F, (float) dist / 8.0F);
            poseStack.scale(scale, -scale, scale);

            float halfWidth = font.width(label) / 2.0F;
            font.drawInBatch(
                    label,
                    -halfWidth, 0,
                    0x00E5E5,
                    true,
                    poseStack.last().pose(),
                    buffers,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    0xF000F0);
            poseStack.popPose();
        }

        buffers.endBatch();
    }
}
