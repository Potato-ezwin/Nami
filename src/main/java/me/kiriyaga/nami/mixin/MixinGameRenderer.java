package me.kiriyaga.nami.mixin;

import me.kiriyaga.nami.feature.module.impl.visuals.FreecamModule;
import me.kiriyaga.nami.feature.module.impl.visuals.NoRenderModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void updateCrosshairTarget(float tickDelta);

    @Shadow
    public abstract void reset();

    @Shadow
    @Final
    private Camera camera;

    @Unique
    private final MatrixStack matrices = new MatrixStack();

    @Shadow
    protected abstract void bobView(MatrixStack matrices, float tickDelta);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);


    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (MODULE_MANAGER.getStorage() == null) return;

        NoRenderModule noRender = MODULE_MANAGER.getStorage().getByClass(NoRenderModule.class);
        if (noRender != null && floatingItem.getItem() == Items.TOTEM_OF_UNDYING && noRender.isEnabled() && noRender.isNoTotem()) {
            info.cancel();
        }
    }

    @Unique
    private boolean freecamSet = false;

    @Inject(method = "updateCrosshairTarget", at = @At("HEAD"), cancellable = true)
    private void updateTargetedEntityInvoke(float tickDelta, CallbackInfo info) {
        if (MODULE_MANAGER.getStorage() == null) return;

        FreecamModule freecamModule = MODULE_MANAGER.getStorage().getByClass(FreecamModule.class);
        if (freecamModule == null || !freecamModule.isEnabled()) return;

        if (client == null) return;

        if (client.getCameraEntity() != null && !freecamSet) {
            info.cancel();

            Entity cameraE = client.getCameraEntity();

            double x = cameraE.getX();
            double y = cameraE.getY();
            double z = cameraE.getZ();
            double lastX = cameraE.lastX;
            double lastY = cameraE.lastY;
            double lastZ = cameraE.lastZ;
            float yaw = cameraE.getYaw();
            float pitch = cameraE.getPitch();
            float lastYaw = cameraE.lastYaw;
            float lastPitch = cameraE.lastPitch;

            cameraE.setPos(freecamModule.getX(), freecamModule.getY() - cameraE.getEyeHeight(cameraE.getPose()), freecamModule.getZ());

            cameraE.lastX = freecamModule.prevPos.x;
            cameraE.lastY = freecamModule.prevPos.y - cameraE.getEyeHeight(cameraE.getPose());
            cameraE.lastZ = freecamModule.prevPos.z;

            cameraE.setYaw(freecamModule.yaw);
            cameraE.setPitch(freecamModule.pitch);
            cameraE.lastYaw = freecamModule.lastYaw;
            cameraE.lastPitch = freecamModule.lastPitch;

            freecamSet = true;
            updateCrosshairTarget(tickDelta);
            freecamSet = false;

            cameraE.setPos(x, y, z);
            cameraE.lastX = lastX;
            cameraE.lastY = lastY;
            cameraE.lastZ = lastZ;
            cameraE.setYaw(yaw);
            cameraE.setPitch(pitch);
            cameraE.lastYaw = lastYaw;
            cameraE.lastPitch = lastPitch;
        }
    }
}