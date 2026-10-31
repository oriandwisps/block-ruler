package dev.blockruler;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(BlockRulerMod.MODID)
public class BlockRulerMod {

    public static final String MODID = "blockruler";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlockRulerMod(IEventBus modBus, ModContainer modContainer) {
        ModRegistry.register(modBus);
        LOGGER.info("Block Ruler loaded");
    }
}

