package me.kiriyaga.nami.mixin;

import me.kiriyaga.nami.Nami;
import me.kiriyaga.nami.event.impl.BreakBlockEvent;
import me.kiriyaga.nami.event.impl.PlaceBlockEvent;
import me.kiriyaga.nami.event.impl.StartBreakingBlockEvent;
import me.kiriyaga.nami.feature.module.impl.world.AntiInteractModule;
import me.kiriyaga.nami.feature.module.impl.world.NoBreakDelayModule;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.kiriyaga.nami.Nami.EVENT_MANAGER;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager{
    @Shadow
    private int blockBreakingCooldown;


    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> call){
        StartBreakingBlockEvent ev = new StartBreakingBlockEvent(blockPos, direction);
        EVENT_MANAGER.post(ev);

        if (ev.isCancelled())
            call.cancel();
    }

    @Inject(method = "interactBlock", at = @At(value = "HEAD"), cancellable = true)
    private void interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        PlaceBlockEvent interactBlockEvent = new PlaceBlockEvent(player, hand, hitResult);
        EVENT_MANAGER.post(interactBlockEvent);

        if (interactBlockEvent.isCancelled()) {
            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Inject(method = "breakBlock", at = @At(value = "HEAD"), cancellable = true)
    private void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BreakBlockEvent breakBlockEvent = new BreakBlockEvent(pos);
        EVENT_MANAGER.post(breakBlockEvent);
        if (breakBlockEvent.isCancelled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void disableBreakCooldown(CallbackInfoReturnable<Boolean> cir) {
        if (MODULE_MANAGER.getStorage() == null) return;

        NoBreakDelayModule noBreakDelay = MODULE_MANAGER.getStorage().getByClass(NoBreakDelayModule.class);
        if (noBreakDelay != null && noBreakDelay.isEnabled()) {
            this.blockBreakingCooldown = 0;
        }
    }

    @Inject(method = "interactBlock", at = @At(value = "HEAD"), cancellable = true)
    private void interactBlockHead(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> info) {
        if (Nami.MODULE_MANAGER.getStorage() == null) return;

        AntiInteractModule antiInteract = Nami.MODULE_MANAGER.getStorage().getByClass(AntiInteractModule.class);
        if (antiInteract == null || !antiInteract.isEnabled() || !antiInteract.spawnPoint.get()) return;

        if (player.getWorld() == null) return;

        Block block = player.getWorld().getBlockState(hitResult.getBlockPos()).getBlock();
        String dimension = player.getWorld().getDimension().toString();

        Identifier blockId = Registries.BLOCK.getId(block);

        if (antiInteract.whitelist.get() && antiInteract.whitelist.isWhitelisted(blockId)) {
            info.setReturnValue(ActionResult.FAIL);
            return;
        }

        if (player.getWorld().getDimension().bedWorks() && antiInteract.isBed(block)) {
            info.setReturnValue(ActionResult.FAIL);
            return;
        }

        if (block == Blocks.RESPAWN_ANCHOR && dimension.contains("nether")) {
            info.setReturnValue(ActionResult.FAIL);
        }
    }
}
