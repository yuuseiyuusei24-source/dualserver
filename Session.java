package example.dualserver;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;

/** 1つのサーバー接続が持つクライアント側の状態一式 */
public class Session {
    public final String name;
    public final ClientWorld world;
    public final ClientPlayerEntity player;
    public final ClientPlayerInteractionManager interactionManager;
    public final ClientPlayNetworkHandler handler;

    private Session(String name, ClientWorld w, ClientPlayerEntity p, ClientPlayerInteractionManager im, ClientPlayNetworkHandler h) {
        this.name = name; this.world = w; this.player = p; this.interactionManager = im; this.handler = h;
    }

    public static Session capture(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return null;
        var info = mc.getCurrentServerEntry();
        String n = info != null ? info.address : "singleplayer";
        return new Session(n, mc.world, mc.player, mc.interactionManager, mc.player.networkHandler);
    }

    public boolean isOpen() {
        return handler.getConnection().isOpen();
    }

    /** このセッションをクライアントのアクティブ状態にする */
    public void apply(MinecraftClient mc) {
        mc.world = world;
        mc.player = player;
        mc.interactionManager = interactionManager;
        mc.worldRenderer.setWorld(world);
        mc.particleManager.setWorld(world);
        mc.setCameraEntity(player);
    }
}
