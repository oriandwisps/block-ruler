package dev.blockruler.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blockruler.BlockRulerMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The ruler mode keybind, bound to Right Ctrl by default and rebindable
 * in the vanilla controls menu.
 */
@EventBusSubscriber(modid = BlockRulerMod.MODID, value = Dist.CLIENT)
public final class ModKeybindings {

    private ModKeybindings() {
    }

    public static final KeyMapping RULER_MODE = new KeyMapping(
            "key.blockruler.ruler_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_CONTROL,
            "key.categories.blockruler");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(RULER_MODE);
    }

    /**
     * Called from a client input event: if the ruler mode key was pressed,
     * send the toggle packet to the server.
     */
    public static void checkKeybinds() {
        while (RULER_MODE.consumeClick()) {
            ClientRulerEvents.sendToggle();
        }
    }

    /**
     * While a screen is not open, the number keys 1-9 change the ruler hotbar
     * selection; this mirrors vanilla hotbar behaviour for our overlay.
     */
    public static void checkHotbarKeys(Minecraft mc) {
        if (mc.player == null || mc.screen != null) {
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (mc.options.keyHotbarSlots[slot].consumeClick()) {
                ClientRulerEvents.sendSlot(slot);
            }
        }
    }
}
