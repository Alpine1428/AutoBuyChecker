package com.autobuy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHandler {
    private static boolean enabled = false;
    private static int state = 0;
    private static String currentNick = null;
    private static String lastProcessedNick = null;
    private static long lastProcessedTime = 0;

    private static final Pattern BUY_PATTERN = Pattern.compile("^.{0,5}\\s*(\\S+)\\s+купил[аи]?\\s+у\\s+вас\\s+\\[.+?\\]\\s+x\\d+\\s+за\\s+[\\d\\s,.]+[¤$€₽]?");
    private static final Pattern FIND_PATTERN = Pattern.compile("Игрок\\s+\\S+\\s+находится\\s+на\\s+сервере\\s+(\\S+)");
    private static final Pattern L2ANARCHY_PATTERN = Pattern.compile("^l2anarchy(\\d*)$");
    private static final Pattern LANARCHY_PATTERN = Pattern.compile("^lanarchy(\\d*)$");
    private static final Pattern ANARCHY_PATTERN = Pattern.compile("^anarchy(\\d*)$");

    public static void enable() { enabled = true; state = 0; sendClientMessage("§a[AutoBuy] Включен!"); }
    public static void disable() { enabled = false; state = 0; sendClientMessage("§c[AutoBuy] Выключен!"); }
    
    public static void sendStatus() {
        sendClientMessage("§e[AutoBuy] Статус: " + (enabled ? "§aВкл" : "§cВыкл") + " | Стейт: " + state);
    }

    public static void onChatMessage(String rawMessage) {
        if (!enabled || rawMessage == null) return;
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        if (state == 0) {
            Matcher m = BUY_PATTERN.matcher(clean);
            if (m.find()) {
                String nick = m.group(1);
                long now = System.currentTimeMillis();
                if (nick.equals(lastProcessedNick) && (now - lastProcessedTime) < 5000) return;
                lastProcessedNick = nick; lastProcessedTime = now; currentNick = nick;
                sendClientMessage("§e[AutoBuy] Покупка от: §b" + nick);
                scheduleCmd("/hm spy " + nick, 300, () -> {
                    state = 1;
                    scheduleCmd("/find " + nick, 1200, null);
                });
            }
        } else if (state == 1) {
            Matcher m = FIND_PATTERN.matcher(clean);
            if (m.find()) {
                String srv = m.group(1).trim().toLowerCase();
                String cmd = buildCmd(srv);
                if (cmd != null) {
                    sendClientMessage("§a[AutoBuy] Переход: §b" + cmd);
                    scheduleCmd(cmd, 500, null);
                }
                state = 0; currentNick = null;
            }
        }
    }

    private static String buildCmd(String srv) {
        Matcher m;
        if ((m = L2ANARCHY_PATTERN.matcher(srv)).matches()) return "/ln120 " + (m.group(1).isEmpty() ? "1" : m.group(1));
        if ((m = LANARCHY_PATTERN.matcher(srv)).matches()) return "/ln " + (m.group(1).isEmpty() ? "1" : m.group(1));
        if ((m = ANARCHY_PATTERN.matcher(srv)).matches()) return "/cn " + (m.group(1).isEmpty() ? "1" : m.group(1));
        return null;
    }

    public static void sendClientMessage(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) mc.execute(() -> mc.player.sendMessage(Text.literal(msg), false));
    }

    private static void scheduleCmd(String cmd, long ms, Runnable cb) {
        new Thread(() -> {
            try { Thread.sleep(ms); } catch (Exception ignored) {}
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) mc.execute(() -> {
                if(cmd.startsWith("/")) mc.getNetworkHandler().sendCommand(cmd.substring(1));
                else mc.getNetworkHandler().sendChatMessage(cmd);
            });
            if (cb != null) {
                try { Thread.sleep(400); } catch (Exception ignored) {}
                if (mc != null) mc.execute(cb);
            }
        }).start();
    }
}
