package io.github.ads.ads.client;

import io.github.ads.ads.Config;
import io.github.ads.ads.ads;
import io.github.ads.ads.entity.dragon.AbstractDragon;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final KeyMapping WALK_FORWARD_KEY = keymap("walk_forward", GLFW.GLFW_KEY_W, "key.categories.movement");
    public static final KeyMapping FLIGHT_DESCENT_KEY = keymap("flight_descent", GLFW.GLFW_KEY_X, "key.categories.movement");
    public static final KeyMapping FLIGHT_ASCENT_KEY = keymap("flight_ascent", GLFW.GLFW_KEY_SPACE, "key.categories.movement");
    public static final KeyMapping CAMERA_CONTROLS = keymap("camera_flight", GLFW.GLFW_KEY_F6, "key.categories.movement");
    public static final KeyMapping DRAGON_BREATH_KEY = keymap("dragon_breath", GLFW.GLFW_KEY_R, "key.categories.gameplay");
    public static final KeyMapping MOUNT_ATTACK_KEY = keymap("mount_attack", GLFW.GLFW_KEY_G, "key.categories.gameplay");
    public static final KeyMapping MOUNT_ROAR_KEY = keymap("mount_roar", GLFW.GLFW_KEY_T, "key.categories.gameplay");

    @SuppressWarnings({"ConstantConditions"})
    private static KeyMapping keymap(String name, int defaultMapping, String category) {
        var keymap = new KeyMapping(String.format("key.%s.%s", ads.MODID, name), defaultMapping, category);
        return keymap;
    }

    public static void handleKeyPress(int key, int action) {
        if (key == CAMERA_CONTROLS.getKey().getValue() && action == GLFW.GLFW_PRESS && Minecraft.getInstance().player.getVehicle() instanceof AbstractDragon) {
            Config.cameraFlight = !Config.cameraFlight();
        }

        if (key == FLIGHT_ASCENT_KEY.getKey().getValue() && action == GLFW.GLFW_PRESS && Minecraft.getInstance().player.getVehicle() instanceof AbstractDragon) {

        }
    }
}