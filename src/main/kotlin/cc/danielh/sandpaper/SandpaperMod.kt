package cc.danielh.sandpaper

import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent

@Mod(
    modid = SandpaperMod.MOD_ID,
    name = SandpaperMod.MOD_NAME,
    version = SandpaperMod.VERSION
)
class SandpaperMod {

    companion object {
        const val MOD_ID = "sandpaper"
        const val MOD_NAME = "Sandpaper"
        const val VERSION = "1.0"
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        print("Initializing Sandpaper...")
    }
}
