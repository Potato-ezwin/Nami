package me.kiriyaga.nami.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.event.impl.PacketSendEvent;
import me.kiriyaga.nami.feature.module.impl.misc.AntiPacketKickModule;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.kiriyaga.nami.Nami.*;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow private Channel channel;
    @Shadow @Final private NetworkSide side;

    @Shadow @Nullable private volatile PacketListener packetListener;

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    public void onPacketReceive(ChannelHandlerContext chc, Packet<?> packet, CallbackInfo ci) {
        if (this.channel.isOpen() && packet != null) {
            PacketReceiveEvent event = new PacketReceiveEvent(packet);
            EVENT_MANAGER.post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "sendImmediately", at = @At("HEAD"), cancellable = true)
    private void onPacketSend(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        PacketSendEvent event = new PacketSendEvent(packet);
        EVENT_MANAGER.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void exceptionCaught(ChannelHandlerContext context, Throwable exception, CallbackInfo call) {
        AntiPacketKickModule module = MODULE_MANAGER.getStorage().getByClass(AntiPacketKickModule.class);
        if (module != null && module.isEnabled()) {
            LOGGER.error("Packet ex: \n", exception);
            call.cancel();
        }
    }
}
