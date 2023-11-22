package io.github.ads.ads.client.entity.model.chimaera;

import io.github.ads.ads.ads;
import io.github.ads.ads.entity.chimaera.ChimaeraBody;
import io.github.ads.ads.entity.chimaera.ChimaeraGoat;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class ChimaeraGoatModel extends DefaultedEntityGeoModel<ChimaeraGoat> {

    public ChimaeraGoatModel() {
        super(new ResourceLocation(ads.MODID, "chimaera"), true);
    }

    @Override
    public ResourceLocation getModelResource(ChimaeraGoat chimaera) {
        return new ResourceLocation(ads.MODID, "geo/entity/chimaera/chimaera_goat.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ChimaeraGoat chimaera) {
        return new ResourceLocation(ads.MODID, "textures/entity/chimaera/chimaera_goat.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ChimaeraGoat chimaera) {
        return new ResourceLocation(ads.MODID, "animations/entity/chimaera/chimaera_goat.animation.json");
    }

    @Override
    public void setCustomAnimations(ChimaeraGoat animatable, long instanceId, AnimationState<ChimaeraGoat> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = this.getAnimationProcessor().getBone("head");

        head.setScaleX(1.1F);
        head.setScaleY(1.1F);
        head.setScaleZ(1.1F);
    }
}
