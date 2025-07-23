package me.kiriyaga.nami.core.module;

import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleStorage {

    private final List<Module> modules = new ArrayList<>();

    public void add(Module module) {
        modules.add(module);
    }

    public void remove(Module module) {
        modules.remove(module);
        module.setEnabled(false);
    }

    public List<Module> getAll() {
        return modules;
    }

    public <T extends Module> T getByClass(Class<T> clazz) {
        return modules.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }

    public Module getByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Module> getByCategory(ModuleCategory category) {
        return modules.stream()
                .filter(m -> m.getCategory().equals(category))
                .sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public int size() {
        return modules.size();
    }
}
