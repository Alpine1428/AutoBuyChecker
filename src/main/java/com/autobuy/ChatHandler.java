package com.autobuy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHandler {
    private static boolean enabled = false;
    private static int state = 0;
    private static String lastProcessedNick = null;
    private static long lastProcessedTime = 0;

    private static final Pattern BUY_PATTERN = Pattern.compile("(\\S+)\\s+купил[аи]?\\s+у\\s+вас\\s+\\[.+?\\]\\s+x\\d+\\s+за\\s+[\\d\\s,.]+[¤$€₽]?");
    private static final Pattern FIND_PATTERN = Pattern.compile("Игрок\\s+\\S+\\s+находится\\s+на\\s+сервере\\s+(\\S+)");
    private static final Pattern L2ANARCHY_PATTERN = Pattern.compile("^l2anarchy(\\d*)$");
    private static final Pattern LANARCHY_PATTERN = Pattern.compile("^lanarchy(\\d*)$");
    private static final Pattern ANARCHY_PATTERN = Pattern.compile("^anarchy(\\d*)$");

    public static void enable() { 
        enabled = true; 
        state = 0; 
        sendClientMessage("§a[AutoBuy] Включен! Ожидаю покупки..."); 
    }
    
    public static void disable() { 
        enabled = false; 
        state = 0; 
        sendClientMessage("§c[AutoBuy] Выключен!"); 
    }

    public static void onChatMessage(String rawMessage) {
        if (!enabled || rawMessage == null) return;
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        // 1. Обработка сообщения о покупке
        Matcher mBuy = BUY_PATTERN.matcher(clean);
        if (mBuy.find()) {
            String nick = mBuy.group(1);
            long now = System.currentTimeMillis();
            
            if (nick.equals(lastProcessedNick) && (now - lastProcessedTime) < 5000) return;
            lastProcessedNick = nick; 
            lastProcessedTime = now;
            
            // --- КОПИРОВАНИЕ В БУФЕР ОБМЕНА ---
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.keyboard != null) {
                mc.execute(() -> mc.keyboard.setClipboard(nick));
            }
            
            sendClientMessage("§e[AutoBuy] Покупка от: §b" + nick + " §7(Ник скопирован)");
            
            // Запускаем цепочку команд
            scheduleCmd("/hm spy " + nick, 300, () -> {
                state = 1; 
                scheduleCmd("/find " + nick, 1200, () -> {
                    new Thread(() -> {
                        try { Thread.sleep(5000); } catch (Exception ignored) {}
                        if (state == 1) state = 0;
                    }).start();
                });
            });
            return;
        }

        // 2. Обработка ответа от /find
        if (state == 1) {
            Matcher mFind = FIND_PATTERN.matcher(clean);
            if (mFind.find()) {
                String srv = mFind.group(1).trim().toLowerCase();
                String cmd = buildCmd(srv);
                if (cmd != null) {
                    sendClientMessage("§a[AutoBuy] Переход: §b" + cmd);
                    scheduleCmd(cmd, 500, null);
                }
                state = 0;
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
            if (mc != null && mc.player != null && cmd != null && !cmd.isEmpty()) {
                mc.execute(() -> {
                    if(cmd.startsWith("/")) mc.getNetworkHandler().sendCommand(cmd.substring(1));
                    else mc.getNetworkHandler().sendChatMessage(cmd);
                });
            }
            if (cb != null) {
                try { Thread.sleep(400); } catch (Exception ignored) {}
                if (mc != null) mc.execute(cb);
            }
        }).start();
    }
}
