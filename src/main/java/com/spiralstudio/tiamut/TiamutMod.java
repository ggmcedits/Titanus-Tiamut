package com.spiralstudio.tiamut;

import com.spiralstudio.tiamut.registry.ModEntities;
import com.spiralstudio.tiamut.registry.ModItems;
import com.spiralstudio.tiamut.registry.ModSounds;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TiamutMod.MOD_ID)
public class TiamutMod {
    public static final String MOD_ID = "tiamut";

    public TiamutMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.TIAMUT_SPAWN_EGG);
        }
    }
}
