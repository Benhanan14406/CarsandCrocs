package io.github.ads.ads.registries;

import io.github.ads.ads.ads;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS  =
            DeferredRegister.create(ForgeRegistries.ITEMS, ads.MODID);

    public static final RegistryObject<ForgeSpawnEggItem> BASILISK_SPAWN_EGG =
            ITEMS.register("basilisk_spawn_egg", () -> new ForgeSpawnEggItem(ModEntityTypes.BASILISK, 3815473, 12954734, props().stacksTo(64)));

    public static final RegistryObject<ForgeSpawnEggItem> CHIMAERA_SPAWN_EGG =
            ITEMS.register("chimaera_spawn_egg", () -> new ForgeSpawnEggItem(ModEntityTypes.GOAT_HEAD, 4010022, 9854549, props().stacksTo(64)));

    private static Item.Properties props() {
        return new Item.Properties();
    }
}

