package me.kiriyaga.nami.mixin;

import me.kiriyaga.nami.feature.module.impl.visuals.NoRenderModule;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    public void noBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (MODULE_MANAGER.getStorage() == null) return;

        NoRenderModule m = MODULE_MANAGER.getStorage().getByClass(NoRenderModule.class);

        if (m != null && m.isEnabled() && m.isNoBackground()) {
            ci.cancel();
        }
    }
}

