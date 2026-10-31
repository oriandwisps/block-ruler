package dev.blockruler;

import dev.blockruler.item.RulerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BlockRulerMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlockRulerMod.MODID);

    public static final DeferredItem<Item> RULER = ITEMS.registerItem("block_ruler",
            props -> new RulerItem(props.stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.blockruler"))
                    .icon(() -> RULER.get().getDefaultInstance())
                    .displayItems((params, output) -> output.accept(RULER.get()))
                    .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
