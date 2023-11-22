package io.github.ads.ads;

import io.github.ads.ads.client.entity.renderer.BasiliskRenderer;
import io.github.ads.ads.client.entity.renderer.IceDRenderer;
import io.github.ads.ads.client.entity.renderer.PaleosuchusRenderer;
import io.github.ads.ads.client.entity.renderer.chimaera.ChimaeraBodyRenderer;
import io.github.ads.ads.client.entity.renderer.chimaera.ChimaeraGoatRenderer;
import io.github.ads.ads.entity.Basilisk;
import io.github.ads.ads.entity.Paleosuchus;
import io.github.ads.ads.entity.chimaera.ChimaeraBody;
import io.github.ads.ads.entity.chimaera.ChimaeraGoat;
import io.github.ads.ads.entity.dragon.IceDragon;
import io.github.ads.ads.registries.ModEntityTypes;
import io.github.ads.ads.registries.ModItems;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

@Mod(io.github.ads.ads.ads.MODID)
public class ads {
    public static final String MODID = "ads";

    public ads() {
        GeckoLib.initialize();
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::onAttributeCreation);
        modEventBus.addListener(this::registerEntityRenderers);

    }

    private void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.BASILISK.get(), Basilisk.createAttributes().build());
        event.put(ModEntityTypes.ICE_DRAGON.get(), IceDragon.createAttributes().build());
        event.put(ModEntityTypes.CHIMAERA_BODY.get(), ChimaeraBody.createAttributes().build());
        event.put(ModEntityTypes.GOAT_HEAD.get(), ChimaeraGoat.createAttributes().build());
        event.put(ModEntityTypes.PALEOSUCHUS.get(), Paleosuchus.createAttributes().build());

    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.BASILISK.get(), BasiliskRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ICE_DRAGON.get(), IceDRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CHIMAERA_BODY.get(), ChimaeraBodyRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.GOAT_HEAD.get(), ChimaeraGoatRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.PALEOSUCHUS.get(), PaleosuchusRenderer::new);

    }
}
