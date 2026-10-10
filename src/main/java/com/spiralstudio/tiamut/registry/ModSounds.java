package com.spiralstudio.tiamut.registry;

import com.spiralstudio.tiamut.TiamutMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TiamutMod.MOD_ID);

    public static final RegistryObject<SoundEvent> ROAR_SHORT = reg("roar_short");
    public static final RegistryObject<SoundEvent> ROAR_LONG = reg("roar_long");
    public static final RegistryObject<SoundEvent> ROAR_CHALLENGE = reg("roar_challenge");
    public static final RegistryObject<SoundEvent> BITE = reg("bite");
    public static final RegistryObject<SoundEvent> HURT = reg("hurt");
    public static final RegistryObject<SoundEvent> DEATH = reg("death");
    public static final RegistryObject<SoundEvent> AMBIENT = reg("ambient");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TiamutMod.MOD_ID, name)));
    }
}
