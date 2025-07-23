package me.kiriyaga.nami.core;

import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.feature.module.impl.client.PingManagerModule;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

import static me.kiriyaga.nami.Nami.*;

// since theres is only S2C speaking, and no C2S response for us, all we can do it count between S2C requests
// The idea is, to calculate the S2C request, and based on that - recieve the ms delay
public class PingManager {
    private volatile long lastReceiveTime = -1;
    private volatile int lastPing = -1;

    private int[] pingHistory = new int[10];
    private int index = 0;
    private int count = 0;
    private volatile long lastUpdated = -1;

    public void init() {
        EVENT_MANAGER.register(this);
        LOGGER.info("Ping Manager loaded");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPacketReceive(PacketReceiveEvent packet) {
        if (packet.getPacket() instanceof KeepAliveS2CPacket) {
            long now = System.currentTimeMillis();

            PingManagerModule config = MODULE_MANAGER.getStorage().getByClass(PingManagerModule.class);
            int keepAliveInterval = config != null ? config.keepAliveInterval.get() : 1000;

            if (lastReceiveTime != -1) {
                long interval = now - lastReceiveTime;
                int ping = (int) Math.max(0, interval - keepAliveInterval);

                int smoothingStrength = config != null ? config.smoothingStrength.get() : 10;
                if (pingHistory.length != smoothingStrength) {
                    int[] newHistory = new int[smoothingStrength];
                    for (int i = 0; i < Math.min(count, smoothingStrength); i++) {
                        newHistory[i] = pingHistory[(index - count + i + pingHistory.length) % pingHistory.length];
                    }
                    pingHistory = newHistory;
                    count = Math.min(count, smoothingStrength);
                    index = count % smoothingStrength;
                }

                pingHistory[index++ % smoothingStrength] = ping;
                count = Math.min(count + 1, smoothingStrength);

                lastPing = averagePing();
                lastUpdated = now;

                if (config != null && config.debug.get()) {
                    CHAT_MANAGER.sendRaw("Interval=" + interval + "ms, Ping=" + ping + "ms, Average=" + lastPing + "ms");
                }
            }

            lastReceiveTime = now;
        }
    }

    private int averagePing() {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            sum += pingHistory[i];
        }
        return count == 0 ? -1 : sum / count;
    }

    public int getPing() {
        return lastPing;
    }

    public boolean isConnectionUnstable() {
        PingManagerModule config = MODULE_MANAGER.getStorage().getByClass(PingManagerModule.class);
        if (config == null) return false;

        int timeoutMillis = config.unstableConnectionTimeout.get() * 1000;
        if (lastUpdated == -1) {
            if (config.debug.get())
                CHAT_MANAGER.sendRaw("Connection unstable: no ping data yet");
            return true;
        }
        boolean unstable = (System.currentTimeMillis() - lastUpdated) > timeoutMillis;
        if (unstable && config.debug.get()) {
            CHAT_MANAGER.sendRaw("Connection unstable: last ping updated " + (System.currentTimeMillis() - lastUpdated) + "ms ago");
        }
        return unstable;
    }
    public float getConnectionUnstableTimeSeconds() {
        if (lastUpdated == -1) return Float.POSITIVE_INFINITY;
        long deltaMillis = System.currentTimeMillis() - lastUpdated;
        return deltaMillis / 1000.0f;
    }

}
