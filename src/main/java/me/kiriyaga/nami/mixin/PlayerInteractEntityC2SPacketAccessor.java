package me.kiriyaga.nami.mixin;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInteractEntityC2SPacket.class)
public interface PlayerInteractEntityC2SPacketAccessor {
    @Accessor("type")
    PlayerInteractEntityC2SPacket.InteractTypeHandler getTypeHandler();
}
