package com.spiralstudio.tiamut.registry;

import com.spiralstudio.tiamut.TiamutMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TiamutMod.MOD_ID);

    public static final RegistryObject<Item> TIAMUT_SPAWN_EGG = ITEMS.register("tiamut_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TIAMUT, 0x16303A, 0xE0B020, new Item.Properties()));
}
