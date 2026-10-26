package example.dualserver;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DualServerClient implements ClientModInitializer {
    private static KeyBinding switchKey;

    @Override
    public void onInitializeClient() {
        // 1.21.9以降は KeyBinding のカテゴリが Category 型。下記で通らない場合は
        // KeyBinding.Category.create(Identifier.of("dualserver","main")) を使ってください。
        switchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dualserver.switch", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8,
                KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            SessionManager.tickParked(mc);
            while (switchKey.wasPressed()) SessionManager.switchNext(mc);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("dualserver")
                .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("address", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            SessionManager.addServer(MinecraftClient.getInstance(),
                                StringArgumentType.getString(ctx, "address"));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("switch")
                    .executes(ctx -> { SessionManager.switchNext(MinecraftClient.getInstance()); return 1; })
                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            SessionManager.switchTo(MinecraftClient.getInstance(),
                                IntegerArgumentType.getInteger(ctx, "index"));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("close")
                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            SessionManager.closeParked(MinecraftClient.getInstance(),
                                IntegerArgumentType.getInteger(ctx, "index"));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("list")
                    .executes(ctx -> {
                        var mc = MinecraftClient.getInstance();
                        if (SessionManager.parked.isEmpty()) SessionManager.msg(mc, "裏のセッションなし");
                        for (int i = 0; i < SessionManager.parked.size(); i++)
                            SessionManager.msg(mc, "[" + i + "] " + SessionManager.parked.get(i).name);
                        return 1;
                    }))));
    }
}
