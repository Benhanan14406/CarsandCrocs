package io.github.ads.ads.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ads.ads.client.entity.model.IceDModel1;
import io.github.ads.ads.entity.dragon.IceDragon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class IceDRenderer extends GeoEntityRenderer<IceDragon> {
    public IceDRenderer(EntityRendererProvider.Context context) {
        super(context, new IceDModel1());
    }

    public RenderType getRenderType(IceDragon animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void postRender(PoseStack poseStack, IceDragon animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

    }
}