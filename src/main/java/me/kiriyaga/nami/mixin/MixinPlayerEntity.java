package me.kiriyaga.nami.mixin;

import me.kiriyaga.nami.event.impl.LedgeClipEvent;
import me.kiriyaga.nami.event.impl.LiquidPushEvent;
import me.kiriyaga.nami.event.impl.SprintResetEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.kiriyaga.nami.Nami.EVENT_MANAGER;
import static me.kiriyaga.nami.Nami.MC;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity {
    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "clipAtLedge", at = @At(value = "HEAD"), cancellable = true)
    private void clipAtLedge(CallbackInfoReturnable<Boolean> cir) {
        LedgeClipEvent ledgeClipEvent = new LedgeClipEvent();
        EVENT_MANAGER.post(ledgeClipEvent);
        
        if (ledgeClipEvent.isCancelled())
            cir.setReturnValue(ledgeClipEvent.getClipped());
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void attack(PlayerEntity playerEntity, Vec3d movementInput) {
        if (playerEntity instanceof ClientPlayerEntity) {
            SprintResetEvent sprintResetEvent = new SprintResetEvent();
            EVENT_MANAGER.post(sprintResetEvent);
            if (!sprintResetEvent.isCancelled())
                MC.player.setVelocity(MC.player.getVelocity().multiply(0.6, 1.0, 0.6));
        }
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"))
    private void attack2(PlayerEntity playerEntity, boolean liv) {
        if (playerEntity instanceof ClientPlayerEntity) {
            SprintResetEvent sprintResetEvent = new SprintResetEvent();
            EVENT_MANAGER.post(sprintResetEvent);
            if (!sprintResetEvent.isCancelled())
                MC.player.setSprinting(false);

        }
    }


    @Inject(method = "isPushedByFluids", at = @At(value = "HEAD"),
            cancellable = true)
    private void isPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != MC.player)
            return;

        LiquidPushEvent pushFluidsEvent = new LiquidPushEvent();
        EVENT_MANAGER.post(pushFluidsEvent);
        if (pushFluidsEvent.isCancelled())
        {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}