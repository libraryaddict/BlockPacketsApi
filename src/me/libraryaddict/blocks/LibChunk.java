package me.libraryaddict.blocks;

import me.libraryaddict.blocks.utils.LibChunkSection;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R3.block.CraftBlock;

public class LibChunk {

    private byte[] biomeData;
    private LibChunkSection[] chunkSections;
    private int chunkX, chunkZ;

    public int getMinY() {
        for (int i = 0; i < chunkSections.length; i++) {
            if (chunkSections[i] != null) {
                return i * 16;
            }
        }
        return 0;
    }

    public int getMaxY() {
        for (int i = chunkSections.length; i > 0; i--) {
            if (chunkSections[i - 1] != null) {
                return i * 16;
            }
        }
        return 0;
    }

    public boolean isChunkUnload() {
        for (LibChunkSection section : chunkSections) {
            if (section != null) {
                return false;
            }
        }
        return true;
    }

    public void setChunkUnload() {
        chunkSections = new LibChunkSection[16];
    }

    public LibChunk[] splitToSixteenChunks() {
        LibChunk[] chunks = new LibChunk[16];
        for (int i = 0; i < 16; i++) {
            chunks[i] = new LibChunk(getX(), getZ());
            chunks[i].chunkSections[i] = chunkSections[i];
        }
        return chunks;
    }

    public LibChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkSections = new LibChunkSection[16];
        this.biomeData = new byte[256];
    }

    public LibChunk(int x, int z, LibChunkSection[] sections, byte[] biomeData) {
        this.chunkSections = sections;
        this.chunkX = x;
        this.chunkZ = z;
        this.biomeData = biomeData;
    }

    public byte[] getBiomeData() {
        return biomeData;
    }

    public LibBlock getBlock(int x, int y, int z) {
        int blockY = y >> 4;
        if (chunkSections[blockY] == null) {
            chunkSections[blockY] = new LibChunkSection();
        }
        return chunkSections[blockY].getBlock(x, y & 0xF, z);
    }

    public LibChunkSection[] getChunkSections() {
        return chunkSections;
    }

    public int getX() {
        return chunkX;
    }

    public int getZ() {
        return chunkZ;
    }

    public boolean hasBlock(int x, int y, int z) {
        int blockY = y >> 4;
        if (chunkSections[blockY] == null) {
            return false;
        }
        return chunkSections[blockY].hasBlock(x, y & 0xF, z);
    }

    public void setBiome(int x, int z, Biome biome) {
        biomeData[((z & 0xF) << 4) | (x & 0xF)] = (byte) CraftBlock.biomeToBiomeBase(biome).id;
    }

    public void setBlock(int x, int y, int z, int id, byte data) {
        int blockY = y >> 4;
        if (chunkSections[blockY] == null) {
            chunkSections[blockY] = new LibChunkSection();
        }
        chunkSections[blockY].setBlock(new LibBlock(x, y, z, id, data));
    }

    public void setBlock(LibBlock block) {
        int blockY = block.getY() >> 4;
        if (chunkSections[blockY] == null) {
            chunkSections[blockY] = new LibChunkSection();
        }
        chunkSections[blockY].setBlock(block);
    }

    public static LibChunk copyFrom(ChunkSnapshot c, boolean copyLight, boolean copyBiome, boolean copyAir) {
        LibChunk chunk = new LibChunk(c.getX(), c.getZ());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (copyBiome) {
                    chunk.setBiome(x, z, c.getBiome(x, z));
                }
                for (int y = 0; y < 256; y++) {
                    int id = c.getBlockTypeId(x, y, z);
                    if (copyAir || id != 0) {
                        LibBlock block = new LibBlock(x, y, z, id, (byte) c.getBlockData(x, y, z));
                        if (copyLight) {
                            block.setLightFromBlocks(c.getBlockEmittedLight(x, y, z));
                            block.setLightFromSky(c.getBlockSkyLight(x, y, z));
                        }
                        chunk.setBlock(block);
                    }
                }
            }
        }
        return chunk;
    }

    public static LibChunk copyFrom(Chunk c, boolean copyLight, boolean copyBiome, boolean copyAir) {
        LibChunk chunk = new LibChunk(c.getX(), c.getZ());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    Block block = c.getBlock(x, y, z);
                    if (copyBiome && y == 0) {
                        chunk.setBiome(x, z, block.getBiome());
                    }
                    if (copyAir || block.getTypeId() != 0) {
                        LibBlock b = new LibBlock(x, y, z, block.getTypeId(), (byte) block.getData());
                        if (copyLight) {
                            b.setLightFromBlocks(block.getLightFromBlocks());
                            b.setLightFromSky(block.getLightFromSky());
                        }
                        chunk.setBlock(b);
                    }
                }
            }
        }
        return chunk;
    }
}
