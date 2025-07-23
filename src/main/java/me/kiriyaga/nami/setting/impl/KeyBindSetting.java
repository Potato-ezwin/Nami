package me.kiriyaga.nami.setting.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.kiriyaga.nami.setting.Setting;
import org.lwjgl.glfw.GLFW;

import static me.kiriyaga.nami.Nami.MC;

public class KeyBindSetting extends Setting<Integer> {

    public static final int KEY_NONE = -1;
    public static final int MOUSE_RIGHT = 1002; // Custom code for right click
    private boolean wasPressedLastTick = false;
    private boolean holdMode = false;

    public KeyBindSetting(String name, int defaultKey) {
        super(name, defaultKey);
    }

    public boolean isPressed() {
        if (value == KEY_NONE) return false;
        return GLFW.glfwGetKey(MC.getWindow().getHandle(), value) == GLFW.GLFW_PRESS;
    }

    public static String getKeyName(int key) {
        if (key == MOUSE_RIGHT) return "Right Click";
        if (key == KEY_NONE) return "none";
        return GLFW.glfwGetKeyName(key, 0);
    }

    public boolean isHoldMode() {
        return holdMode;
    }

    public void setHoldMode(boolean holdMode) {
        this.holdMode = holdMode;
    }


    public boolean wasPressedLastTick() {
        return wasPressedLastTick;
    }

    public void setWasPressedLastTick(boolean val) {
        this.wasPressedLastTick = val;
    }

    @Override
    public void set(Integer value) {
        this.value = value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            this.value = json.getAsInt();
        } else {
            this.value = KEY_NONE;
        }
    }
}
