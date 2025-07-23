package me.kiriyaga.nami.core;

import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.mixin.ChatHudAccessor;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static me.kiriyaga.nami.Nami.*;

public class ChatManager {

    /**
     * Usage:
     * .sendRaw() just sends raw message without tracking
     *
     * .sendPersistent() non timed/replaced by default message, but is tracked and we can delete it by its key
     * Why? Because we want to track session joins/leaves, for example, and clean "outdated" info, but without
     * affecting other messages. Can be replaced by key.
     *
     * .removePersistent() all by its name
     *
     * .sendTransient() transient message, that will be deleted and replaced while new transient message appears     *
     *
     * .clear() clears all messages (contained by us btw)
     *
     */

    private final Map<String, MessageSignatureData> persistentMessages = new HashMap<>();
    private MessageSignatureData transientSignature = null;

    private MessageSignatureData generateSignature() {
        byte[] data = new byte[256];
        new SecureRandom().nextBytes(data);
        return new MessageSignatureData(data);
    }

    public void sendRaw(String message) {
        sendRaw(Text.literal(message), true);
    }

    public void sendRaw(String message, boolean prefix) {
        sendRaw(Text.literal(message), prefix);
    }

    public void sendRaw(Text message) {
        sendRaw(message, true);
    }

    public void sendRaw(Text message, boolean prefix) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;
        Text text = prefix ? prefix().copy().append(message) : message;
        getChatHud().addMessage(text);
    }

    public void sendPersistent(String key, String message) {
        sendPersistent(key, Text.literal(message), true);
    }

    public void sendPersistent(String key, String message, boolean prefix) {
        sendPersistent(key, Text.literal(message), prefix);
    }

    public void sendPersistent(String key, Text message) {
        sendPersistent(key, message, true);
    }

    public void sendPersistent(String key, Text message, boolean prefix) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        ChatHud chatHud = getChatHud();

        if (persistentMessages.containsKey(key)) {
            removeSilently(persistentMessages.get(key));
        }

        Text text = prefix ? prefix().copy().append(message) : message;
        MessageSignatureData signature = generateSignature();
        MessageIndicator indicator = MessageIndicator.system();

        chatHud.addMessage(text, signature, indicator);
        persistentMessages.put(key, signature);
    }

    public void sendTransient(String message) {
        sendTransient(Text.literal(message), true);
    }

    public void sendTransient(String message, boolean prefix) {
        sendTransient(Text.literal(message), prefix);
    }

    public void sendTransient(Text message) {
        sendTransient(message, true);
    }

    public void sendTransient(Text message, boolean prefix) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        ChatHud chatHud = getChatHud();

        if (transientSignature != null) {
            removeSilently(transientSignature);
            transientSignature = null;
        }

        Text text = prefix ? prefix().copy().append(message) : message;
        MessageSignatureData signature = generateSignature();
        MessageIndicator indicator = MessageIndicator.system();

        chatHud.addMessage(text, signature, indicator);
        transientSignature = signature;
    }

    public void removePersistent(String key) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        if (persistentMessages.containsKey(key)) {
            removeSilently(persistentMessages.get(key));
            persistentMessages.remove(key);
        }
    }

    public void clear() {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        ChatHud chatHud = getChatHud();
        for (MessageSignatureData sig : persistentMessages.values()) {
            chatHud.removeMessage(sig);
            removeSilently(sig);
        }
        persistentMessages.clear();

        if (transientSignature != null) {
            removeSilently(transientSignature);
            transientSignature = null;
        }
    }

    private ChatHud getChatHud() {
        return MC.inGameHud.getChatHud();
    }

    public void removeSilently(MessageSignatureData signature) {
        if (MC == null || MC.inGameHud == null || getChatHud() == null) return;

        ChatHud hud = MC.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) hud;

        accessor.getMessages().removeIf(line -> signature.equals(line.signature()));

        accessor.getVisibleMessages().removeIf(visible -> {
            for (ChatHudLine line : accessor.getMessages()) {
                if (signature.equals(line.signature())) {
                    return visible.content().equals(line.content());
                }
            }
            return false;
        });
        accessor.callRefresh();
    }

    private Text prefix() {
        int global = MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledGlobalColor().getRGB() & 0x00FFFFFF;
        int brac = MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledSecondColor().getRGB() & 0x00FFFFFF;

        Text left = Text.literal("[").setStyle(Style.EMPTY.withColor(brac));
        Text name = Text.literal(NAME).setStyle(Style.EMPTY.withColor(global));
        Text right = Text.literal("] ").setStyle(Style.EMPTY.withColor(brac));

        return Text.empty().append(left).append(name).append(right);
    }
}
