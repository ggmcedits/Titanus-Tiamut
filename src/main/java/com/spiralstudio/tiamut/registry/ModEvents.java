package com.spiralstudio.tiamut.registry;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.spiralstudio.tiamut.TiamutMod;
import com.spiralstudio.tiamut.entity.TiamutEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

public class ModEvents {

    @Mod.EventBusSubscriber(modid = TiamutMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.TIAMUT.get(), TiamutEntity.createAttributes().build());
        }

        @SubscribeEvent
        public static void spawnPlacement(SpawnPlacementRegisterEvent event) {
            event.register(ModEntities.TIAMUT.get(), SpawnPlacements.Type.IN_WATER,
                    Heightmap.Types.OCEAN_FLOOR_WG, TiamutEntity::checkTiamutSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    @Mod.EventBusSubscriber(modid = TiamutMod.MOD_ID)
    public static class ForgeBus {
        /** /tiamut roar <short|long|challenge>  - forces the nearest Tiamut (128 blocks) to roar. OP only. */
        @SubscribeEvent
        public static void commands(RegisterCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("tiamut").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("roar")
                            .then(Commands.argument("type", StringArgumentType.word())
                                    .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"short", "long", "challenge"}, b))
                                    .executes(ctx -> {
                                        CommandSourceStack src = ctx.getSource();
                                        String t = StringArgumentType.getString(ctx, "type");
                                        TiamutEntity.RoarType type = switch (t) {
                                            case "long" -> TiamutEntity.RoarType.LONG;
                                            case "challenge" -> TiamutEntity.RoarType.CHALLENGE;
                                            default -> TiamutEntity.RoarType.SHORT;
                                        };
                                        List<TiamutEntity> list = src.getLevel().getEntitiesOfClass(TiamutEntity.class,
                                                new AABB(src.getPosition(), src.getPosition()).inflate(128));
                                        if (list.isEmpty()) {
                                            src.sendFailure(Component.literal("No Tiamut within 128 blocks."));
                                            return 0;
                                        }
                                        list.sort((a, b) -> Double.compare(a.distanceToSqr(src.getPosition()), b.distanceToSqr(src.getPosition())));
                                        list.get(0).roar(type, true);
                                        return 1;
                                    }))));
        }
    }
}
