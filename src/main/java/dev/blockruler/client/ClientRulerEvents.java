package dev.blockruler.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.blockruler.BlockRulerMod;
import dev.blockruler.network.C2SRulerAction;
import dev.blockruler.ruler.Ruler;
import dev.blockruler.ruler.RulerMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;

/**
 * All client-side ruler mode behaviour: the keybind, the cyan hotbar overlay,
 * locking the inventory and hotbar while active, input interception, the
 * particle ribs and the look-target outline feedback.
 */
@EventBusSubscriber(modid = BlockRulerMod.MODID, value = Dist.CLIENT)
public final class ClientRulerEvents {

    private ClientRulerEvents() {
    }

    private static final Vector3f CYAN = new Vector3f(0.0F, 1.0F, 1.0F);
    private static final Vector3f WHITE = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final Vector3f GREEN = new Vector3f(0.3F, 1.0F, 0.3F);
    private static final Vector3f RED = new Vector3f(1.0F, 0.3F, 0.3F);

    private static final DustParticleOptions CYAN_DUST = new DustParticleOptions(CYAN, 1.0F);
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(WHITE, 1.0F);
    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(GREEN, 1.0F);
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(RED, 1.0F);

    private static final int HOTBAR_SLOTS = 9;

    // Ruler ribs: spawned frequently, living briefly so the outline stays
    // crisp and re-draws itself continuously.
    private static final int TICKS_PER_RIB_BURST = 2;
    private static final int RIB_PARTICLE_LIFETIME = 4;

    // Look-target outline: refreshed every tick, even shorter lifetime.
    private static final int TARGET_PARTICLE_LIFETIME = 3;

