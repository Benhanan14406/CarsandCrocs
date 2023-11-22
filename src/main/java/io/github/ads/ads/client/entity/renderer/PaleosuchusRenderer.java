package io.github.ads.ads.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ads.ads.client.entity.model.PaleosuchusModel;
import io.github.ads.ads.entity.Paleosuchus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;

public class PaleosuchusRenderer extends GeoEntityRenderer<Paleosuchus> {
    public PaleosuchusRenderer(EntityRendererProvider.Context context) {
        super(context, new PaleosuchusModel());
    }

    public RenderType getRenderType(Paleosuchus animatable, float ticks, PoseStack stack,
                                    @Nullable MultiBufferSource bufferSource,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation texture) {
        stack.scale(1.0f, 1.0f, 1.0f);

        return super.getRenderType(animatable, texture, bufferSource, ticks);
    }

    @Override
    public void postRender(PoseStack poseStack, Paleosuchus animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

    }
}