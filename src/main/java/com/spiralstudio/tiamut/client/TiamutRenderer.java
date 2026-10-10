package com.spiralstudio.tiamut.client;

import com.spiralstudio.tiamut.entity.TiamutEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class TiamutRenderer extends GeoEntityRenderer<TiamutEntity> {
    public TiamutRenderer(EntityRendererProvider.Context context) {
        super(context, new TiamutModel());
        this.shadowRadius = 3.0F;
        // eyes + dorsal spine glow, from textures/entity/tiamut_glowmask.png
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
