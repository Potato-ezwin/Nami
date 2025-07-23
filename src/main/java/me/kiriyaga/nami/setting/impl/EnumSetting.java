package me.kiriyaga.nami.setting.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.kiriyaga.nami.setting.Setting;

public class EnumSetting<T extends Enum<T>> extends Setting<T> {

    private final T[] values;

    public EnumSetting(String name, T defaultValue) {
        super(name, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public void cycle() {
        int index = (value.ordinal() + 1) % values.length;
        value = values[index];
    }

    public T[] getValues() {
        return values;
    }

    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            String name = json.getAsString();
            for (T constant : values) {
                if (constant.name().equalsIgnoreCase(name)) {
                    value = constant;
                    return;
                }
            }
        }
    }

    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }
}
