package me.kiriyaga.nami.core.command;

import static me.kiriyaga.nami.Nami.*;

public class CommandManager {

    private final CommandStorage storage = new CommandStorage();
    private final CommandExecutor executor = new CommandExecutor(storage);

    public void init() {
        CommandRegistry.registerAnnotatedCommands(storage);
        EVENT_MANAGER.register(executor);
        LOGGER.info("Registered " + storage.size() + " commands.");
    }

    public CommandStorage getStorage() {
        return storage;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }
}
