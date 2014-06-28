package me.libraryaddict.blocks.utils;

public class ChunkMap {
    private byte[] buffer;
    private int maxChunkHeight;
    private int minChunkHeight;

    public byte[] getBuffer() {
        return buffer;
    }

    public int getMaxChunkHeight() {
        return maxChunkHeight;
    }

    public int getMinChunkHeight() {
        return minChunkHeight;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public void setMaxChunkHeight(int maxChunkHeight) {
        this.maxChunkHeight = maxChunkHeight;
    }

    public void setMinChunkHeight(int minChunkHeight) {
        this.minChunkHeight = minChunkHeight;
    }
}
