package dev.blockruler.item;

import dev.blockruler.ruler.RulerLogic;
import dev.blockruler.ruler.RulerState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Block Ruler item. Right-clicking places measurement points; the item
 * is only a regular item when found outside ruler mode (creative tab etc.).
 * In ruler mode the hotbar is an overlay, so points are normally placed via
 * the right-click interception in the client handler.
 */
public class RulerItem extends Item {

    public RulerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            RulerState state = RulerState.get(RulerState.SERVER, serverPlayer.getUUID());
            if (state.active) {
                RulerLogic.placePoint(serverPlayer, state);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.blockruler.ruler").withStyle(ChatFormatting.GRAY));
    }
}
