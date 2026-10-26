package example.dualserver.mixin;

import example.dualserver.SessionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    /** 2つ目のサーバーへ接続する際、現在の接続を切断させない */
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"), cancellable = true)
    private void dualserver$keepAlive(Screen screen, boolean transferring, CallbackInfo ci) {
        if (SessionManager.consumeParkingFlag()) {
            SessionManager.detachCurrentWithoutClosing();
            ci.cancel();
        }
    }
}
