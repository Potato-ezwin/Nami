package me.kiriyaga.nami.core;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.impl.client.RotationManagerModule;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.function.Supplier;

import static me.kiriyaga.nami.Nami.*;

public class RotationManager {
    private final List<RotationRequest> requests = new ArrayList<>();
    private RotationRequest activeRequest = null;

    private float realYaw, realPitch;
    private float rotationYaw, rotationPitch;
    private float currentYawSpeed = 0f;
    private float currentPitchSpeed = 0f;

    private float rotationSpeed;
    private float rotationEaseFactor;
    private float rotationThreshold;
    private int ticksBeforeRelease;
    private float jitterAmount;
    private float jitterSpeed;

    private int tickCount = 0;
    private boolean returning = false;
    private int ticksHolding = 0;

    public void init() {
        EVENT_MANAGER.register(this);
    }

    public void updateRealRotation(float yaw, float pitch) {
        realYaw = wrapDegrees(yaw);
        realPitch = MathHelper.clamp(pitch, -90f, 90f);
    }

    public void submitRequest(RotationRequest request) {
        boolean wasActive = activeRequest != null && Objects.equals(activeRequest.id, request.id);

        requests.removeIf(r -> Objects.equals(r.id, request.id));
        requests.add(request);
        requests.sort(Comparator.comparingInt(r -> -r.priority));

        if (wasActive) {
            activeRequest = request;
        }
    }

    public boolean hasRequest(String id) {
        if (activeRequest != null && Objects.equals(activeRequest.id, id)) {
            return true;
        }

        for (RotationRequest r : requests) {
            if (Objects.equals(r.id, id)) {
                return true;
            }
        }

        return false;
    }


    public boolean isRequestCompleted(String id) {
        RotationRequest request = null;

        if (activeRequest != null && activeRequest.id.equals(id)) {
            request = activeRequest;
        } else {
            for (RotationRequest r : requests) {
                if (r.id.equals(id)) {
                    request = r;
                    break;
                }
            }
        }

        if (request == null) {
            return false;
        }

        float yawDiff = wrapDegrees(request.targetYaw - rotationYaw);
        float pitchDiff = request.targetPitch - rotationPitch;

        return Math.abs(yawDiff) <= rotationThreshold && Math.abs(pitchDiff) <= rotationThreshold;
    }


    public void cancelRequest(String id) {
        requests.removeIf(r -> Objects.equals(r.id, id));
    }

    public boolean isRotating() {
        return activeRequest != null || returning;
    }


    public float getRotationYaw() {
        return rotationYaw;
    }

