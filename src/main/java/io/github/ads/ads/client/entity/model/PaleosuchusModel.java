package io.github.ads.ads.client.entity.model;

import io.github.ads.ads.ads;
import io.github.ads.ads.entity.Paleosuchus;
import io.github.ads.ads.entity.chimaera.ChimaeraGoat;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class PaleosuchusModel extends DefaultedEntityGeoModel<Paleosuchus> {

    public PaleosuchusModel() {
        super(new ResourceLocation(ads.MODID, "paleosuchus"),true);
    }

    @Override
    public ResourceLocation getModelResource(Paleosuchus paleosuchus) {
        return new ResourceLocation(ads.MODID, "geo/entity/paleosuchus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Paleosuchus paleosuchus) {
        return new ResourceLocation(ads.MODID, "textures/entity/paleosuchus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Paleosuchus paleosuchus) {
        return new ResourceLocation(ads.MODID, "animations/entity/paleosuchus.animation.json");
    }

    @Override
    public void setCustomAnimations(Paleosuchus animatable, long instanceId, AnimationState<Paleosuchus> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone body = this.getAnimationProcessor().getBone("body");
        CoreGeoBone head = this.getAnimationProcessor().getBone("head");

        body.setScaleX(0.875F);
        body.setScaleY(0.875F);
        body.setScaleZ(0.875F);

        if (animatable.baskingProgress > 0.0F && animatable.isOnGround() && animatable.getDeltaMovement().horizontalDistanceSqr() < 1.0E-6) {
            head.setRotX(0.0F);
            head.setRotY(0.0F);
            body.setRotX(0.0F);
            body.setRotY(0.0F);
        }

        if (animatable.isBaby()) {
            head.setScaleX(1.5F);
            head.setScaleY(1.5F);
            head.setScaleZ(1.5F);
        }
    }
}
