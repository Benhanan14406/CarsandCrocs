package io.github.ads.ads.client.entity.model;

import io.github.ads.ads.ads;
import io.github.ads.ads.entity.Basilisk;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class BasiliskModel extends DefaultedEntityGeoModel<Basilisk> {

    public BasiliskModel() {
        super(new ResourceLocation(ads.MODID, "basilisk"), true);
    }

    @Override
    public ResourceLocation getModelResource(Basilisk basilisk) {
        return new ResourceLocation(ads.MODID, "geo/basilisk.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Basilisk basilisk) {
        if (basilisk.getVariant() == Basilisk.Type.GUINEAFOWL) {
            if (!basilisk.isBaby()) {
                if (basilisk.isMale()) {
                    return new ResourceLocation(ads.MODID, "textures/entity/basilisk/malebasilisk2.png");
                } else {
                    return new ResourceLocation(ads.MODID, "textures/entity/basilisk/femalebasilisk2.png");
                }
            } else {
                return new ResourceLocation(ads.MODID, "textures/entity/basilisk/femalebasilisk2.png");
            }
        } else {
            if (!basilisk.isBaby()) {
                if (basilisk.isMale()) {
                    return new ResourceLocation(ads.MODID, "textures/entity/basilisk/malebasilisk1.png");
                } else {
                    return new ResourceLocation(ads.MODID, "textures/entity/basilisk/femalebasilisk1.png");
                }
            } else {
                return new ResourceLocation(ads.MODID, "textures/entity/basilisk/femalebasilisk1.png");
            }
        }
    }

    @Override
    public ResourceLocation getAnimationResource(Basilisk basilisk) {
        return new ResourceLocation(ads.MODID, "animations/entity/basilisk.animation.json");
    }

    @Override
    public void setCustomAnimations(Basilisk animatable, long instanceId, AnimationState<Basilisk> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone body = this.getAnimationProcessor().getBone("body");
        CoreGeoBone neck = this.getAnimationProcessor().getBone("neck");
        CoreGeoBone rightthigh = this.getAnimationProcessor().getBone("rightthigh");
        CoreGeoBone leftthigh = this.getAnimationProcessor().getBone("leftthigh");
        CoreGeoBone tail2 = this.getAnimationProcessor().getBone("bone4");
        CoreGeoBone tail3= this.getAnimationProcessor().getBone("bone5");
        CoreGeoBone tail4= this.getAnimationProcessor().getBone("bone6");

        if (animatable.getVariant() == Basilisk.Type.JUNGLEFOWL) {
            neck.setScaleX(1.125f);
            neck.setScaleY(1.125f);
            neck.setScaleZ(1.125f);
        } else {
            neck.setScaleX(1.0f);
            neck.setScaleY(1.0f);
            neck.setScaleZ(1.0f);
        }

        if (animatable.getVariant() == Basilisk.Type.GUINEAFOWL) {
            rightthigh.setScaleY(1.125f);
            leftthigh.setScaleY(1.125f);
            body.setPosY(1.1275f);
        } else {
            rightthigh.setScaleY(1.0f);
            leftthigh.setScaleY(1.0f);
            body.setPosY(1.0f);
        }
    }
}
