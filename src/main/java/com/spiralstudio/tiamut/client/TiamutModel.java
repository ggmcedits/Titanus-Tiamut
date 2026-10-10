package com.spiralstudio.tiamut.client;

import com.spiralstudio.tiamut.TiamutMod;
import com.spiralstudio.tiamut.entity.TiamutEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class TiamutModel extends GeoModel<TiamutEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(TiamutMod.MOD_ID, "geo/tiamut.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(TiamutMod.MOD_ID, "textures/entity/tiamut.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(TiamutMod.MOD_ID, "animations/tiamut.animation.json");

    @Override public ResourceLocation getModelResource(TiamutEntity animatable) { return MODEL; }
    @Override public ResourceLocation getTextureResource(TiamutEntity animatable) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(TiamutEntity animatable) { return ANIMATION; }

    /** Tilts the whole body with the swim pitch (dive / climb) and lets the head look at targets. */
    @Override
    public void setCustomAnimations(TiamutEntity entity, long instanceId, AnimationState<TiamutEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);

        CoreGeoBone root = getAnimationProcessor().getBone("root");
        if (root != null && entity.isInWaterOrBubble()) {
            float pitch = Mth.clamp(Mth.lerp(state.getPartialTick(), entity.xRotO, entity.getXRot()), -45.0F, 45.0F);
            root.setRotX(root.getRotX() + pitch * Mth.DEG_TO_RAD * 0.8F);
        }

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && data != null && !entity.isRoaring()) {
            head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.6F);
        }
    }
}
