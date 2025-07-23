package me.kiriyaga.nami.mixin;

import me.kiriyaga.nami.event.impl.BlockPushEvent;
import me.kiriyaga.nami.event.impl.MoveEvent;
import me.kiriyaga.nami.event.impl.PostTickEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static me.kiriyaga.nami.Nami.*;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {

    @Shadow private float lastYawClient;
    @Shadow private float lastPitchClient;

    @Shadow
    protected MinecraftClient client;

    @Shadow public abstract void move(MovementType type, Vec3d movement);

    private float originalYaw, originalPitch;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickHookPre(CallbackInfo ci) {

        EVENT_MANAGER.post(new PreTickEvent());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickHookPost(CallbackInfo ci) {

        EVENT_MANAGER.post(new PostTickEvent());
    }

    @Inject(method = "pushOutOfBlocks", at = @At(value = "HEAD"), cancellable = true)
    private void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        BlockPushEvent pushOutOfBlocksEvent = new BlockPushEvent();
        EVENT_MANAGER.post(pushOutOfBlocksEvent);

        if (pushOutOfBlocksEvent.isCancelled())
            ci.cancel();
    }

    @Inject(method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void onMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        MoveEvent moveEvent = new MoveEvent(movementType, movement);
        EVENT_MANAGER.post(moveEvent);

        if (moveEvent.isCancelled()) {
            ci.cancel();
            return;
        }

        Vec3d newMovement = moveEvent.getMovement();
        if (!newMovement.equals(movement)) {
            this.move(movementType, newMovement);
            ci.cancel();
        }
    }



    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void preSendMovementPackets(CallbackInfo ci) {
        if (!ROTATION_MANAGER.isRotating())
            return;

        originalYaw = MC.player.getYaw();
        originalPitch = MC.player.getPitch();

        MC.player.setYaw(ROTATION_MANAGER.getRotationYaw());
        MC.player.setPitch(ROTATION_MANAGER.getRotationPitch());

        MC.player.setBodyYaw(ROTATION_MANAGER.getRotationYaw());
        MC.player.setHeadYaw(ROTATION_MANAGER.getRotationYaw());
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void postSendMovementPackets(CallbackInfo ci) {
        if (!ROTATION_MANAGER.isRotating())
            return;

        MC.player.setYaw(originalYaw);
        MC.player.setPitch(originalPitch);
    }
}
