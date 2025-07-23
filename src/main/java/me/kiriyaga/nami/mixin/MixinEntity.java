package me.kiriyaga.nami.mixin;

import me.kiriyaga.nami.event.impl.EntityPushEvent;
import me.kiriyaga.nami.feature.module.impl.movement.ElytraFlyModule;
import me.kiriyaga.nami.feature.module.impl.visuals.FreeLookModule;
import me.kiriyaga.nami.feature.module.impl.visuals.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.kiriyaga.nami.Nami.*;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow public abstract boolean isPlayer();

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void updateChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this != MC.player) return;

        FreecamModule freecamModule = MODULE_MANAGER.getStorage().getByClass(FreecamModule.class);
        FreeLookModule freeLookModule = MODULE_MANAGER.getStorage().getByClass(FreeLookModule.class);

        if (freecamModule != null && freecamModule.isEnabled()) {
            freecamModule.changeLookDirection(cursorDeltaX * 0.15, cursorDeltaY * 0.15);
            ci.cancel();
        } else if (freeLookModule != null && freeLookModule.isEnabled()) {
            freeLookModule.cameraYaw += (float) (cursorDeltaX / freeLookModule.sensivity.get().floatValue());
            freeLookModule.cameraPitch += (float) (cursorDeltaY / freeLookModule.sensivity.get().floatValue());

            if (Math.abs(freeLookModule.cameraPitch) > 90.0F)
                freeLookModule.cameraPitch = freeLookModule.cameraPitch > 0.0F ? 90.0F : -90.0F;

            ci.cancel();
        }
    }

    @Inject(method = "pushAwayFrom", at = @At(value = "HEAD"), cancellable = true)
    private void pushAwayFrom(Entity e, CallbackInfo ci) {
        EntityPushEvent pushEntityEvent = new EntityPushEvent((Entity)(Object) this, e);
        EVENT_MANAGER.post(pushEntityEvent);
        if (pushEntityEvent.isCancelled()) ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/Entity;getPose()Lnet/minecraft/entity/EntityPose;", cancellable = true)
    private void entityPose(CallbackInfoReturnable<EntityPose> cir) {
        ElytraFlyModule elytraFlyModule = MODULE_MANAGER.getStorage().getByClass(ElytraFlyModule.class);
        if (elytraFlyModule != null && elytraFlyModule.isEnabled()
                && elytraFlyModule.mode.get() == ElytraFlyModule.FlyMode.bounce
                && (Object) this == MinecraftClient.getInstance().player
                && MC.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            cir.setReturnValue(EntityPose.STANDING);
        }
    }
}