    // ---- Keybinds (Pre fires before vanilla handleKeybinds, so our
    // consumeClick eats the presses first and the real hotbar is locked) ----

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        ModKeybindings.checkKeybinds();
        if (ClientRulerState.isActive()) {
            ModKeybindings.checkHotbarKeys(mc);
        }
    }

    // ---- Mouse input interception ----

    @SubscribeEvent
    public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientRulerState.isActive()) {
            return;
        }

        if (event.isAttack()) {
            // Left click discards the pending selection (crouch: everything).
            event.setCanceled(true);
            event.setSwingHand(false);
            if (mc.player.isCrouching()) {
                sendDiscardAll();
            } else {
                sendDiscard();
            }
        } else if (event.isUseItem()) {
            // Right-click places measurement points instead of using the
            // real held item.
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                sendPlace();
            }
        } else {
            // Block pick-block while the ruler hotbar is up.
            event.setCanceled(true);
        }
    }

    // ---- Inventory lock ----

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!ClientRulerState.isActive()) {
            return;
        }
        // While ruler mode is on the player may not open any inventory-like screen.
        if (event.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
            event.setCanceled(true);
        }
    }

    // ---- Hotbar scroll lock ----

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen == null || !ClientRulerState.isActive()) {
            return;
        }
        // In ruler mode, scrolling must not touch the real hotbar.
        if (event.getScrollDeltaY() != 0) {
            event.setCanceled(true);
        }
    }

    // ---- Cyan hotbar overlay ----

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            return;
        }
        if (!ClientRulerState.isActive()) {
            return;
        }
        // Hide the vanilla hotbar; our cyan one is drawn after the layer.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHotbarPost(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            return;
        }
        if (!ClientRulerState.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        renderCyanHotbar(event.getGuiGraphics(), mc, ClientRulerState.INSTANCE.selectedSlot);
    }

    private static void renderCyanHotbar(GuiGraphics gui, Minecraft mc, int selectedSlot) {
        int width = gui.guiWidth();
        int height = gui.guiHeight();

        // Same geometry as the vanilla hotbar: 182x22, slots 20px wide,
        // 16x16 item area inside each slot.
        int x = (width - 182) / 2;
        int y = height - 22;

        RenderSystem.enableBlend();

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int slotX = x + 1 + i * 20;
            int slotY = y + 3;
            // Cyan tinted slot background with a dark interior, like a
            // recoloured vanilla hotbar slot.
            gui.fill(slotX, slotY, slotX + 18, slotY + 18, 0xA8000000);
            gui.renderOutline(slotX, slotY, 18, 18, 0xFF00E5E5);
        }

        // Slot contents: only slot 0, the Block Ruler.
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            var stack = ClientRulerState.rulerHotbarStack(i);
            if (!stack.isEmpty()) {
                gui.renderItem(stack, x + 3 + i * 20, y + 3);
            }
        }

        // Selection frame around the selected slot.
        int selX = x + selectedSlot * 20 - 1;
        int selY = y - 1;
        gui.fill(selX, selY, selX + 24, selY + 23, 0x4000FFFF);
        gui.renderOutline(selX, selY, 24, 23, 0xFF00FFFF);

        RenderSystem.disableBlend();
    }

    // ---- Ruler-mode HUD text (bottom left) ----

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientRulerState.isActive()) {
            return;
        }
        GuiGraphics gui = event.getGuiGraphics();
        int x = 4;
        int y = gui.guiHeight() - 34;
        gui.drawString(
                Minecraft.getInstance().font,
                Component.translatable("hud.blockruler.title"),
                x, y, 0xFF00E5E5, true);
        gui.drawString(
                Minecraft.getInstance().font,
                Component.translatable("hud.blockruler.hint"),
                x, y + 10, 0xFFFFFFFF, true);
    }

    // ---- Particles ----

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        // Particle ribs render for every ruler, even outside ruler mode,
        // since the rulers persist.
        if (player.level().getGameTime() % TICKS_PER_RIB_BURST == 0) {
            for (Ruler ruler : ClientRulerState.INSTANCE.rulers) {
                for (BlockPos pos : RulerMath.lineBetween(ruler.first(), ruler.second())) {
                    if (mc.level.isLoaded(pos)) {
                        spawnBlockRibs(mc, pos, CYAN_DUST);
                    }
                }
            }
        }

        // Look-target outline feedback while measuring.
        if (ClientRulerState.isActive()) {
            spawnTargetOutline(mc);
        }
    }

    /**
     * Outline the block the player is looking at:
     * - no first point yet: white (candidate)
     * - the block is part of a finished ruler: white
     * - first point placed, and the block is a valid second point: green
     * - first point placed, but the block is diagonal: red
     */
    private static void spawnTargetOutline(Minecraft mc) {
        LocalPlayer player = mc.player;
        BlockHitResult hit = Item.getPlayerPOVHitResult(mc.level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.MISS) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        var state = ClientRulerState.INSTANCE;

        boolean onExistingRuler = false;
        for (Ruler ruler : state.rulers) {
            if (RulerMath.lineBetween(ruler.first(), ruler.second()).contains(pos)) {
                onExistingRuler = true;
                break;
            }
        }

        DustParticleOptions color;
        if (state.hasFirst && !onExistingRuler) {
            BlockPos first = state.first;
            if (RulerMath.isAxisAligned(first, pos)) {
                color = GREEN_DUST;
            } else {
                color = RED_DUST;
            }
        } else {
            // Candidate for a fresh first point, or already part of a ruler.
            color = WHITE_DUST;
        }

        spawnBlockRibs(mc, pos, color);
    }

    /**
     * Spawn small dust particles tightly along all 12 edges (ribs) of the
     * block, forming a glowing wireframe cube. Particles are short-lived so
     * the outline constantly re-draws itself.
     */
    private static void spawnBlockRibs(Minecraft mc, BlockPos pos, DustParticleOptions options) {
        var level = mc.level;
        var engine = mc.particleEngine;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        int lifetime = options == CYAN_DUST ? RIB_PARTICLE_LIFETIME : TARGET_PARTICLE_LIFETIME;

        // 4 particles per edge, 12 edges => 48 particles per block.
        final int steps = 4;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            // Edges along X (4): vary x, fixed y/z at the 4 corners
            for (int cy = 0; cy <= 1; cy++) {
                for (int cz = 0; cz <= 1; cz++) {
                    if (i == steps && cz == 1) continue; // avoid double corner
                    addParticle(engine, level, options, lifetime, x + t, y + cy, z + cz);
                }
            }
            // Edges along Y (4): vary y, fixed x/z
            for (int cx = 0; cx <= 1; cx++) {
                for (int cz = 0; cz <= 1; cz++) {
                    if (i == steps && (cz == 1 || cx == 1)) continue;
                    addParticle(engine, level, options, lifetime, x + cx, y + t, z + cz);
                }
            }
            // Edges along Z (4): vary z, fixed x/y
            for (int cx = 0; cx <= 1; cx++) {
                for (int cy = 0; cy <= 1; cy++) {
                    if (i == steps && (cy == 1 || cx == 1)) continue;
                    addParticle(engine, level, options, lifetime, x + cx, y + cy, z + t);
                }
            }
        }
    }

    private static void addParticle(
            ParticleEngine engine,
            ClientLevel level,
            DustParticleOptions options,
            int lifetime,
            double x, double y, double z) {
        Particle particle = engine.createParticle(options, x, y, z, 0, 0, 0);
        if (particle != null) {
            particle.setLifetime(lifetime);
        }
    }

    // ---- Networking helpers ----

    public static void sendToggle() {
        PacketDistributor.sendToServer(C2SRulerAction.toggle());
    }

    public static void sendSlot(int slot) {
        PacketDistributor.sendToServer(C2SRulerAction.slot(slot));
    }

    public static void sendPlace() {
        PacketDistributor.sendToServer(C2SRulerAction.place());
    }

    public static void sendDiscard() {
        PacketDistributor.sendToServer(C2SRulerAction.discard());
    }

    public static void sendDiscardAll() {
        PacketDistributor.sendToServer(C2SRulerAction.discardAll());
    }
}
