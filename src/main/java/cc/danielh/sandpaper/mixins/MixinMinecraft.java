package cc.danielh.sandpaper.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = { "startGame" }, at = @At("RETURN"))
    private void startGame(CallbackInfo ci) {
        System.out.println("Successfully injected into startGame.");
    }
}
