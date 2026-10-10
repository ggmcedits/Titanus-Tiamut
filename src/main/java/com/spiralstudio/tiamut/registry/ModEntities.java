package com.spiralstudio.tiamut.registry;

import com.spiralstudio.tiamut.TiamutMod;
import com.spiralstudio.tiamut.entity.TiamutEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TiamutMod.MOD_ID);

    public static final RegistryObject<EntityType<TiamutEntity>> TIAMUT = ENTITIES.register("tiamut",
            () -> EntityType.Builder.<TiamutEntity>of(TiamutEntity::new, MobCategory.WATER_CREATURE)
                    .sized(3.2F, 3.0F)
                    .clientTrackingRange(24)
                    .updateInterval(2)
                    .build(new ResourceLocation(TiamutMod.MOD_ID, "tiamut").toString()));
}
