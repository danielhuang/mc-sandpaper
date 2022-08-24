package cc.danielh.sandpaper.mixins;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderManager.class)
public class MixinRenderManager {
    @Shadow
    public <T extends Entity> Render<T> getEntityRenderObject(Entity entityIn)
    {
        throw new AbstractMethodError();
    }

    @Overwrite
    public boolean shouldRender(Entity entityIn, ICamera camera, double camX, double camY, double camZ)
    {
        try {
            Render<Entity> render = this.<Entity>getEntityRenderObject(entityIn);
            return render != null && render.shouldRender(entityIn, camera, camX, camY, camZ);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}