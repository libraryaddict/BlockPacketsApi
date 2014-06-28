package me.libraryaddict.blocks;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class LibBlock {
    private byte blockData;
    private int blockId;
    private int x, y, z, lightFromSky, lightFromBlocks;

    public LibBlock(int x, int y, int z, int blockId, byte blockData) {
        this(x, y, z);
        this.blockId = blockId;
        this.blockData = blockData;
    }

    @Override
    public String toString() {
        return "LibBlock[Id=" + blockId + ", Data=" + blockData + ", X=" + x + ", Y=" + y + ", Z=" + z + ", skyLight="
                + lightFromSky + ", blockLight=" + lightFromBlocks + "]";
    }

    public LibBlock(int x2, int y2, int z2) {
        this.x = x2;
        this.y = y2;
        this.z = z2;
    }

    public byte getData() {
        return blockData;
    }

    public int getLightFromBlocks() {
        return lightFromBlocks;
    }

    public int getLightFromSky() {
        return lightFromSky;
    }

    public byte getLightLevel() {
        return (byte) Math.max(getLightFromBlocks(), getLightFromBlocks());
    }

    public Material getType() {
        return Material.getMaterial(getTypeId());
    }

    public int getTypeId() {
        return blockId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void giveData(Block block) {
        this.setTypeIdAndData(block.getTypeId(), block.getData());
        this.setLightFromBlocks(block.getLightFromBlocks());
        this.setLightFromSky(block.getLightFromSky());
    }

    public void giveData(LibBlock block) {
        this.setTypeIdAndData(block.getTypeId(), block.getData());
        this.setLightFromBlocks(block.getLightFromBlocks());
        this.setLightFromSky(block.getLightFromSky());
    }

    public void setData(byte arg0) {
        this.blockData = arg0;
    }

    public void setLightFromBlocks(int newLight) {
        lightFromBlocks = newLight;
    }

    public void setLightFromSky(int newLight) {
        lightFromSky = newLight;
    }

    public void setType(Material arg0) {
        this.blockId = arg0.getId();
    }

    public void setTypeId(int arg0) {
        this.blockId = arg0;
    }

    public void setTypeIdAndData(int arg0, byte arg1) {
        this.blockId = arg0;
        this.blockData = arg1;
    }

    public void setTypeAndData(Material mat, byte data) {
        this.blockId = mat.getId();
        this.blockData = data;
    }

}
