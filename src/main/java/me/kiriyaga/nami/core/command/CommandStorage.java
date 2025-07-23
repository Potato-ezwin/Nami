package me.kiriyaga.nami.core.command;

import me.kiriyaga.nami.feature.command.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandStorage {

    private final List<Command> commands = new ArrayList<>();

    public void addCommand(Command command) {
        commands.add(command);
    }

    public void removeCommand(Command command) {
        commands.remove(command);
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public Command getCommandByNameOrAlias(String name) {
        String lowerName = name.toLowerCase();
        for (Command cmd : commands) {
            if (cmd.getName().equalsIgnoreCase(lowerName)) return cmd;
            for (String alias : cmd.getAliases()) {
                if (alias.equalsIgnoreCase(lowerName)) return cmd;
            }
        }
        return null;
    }

    public int size() {
        return commands.size();
    }
}
