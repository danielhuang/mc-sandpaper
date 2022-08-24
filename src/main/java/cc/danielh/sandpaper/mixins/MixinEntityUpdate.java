package cc.danielh.sandpaper.mixins;

import com.google.common.collect.Lists;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ITickable;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(World.class)
public class MixinEntityUpdate {
    @Shadow
    public final Profiler theProfiler = null;

    @Shadow
    public final List<Entity> loadedEntityList = Lists.<Entity>newArrayList();

    @Shadow
    protected final List<Entity> unloadedEntityList = Lists.<Entity>newArrayList();

    @Shadow
    public final List<TileEntity> loadedTileEntityList = Lists.<TileEntity>newArrayList();

    @Shadow
    public final List<TileEntity> tickableTileEntities = Lists.<TileEntity>newArrayList();

    @Shadow
    private final List<TileEntity> addedTileEntityList = Lists.<TileEntity>newArrayList();

    @Shadow
    private final List<TileEntity> tileEntitiesToBeRemoved = Lists.<TileEntity>newArrayList();

    @Shadow
    public final List<EntityPlayer> playerEntities = Lists.<EntityPlayer>newArrayList();

    @Shadow
    public final List<Entity> weatherEffects = Lists.<Entity>newArrayList();


    @Shadow
    public void removeEntity(Entity entityIn) {
    }

