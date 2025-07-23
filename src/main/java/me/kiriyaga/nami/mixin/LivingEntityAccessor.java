package me.kiriyaga.nami.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor("bodyYaw") void setBodyYaw(float yaw);
    @Accessor("headYaw") void setHeadYaw(float yaw);

    @Accessor("bodyYaw") float getBodyYaw();
    @Accessor("headYaw") float getHeadYaw();
}
