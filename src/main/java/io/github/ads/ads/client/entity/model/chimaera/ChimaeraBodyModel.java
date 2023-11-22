package io.github.ads.ads.client.entity.model.chimaera;

import io.github.ads.ads.ads;
import io.github.ads.ads.entity.Basilisk;
import io.github.ads.ads.entity.chimaera.ChimaeraBody;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ChimaeraBodyModel extends DefaultedEntityGeoModel<ChimaeraBody> {

    public ChimaeraBodyModel() {
        super(new ResourceLocation(ads.MODID, "chimaera"), false);
    }

    @Override
    public ResourceLocation getModelResource(ChimaeraBody chimaera) {
        return new ResourceLocation(ads.MODID, "geo/entity/chimaera/chimaera_body.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ChimaeraBody chimaera) {
        return new ResourceLocation(ads.MODID, "textures/entity/chimaera/chimaera_body.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ChimaeraBody chimaera) {
        return new ResourceLocation(ads.MODID, "animations/entity/chimaera/chimaera_body.animation.json");
    }

    @Override
    public void setCustomAnimations(ChimaeraBody animatable, long instanceId, AnimationState<ChimaeraBody> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = this.getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