    @Shadow
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        throw new AbstractMethodError("Shadow");
    }

    @Shadow
    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
        throw new AbstractMethodError("Shadow");
    }

    @Shadow
    public void onEntityRemoved(Entity entityIn) {
    }

    @Shadow
    public void updateEntityWithOptionalForce(Entity entityIn, boolean forceUpdate) {
        throw new AbstractMethodError();
    }

    private static boolean optimizeEntityUpdate = false;

    @Overwrite
    public void updateEntity(Entity ent) {
        if (!optimizeEntityUpdate) {
            this.updateEntityWithOptionalForce(ent, true);
            return;
        }

        synchronized (ent) {
            synchronized (getChunkFromChunkCoords(ent.chunkCoordX, ent.chunkCoordZ)) {
                int l = MathHelper.floor_double(ent.posX / 16.0D);
                int i1 = MathHelper.floor_double(ent.posY / 16.0D);
                int j1 = MathHelper.floor_double(ent.posZ / 16.0D);

                if (this.isChunkLoaded(l, j1, true)) {
                    synchronized (this.getChunkFromChunkCoords(l, j1)) {
                        this.updateEntityWithOptionalForce(ent, true);
                    }
                } else {
                    this.updateEntityWithOptionalForce(ent, true);
                }
            }
        }
    }

    @Shadow
    private boolean processingLoadedTiles;

    @Shadow
    public boolean isBlockLoaded(BlockPos pos) {
        throw new AbstractMethodError("Shadow");
    }

    @Overwrite
    public void updateEntities() {
        boolean isProfilerAlreadyEnabled = this.theProfiler.profilingEnabled;
        optimizeEntityUpdate = true;

        this.theProfiler.startSection("entities (optimized)");

        this.theProfiler.profilingEnabled = false;

        this.theProfiler.startSection("global");

        for (int i = 0; i < this.weatherEffects.size(); ++i) {
            Entity entity = (Entity) this.weatherEffects.get(i);

            try {
                ++entity.ticksExisted;
                entity.onUpdate();
            } catch (Throwable throwable2) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable2, "Ticking entity");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being ticked");

                if (entity == null) {
                    crashreportcategory.addCrashSection("Entity", "~~NULL~~");
                } else {
                    entity.addEntityCrashInfo(crashreportcategory);
                }

                if (net.minecraftforge.common.ForgeModContainer.removeErroringEntities) {
                    net.minecraftforge.fml.common.FMLLog.severe(crashreport.getCompleteReport());
                    removeEntity(entity);
                } else {
                    throw new ReportedException(crashreport);
                }
            }

            if (entity.isDead) {
                this.weatherEffects.remove(i--);
            }
        }

        this.theProfiler.endStartSection("remove");
        this.loadedEntityList.removeAll(this.unloadedEntityList);

        for (int k = 0; k < this.unloadedEntityList.size(); ++k) {
            Entity entity1 = (Entity) this.unloadedEntityList.get(k);
            int j = entity1.chunkCoordX;
            int l1 = entity1.chunkCoordZ;

            if (entity1.addedToChunk && this.isChunkLoaded(j, l1, true)) {
                this.getChunkFromChunkCoords(j, l1).removeEntity(entity1);
            }
        }

        for (int l = 0; l < this.unloadedEntityList.size(); ++l) {
            this.onEntityRemoved((Entity) this.unloadedEntityList.get(l));
        }

        this.unloadedEntityList.clear();
        this.theProfiler.endStartSection("regular");

        this.theProfiler.startSection("tick (optimized)");

        for (int i1 = 0; i1 < this.loadedEntityList.size(); ++i1) {
            Entity entity2 = (Entity) this.loadedEntityList.get(i1);
            if (entity2.ridingEntity != null) {
                if (!entity2.ridingEntity.isDead && entity2.ridingEntity.riddenByEntity == entity2) {
                    continue;
                }

                entity2.ridingEntity.riddenByEntity = null;
                entity2.ridingEntity = null;
            }

            if (entity2.riddenByEntity != null || entity2.ridingEntity != null) {
                this.updateEntity(entity2);
            }
        }

        this.loadedEntityList.parallelStream().forEachOrdered(entity2 -> {

            if (entity2.riddenByEntity != null || entity2.ridingEntity != null) {
                return;
            }
            if (!entity2.isDead) {
                try {
                    this.updateEntity(entity2);
                } catch (Throwable throwable1) {
                    CrashReport crashreport1 = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                    CrashReportCategory crashreportcategory2 = crashreport1.makeCategory("Entity being ticked");
                    entity2.addEntityCrashInfo(crashreportcategory2);
                    if (net.minecraftforge.common.ForgeModContainer.removeErroringEntities) {
                        net.minecraftforge.fml.common.FMLLog.severe(crashreport1.getCompleteReport());
                        removeEntity(entity2);
                    } else {
                        throw new ReportedException(crashreport1);
                    }
                }
            }

        });


        this.theProfiler.endSection();

        for (int i1 = 0; i1 < this.loadedEntityList.size(); ++i1) {
            Entity entity2 = (Entity) this.loadedEntityList.get(i1);


            this.theProfiler.startSection("remove");

            if (entity2.isDead) {
                int k1 = entity2.chunkCoordX;
                int i2 = entity2.chunkCoordZ;

                if (entity2.addedToChunk && this.isChunkLoaded(k1, i2, true)) {
                    this.getChunkFromChunkCoords(k1, i2).removeEntity(entity2);
                }

                this.loadedEntityList.remove(i1--);
                this.onEntityRemoved(entity2);
            }

            this.theProfiler.endSection();
        }

        this.theProfiler.endStartSection("blockEntities");
        this.processingLoadedTiles = true;
        Iterator<TileEntity> iterator = this.tickableTileEntities.iterator();

        while (iterator.hasNext()) {
            TileEntity tileentity = (TileEntity) iterator.next();

            if (!tileentity.isInvalid() && tileentity.hasWorldObj()) {
                BlockPos blockpos = tileentity.getPos();

                if (this.isBlockLoaded(blockpos)) {
                    try {
                        ((ITickable) tileentity).update();
                    } catch (Throwable throwable) {
                        CrashReport crashreport2 = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                        CrashReportCategory crashreportcategory1 = crashreport2.makeCategory("Block entity being ticked");
                        tileentity.addInfoToCrashReport(crashreportcategory1);
                        if (net.minecraftforge.common.ForgeModContainer.removeErroringTileEntities) {
                            net.minecraftforge.fml.common.FMLLog.severe(crashreport2.getCompleteReport());
                            tileentity.invalidate();
                            this.removeTileEntity(tileentity.getPos());
                        } else {
                            throw new ReportedException(crashreport2);
                        }
                    }
                }
            }

            if (tileentity.isInvalid()) {
                iterator.remove();
                this.loadedTileEntityList.remove(tileentity);

                if (this.isBlockLoaded(tileentity.getPos())) {
                    this.getChunkFromBlockCoords(tileentity.getPos()).removeTileEntity(tileentity.getPos());
                }
            }
        }

        if (!this.tileEntitiesToBeRemoved.isEmpty()) {
            for (Object tile : tileEntitiesToBeRemoved) {
                ((TileEntity) tile).onChunkUnload();
            }

            this.tickableTileEntities.removeAll(this.tileEntitiesToBeRemoved);
            this.loadedTileEntityList.removeAll(this.tileEntitiesToBeRemoved);
            this.tileEntitiesToBeRemoved.clear();
        }

        this.processingLoadedTiles = false;

        this.theProfiler.endStartSection("pendingBlockEntities");

        if (!this.addedTileEntityList.isEmpty()) {
            for (int j1 = 0; j1 < this.addedTileEntityList.size(); ++j1) {
                TileEntity tileentity1 = (TileEntity) this.addedTileEntityList.get(j1);

                if (!tileentity1.isInvalid()) {
                    if (!this.loadedTileEntityList.contains(tileentity1)) {
                        this.addTileEntity(tileentity1);
                    }

                    if (this.isBlockLoaded(tileentity1.getPos())) {
                        this.getChunkFromBlockCoords(tileentity1.getPos()).addTileEntity(tileentity1.getPos(), tileentity1);
                    }

                    this.markBlockForUpdate(tileentity1.getPos());
                }
            }

            this.addedTileEntityList.clear();
        }

        this.theProfiler.profilingEnabled = isProfilerAlreadyEnabled;

        optimizeEntityUpdate = false;

        this.theProfiler.endSection();
    }

    @Shadow
    private void markBlockForUpdate(BlockPos pos) {
        throw new AbstractMethodError("Shadow");
    }

    @Shadow
    private Chunk getChunkFromBlockCoords(BlockPos pos) {
        throw new AbstractMethodError("Shadow");
    }

    @Shadow
    public boolean addTileEntity(TileEntity tile) {
        throw new AbstractMethodError("Shadow");
    }

    @Shadow
    private void removeTileEntity(BlockPos pos) {
        throw new AbstractMethodError("Shadow");
    }

    @Shadow
    public com.google.common.collect.ImmutableSetMultimap<ChunkCoordIntPair, net.minecraftforge.common.ForgeChunkManager.Ticket> getPersistentChunks() {
        throw new AbstractMethodError("Shadow");
    }
}