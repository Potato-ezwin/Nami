package me.kiriyaga.nami.core.command;

import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.ChatMessageEvent;

import static me.kiriyaga.nami.Nami.*;

public class CommandExecutor {

    private final CommandStorage storage;
    private String prefix = "-";

    public CommandExecutor(CommandStorage storage) {
        this.storage = storage;
    }

    public void setPrefix(String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            this.prefix = prefix;
            LOGGER.info("Command prefix changed to: " + prefix);
        } else {
            LOGGER.warn("Attempted to set empty or null prefix.");
        }
    }

    public String getPrefix() {
        return prefix;
    }

    @SubscribeEvent
    public void onChatMessage(ChatMessageEvent event) {
        String message = event.getMessage();

        if (!message.startsWith(prefix)) return;

        event.setCancelled(true);

        String[] parts = message.split("\\s+");
        if (parts.length == 0) return;

        String cmdName = parts[0].substring(prefix.length());
        String[] args = new String[0];
        if (parts.length > 1) {
            args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, args.length);
        }

        Command command = storage.getCommandByNameOrAlias(cmdName);
        if (command == null) {
            LOGGER.warn("Unknown command: " + cmdName);
            return;
        }

        try {
            command.execute(args);
        } catch (Exception e) {
            LOGGER.error("Error executing command " + cmdName, e);
        }
    }
}