    public float getRotationPitch() {
        return rotationPitch;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;

        RotationManagerModule rotationModule = MODULE_MANAGER.getStorage().getByClass(RotationManagerModule.class);
        if (rotationModule == null) return;
        
        rotationSpeed = rotationModule.rotationSpeed.get().floatValue();
        rotationEaseFactor = rotationModule.rotationEaseFactor.get().floatValue();
        rotationThreshold = rotationModule.rotationThreshold.get().floatValue();
        ticksBeforeRelease = rotationModule.ticksBeforeRelease.get();
        jitterAmount = rotationModule.jitterAmount.get().floatValue();
        jitterSpeed = rotationModule.jitterSpeed.get().floatValue();

        updateRealRotation(MC.player.getYaw(), MC.player.getPitch());

        if (!requests.isEmpty()) {
            if (activeRequest == null || !requests.contains(activeRequest)) {
                activeRequest = requests.get(0);
                rotationYaw = realYaw;
                rotationPitch = realPitch;
                ticksHolding = 0;
                currentYawSpeed = 0f;
                currentPitchSpeed = 0f;
                returning = false;
            }

            boolean updated = false;
            if (activeRequest.shouldUpdate()) {
                float oldYaw = activeRequest.targetYaw;
                float oldPitch = activeRequest.targetPitch;
                activeRequest.updateTarget();
                if (Math.abs(wrapDegrees(oldYaw - activeRequest.targetYaw)) > 0.001f ||
                        Math.abs(oldPitch - activeRequest.targetPitch) > 0.001f) {
                    ticksHolding = 0;
                    updated = true;
                }
            }

            float yawDiff = wrapDegrees(activeRequest.targetYaw - rotationYaw);
            float pitchDiff = activeRequest.targetPitch - rotationPitch;

            boolean reached = Math.abs(yawDiff) <= rotationThreshold && Math.abs(pitchDiff) <= rotationThreshold;

            if (reached && !updated) {
                ticksHolding++;
                if (ticksHolding >= ticksBeforeRelease) {
                    requests.remove(activeRequest);
                    activeRequest = null;
                    ticksHolding = 0;
                    returning = true;
                }
            } else {
                ticksHolding = 0;

                currentYawSpeed = lerp(currentYawSpeed, yawDiff, rotationEaseFactor);
                currentPitchSpeed = lerp(currentPitchSpeed, pitchDiff, rotationEaseFactor);

                float clampedYawSpeed = MathHelper.clamp(currentYawSpeed, -rotationSpeed, rotationSpeed);
                float clampedPitchSpeed = MathHelper.clamp(currentPitchSpeed, -rotationSpeed, rotationSpeed);

                rotationYaw = wrapDegrees(rotationYaw + clampedYawSpeed);
                rotationPitch = MathHelper.clamp(rotationPitch + clampedPitchSpeed, -90f, 90f);
            }
        } else if (returning) {
            float yawDiff = wrapDegrees(realYaw - rotationYaw);
            float pitchDiff = realPitch - rotationPitch;

            currentYawSpeed = lerp(currentYawSpeed, yawDiff, rotationEaseFactor);
            currentPitchSpeed = lerp(currentPitchSpeed, pitchDiff, rotationEaseFactor);

            float clampedYawSpeed = MathHelper.clamp(currentYawSpeed, -rotationSpeed, rotationSpeed);
            float clampedPitchSpeed = MathHelper.clamp(currentPitchSpeed, -rotationSpeed, rotationSpeed);

            if (Math.abs(yawDiff) <= rotationThreshold) {
                rotationYaw = realYaw;
                currentYawSpeed = 0f;
            } else {
                rotationYaw = wrapDegrees(rotationYaw + clampedYawSpeed);
            }

            if (Math.abs(pitchDiff) <= rotationThreshold) {
                rotationPitch = realPitch;
                currentPitchSpeed = 0f;
            } else {
                rotationPitch = MathHelper.clamp(rotationPitch + clampedPitchSpeed, -90f, 90f);
            }

            boolean backReached = Math.abs(yawDiff) <= rotationThreshold && Math.abs(pitchDiff) <= rotationThreshold;

            if (backReached) {
                returning = false;
                rotationYaw = realYaw;
                rotationPitch = realPitch;
            }
        } else {
            rotationYaw = realYaw;
            rotationPitch = realPitch;
            currentYawSpeed = 0f;
            currentPitchSpeed = 0f;
        }

        if (isRotating() && !returning) {
            float jitterYawOffset = jitterAmount * (float) Math.sin(tickCount * jitterSpeed);
            float jitterPitchOffset = jitterYawOffset / 5f;

            rotationYaw = wrapDegrees(rotationYaw + jitterYawOffset);
            rotationPitch = MathHelper.clamp(rotationPitch + jitterPitchOffset, -90f, 90f);
        }
    }

    private float wrapDegrees(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    private float lerp(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    public static class RotationRequest {
        public final String id;
        public final int priority;
        private final boolean dynamic;
        private final Supplier<Float> yawSupplier;
        private final Supplier<Float> pitchSupplier;

        public float targetYaw;
        public float targetPitch;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RotationRequest)) return false;
            RotationRequest other = (RotationRequest) o;
            return this.id.equals(other.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public RotationRequest(String id, int priority, float yaw, float pitch) {
            this.id = id;
            this.priority = priority;
            this.dynamic = false;
            this.targetYaw = yaw;
            this.targetPitch = pitch;
            this.yawSupplier = null;
            this.pitchSupplier = null;
        }

        public RotationRequest(String id, int priority, Supplier<Float> yawSupplier, Supplier<Float> pitchSupplier) {
            this.id = id;
            this.priority = priority;
            this.dynamic = true;
            this.yawSupplier = yawSupplier;
            this.pitchSupplier = pitchSupplier;
            updateTarget();
        }

        public boolean shouldUpdate() {
            return dynamic;
        }

        public void updateTarget() {
            if (dynamic && yawSupplier != null && pitchSupplier != null) {
                targetYaw = yawSupplier.get();
                targetPitch = pitchSupplier.get();
            }
        }
    }
}
