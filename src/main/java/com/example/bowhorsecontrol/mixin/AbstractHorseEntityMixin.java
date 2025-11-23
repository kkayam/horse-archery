package com.example.bowhorsecontrol.mixin;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractHorseEntity.class)
public class AbstractHorseEntityMixin {
    
    /**
     * Redirects the getRotationVector() call in travel() to return zero vector
     * when the player is drawing a bow, preventing the horse from following camera direction.
     */
    @Redirect(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractHorseEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d redirectRotationVector(AbstractHorseEntity horse) {
        // Check if the horse has a passenger
        if (horse.getFirstPassenger() instanceof PlayerEntity player) {
            // Check if the player is using a bow (drawing)
            if (player.isUsingItem() && player.getActiveItem().getItem() instanceof net.minecraft.item.BowItem) {
                // Return zero vector to prevent horse from following camera direction
                // This allows free camera movement while drawing the bow
                // The horse will still respond to explicit movement key presses (WASD)
                return Vec3d.ZERO;
            }
        }
        
        // Normal behavior: return the rotation vector
        return horse.getRotationVector();
    }
}

