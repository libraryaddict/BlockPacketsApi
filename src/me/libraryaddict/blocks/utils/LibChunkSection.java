package me.libraryaddict.blocks.utils;

import me.libraryaddict.blocks.LibBlock;

public class LibChunkSection {

    private LibBlock[][][] blocks = new LibBlock[16][16][16];

    public LibBlock getBlock(int x, int y, int z) {
        if (blocks[x][y][z] == null) {
            blocks[x][y][z] = new LibBlock(x, y, z, 0, (byte) 0);
        }
        return blocks[x][y][z];
    }

    public NibbleArray getDataArray() {
        NibbleArray data = new NibbleArray();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    data.setValue(x, y, z, hasBlock(x, y, z) ? getBlock(x, y, z).getData() : 0);
                }
            }
        }
        return data;
    }

    public NibbleArray getEmittedLightArray() {
        NibbleArray data = new NibbleArray();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    data.setValue(x, y, z, hasBlock(x, y, z) ? getBlock(x, y, z).getLightFromBlocks() : 0);
                }
            }
        }
        return data;
    }

    public byte[] getIds() {
        byte[] bytes = new byte[4096];
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    bytes[(y << 8 | z << 4 | x)] = ((byte) ((hasBlock(x, y, z) ? getBlock(x, y, z).getTypeId() : 0) & 0xFF));
                }
            }
        }
        return bytes;
    }

    public NibbleArray getSkyLightArray() {
        NibbleArray data = new NibbleArray();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    data.setValue(x, y, z, hasBlock(x, y, z) ? getBlock(x, y, z).getLightFromSky() : 0);
                }
            }
        }
        return data;
    }

    public boolean hasBlock(int x, int y, int z) {
        return blocks[x][y][z] != null;
    }

    public boolean isEmpty() {
        for (LibBlock[][] block : blocks) {
            if (block != null) {
                return false;
            }
        }
        return true;
    }

    public void setBlock(LibBlock block) {
        blocks[block.getX()][block.getY() & 0xF][block.getZ()] = block;
    }

    public void setBlockLight(byte[] data) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    LibBlock b = getBlock(x, y, z);
                    int l = y << 8 | z << 4 | x;
                    int i1 = l >> 1;
                    int j1 = l & 0x1;
                    b.setLightFromBlocks((byte) (j1 == 0 ? data[i1] & 0xF : data[i1] >> 4 & 0xF));
                }
            }
        }
    }

    public void setDataArray(byte[] data) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    LibBlock b = getBlock(x, y, z);
                    int l = y << 8 | z << 4 | x;
                    int i1 = l >> 1;
                    int j1 = l & 0x1;

                    b.setData((byte) (j1 == 0 ? data[i1] & 0xF : data[i1] >> 4 & 0xF));
                }
            }
        }
    }

    public void setSkyLight(byte[] data) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    LibBlock b = getBlock(x, y, z);
                    int l = y << 8 | z << 4 | x;
                    int i1 = l >> 1;
                    int j1 = l & 0x1;
                    b.setLightFromSky((byte) (j1 == 0 ? data[i1] & 0xF : data[i1] >> 4 & 0xF));
                }
            }
        }
    }

    public void writeIds(byte[] bytes) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int id = bytes[y << 8 | z << 4 | x] & 255;
                    LibBlock b = new LibBlock(x, y, z, id, (byte) 0);
                    this.setBlock(b);
                }
            }
        }
    }
}
