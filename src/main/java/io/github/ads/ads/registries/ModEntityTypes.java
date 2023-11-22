package io.github.ads.ads.registries;

import com.mojang.logging.LogUtils;
import io.github.ads.ads.ads;
import io.github.ads.ads.entity.Basilisk;
import io.github.ads.ads.entity.Paleosuchus;
import io.github.ads.ads.entity.chimaera.ChimaeraBody;
import io.github.ads.ads.entity.chimaera.ChimaeraGoat;
import io.github.ads.ads.entity.dragon.IceDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Consumer;

public class ModEntityTypes<T extends Entity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ads.MODID);
    public static final RegistryObject<EntityType<Basilisk>> BASILISK =
            ENTITY_TYPES.register("basilisk",
                    () -> EntityType.Builder.of(Basilisk::new, MobCategory.CREATURE)
                            .sized(0.5f, 0.75f)
                            .build(new ResourceLocation(ads.MODID, "basilisk").toString()));

    public static final RegistryObject<EntityType<IceDragon>> ICE_DRAGON =
            ENTITY_TYPES.register("ice_dragon",
                    () -> EntityType.Builder.of(IceDragon::new, MobCategory.CREATURE)
                            .sized(1.25f, 1.75f)
                            .build(new ResourceLocation(ads.MODID, "ice_dragon").toString()));

    public static final RegistryObject<EntityType<ChimaeraBody>> CHIMAERA_BODY =
            ENTITY_TYPES.register("chimaera_b",
                    () -> EntityType.Builder.of(ChimaeraBody::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.5f)
                            .build(new ResourceLocation(ads.MODID, "chimaera_b").toString()));

    public static final RegistryObject<EntityType<ChimaeraGoat>> GOAT_HEAD =
            ENTITY_TYPES.register("goat_head",
                    () -> EntityType.Builder.of(ChimaeraGoat::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.0f)
                            .build(new ResourceLocation(ads.MODID, "goat_head").toString()));

    public static final RegistryObject<EntityType<Paleosuchus>> PALEOSUCHUS =
            ENTITY_TYPES.register("dwarf_caiman",
                    () -> EntityType.Builder.of(Paleosuchus::new, MobCategory.CREATURE)
                            .sized(0.5f, 0.375f)
                            .build(new ResourceLocation(ads.MODID, "dwarf_caiman").toString()));


}
