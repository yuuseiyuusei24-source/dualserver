package example.dualserver;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    /** 裏で接続を維持しているセッション */
    public static final List<Session> parked = new ArrayList<>();
    private static boolean parking = false;

    public static boolean consumeParkingFlag() {
        boolean p = parking; parking = false; return p;
    }

    /** 現在のサーバーを裏に回して、別のサーバーに接続する */
    public static void addServer(MinecraftClient mc, String address) {
        Session cur = Session.capture(mc);
        if (cur == null) { msg(mc, "先にどれかのサーバーに接続してください"); return; }
        parked.add(cur);
        parking = true;
        ServerAddress addr = ServerAddress.parse(address);
        ServerInfo info = new ServerInfo(address, address, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(new TitleScreen(), mc, addr, info, false, null);
    }

    /** disconnect() をキャンセルする代わりに、クライアントの参照だけ外す */
    public static void detachCurrentWithoutClosing() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.world = null;
        mc.player = null;
        mc.interactionManager = null;
    }

    public static void switchTo(MinecraftClient mc, int index) {
        cleanup(mc);
        if (index < 0 || index >= parked.size()) { msg(mc, "そのセッションはありません"); return; }
        Session cur = Session.capture(mc);
        Session target = parked.remove(index);
        if (cur != null) parked.add(index, cur);
        target.apply(mc);
        msg(mc, "切り替え: " + target.name);
    }

    public static void switchNext(MinecraftClient mc) { switchTo(mc, 0); }

    public static void closeParked(MinecraftClient mc, int index) {
        if (index < 0 || index >= parked.size()) return;
        parked.remove(index).handler.getConnection().disconnect(Text.literal("closed"));
    }

    /** 裏のセッションを毎tick動かして、接続（KeepAlive等）を維持する */
    public static void tickParked(MinecraftClient mc) {
        if (parked.isEmpty()) return;
        var world = mc.world; var player = mc.player; var im = mc.interactionManager;
        for (Session s : new ArrayList<>(parked)) {
            if (!s.isOpen()) continue;
            mc.world = s.world; mc.player = s.player; mc.interactionManager = s.interactionManager;
            try {
                s.handler.getConnection().tick();
                s.handler.tick();
            } catch (Exception e) { e.printStackTrace(); }
        }
        mc.world = world; mc.player = player; mc.interactionManager = im;
        cleanup(mc);
    }

    private static void cleanup(MinecraftClient mc) {
        parked.removeIf(s -> {
            boolean dead = !s.isOpen();
            if (dead) msg(mc, "裏のセッションが切断されました: " + s.name);
            return dead;
        });
    }

    public static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[DualServer] " + s), false);
    }
}
