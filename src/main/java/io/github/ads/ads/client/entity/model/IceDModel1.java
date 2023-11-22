package io.github.ads.ads.client.entity.model;

import io.github.ads.ads.ads;
import io.github.ads.ads.client.Keybinds;
import io.github.ads.ads.entity.dragon.IceDragon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class IceDModel1 extends DefaultedEntityGeoModel<IceDragon> {

    public IceDModel1() {
        super(new ResourceLocation(ads.MODID, "iced"), false);
    }

    @Override
    public ResourceLocation getModelResource(IceDragon dragon) {
        return new ResourceLocation(ads.MODID, "geo/entity/iced.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(IceDragon dragon) {
        if (!dragon.isBaby()) {
            if (dragon.isMale()) {
                return new ResourceLocation(ads.MODID, "textures/entity/iced/maleiced1.png");
            } else {
                return new ResourceLocation(ads.MODID, "textures/entity/iced/maleiced1.png");
            }
        } else {
            return new ResourceLocation(ads.MODID, "textures/entity/iced/maleiced1.png");
        }
    }

    @Override
    public ResourceLocation getAnimationResource(IceDragon dragon) {
        return new ResourceLocation(ads.MODID, "animations/entity/iced.animation.json");
    }

    @Override
    public void setCustomAnimations(IceDragon animatable, long instanceId, AnimationState<IceDragon> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone body = this.getAnimationProcessor().getBone("body");
        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        CoreGeoBone tail = this.getAnimationProcessor().getBone("tail");
        CoreGeoBone tail2 = this.getAnimationProcessor().getBone("tail2");
        CoreGeoBone tail3 = this.getAnimationProcessor().getBone("tail3");
        CoreGeoBone neck3 = this.getAnimationProcessor().getBone("neck");
        CoreGeoBone neck4 = this.getAnimationProcessor().getBone("neck2");

        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        if (head != null && neck3 != null && neck4 != null) {
            head.setRotX(entityData.headPitch() * ((float) Math.PI / 420F));
            head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 420F));

            neck3.setRotX(entityData.headPitch() * ((float) Math.PI / 420F));
            neck3.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 420F));

            neck4.setRotX(entityData.headPitch() * ((float) Math.PI / 420F));
            neck4.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 420F));
        }

        if (body.getPivotY() > 0F) {
            tail.setRotY(body.getPivotY());
            tail2.setRotY(body.getPivotY());
            tail3.setRotY(body.getPivotY());
        }

        if (animatable.getDragonStage() < 4) {
            body.setScaleX(0.02f * animatable.getAgeInDays());
            body.setScaleY(0.02f * animatable.getAgeInDays());
            body.setScaleZ(0.02f * animatable.getAgeInDays());
        } else {
            body.setScaleX(0.0275f * animatable.getAgeInDays());
            body.setScaleY(0.0275f * animatable.getAgeInDays());
            body.setScaleZ(0.0275f * animatable.getAgeInDays());
        }

        if (animatable.isFlying() && animatable.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6){
            if (Keybinds.FLIGHT_ASCENT_KEY.isDown()) {
                body.setRotX((animatable.getXRot() * (float) Math.PI / 180F));
            } else if (Keybinds.FLIGHT_DESCENT_KEY.isDown()) {
                body.setRotX((animatable.getXRot() * -(float) Math.PI / 180F));
            }
        }
    }
}