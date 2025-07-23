package me.kiriyaga.nami.util;

public class ChatAnimationHelper {
    private static volatile float animationOffset = 0f;

    public static void setAnimationOffset(float offset) {
        animationOffset = offset;
    }

    public static float getAnimationOffset() {
        return animationOffset;
    }
}
