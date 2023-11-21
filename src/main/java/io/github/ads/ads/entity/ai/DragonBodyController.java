package io.github.ads.ads.entity.ai;

import io.github.ads.ads.entity.dragon.AbstractDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

public class DragonBodyController extends BodyRotationControl
{
    private final AbstractDragon dragon;

    public DragonBodyController(AbstractDragon dragon)
    {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    public void clientTick()
    {
        dragon.yBodyRot = dragon.getYRot();

        dragon.yHeadRot = Mth.rotateIfNecessary(dragon.yHeadRot, dragon.yBodyRot, dragon.getMaxHeadYRot());
    }
}