package pl.pearltrail;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class PearlTrailClient implements ClientModInitializer {

    private static KeyBinding settingsKey;

    @Override
    public void onInitializeClient() {
        TrailConfig.load();

        // Klawisz otwierający ustawienia (domyślnie O, można zmienić w Opcje -> Sterowanie)
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pearltrail.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.pearltrail"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.wasPressed()) {
                client.setScreen(new SettingsScreen(client.currentScreen));
            }
        });

        ClientTickEvents.END_WORLD_TICK.register(TrailManager::tick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(HighlightRenderer::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TrailManager.clear());
    }
}
