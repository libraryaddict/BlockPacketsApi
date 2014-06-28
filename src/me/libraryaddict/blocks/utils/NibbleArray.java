package me.libraryaddict.blocks.utils;

import java.util.Arrays;

public class NibbleArray {
    private final int arraySizeMaybe = 4;
    private final int arraySizePlus4 = 8;
    private byte[] bytes;
    private int length = 2048;
    private byte trivialByte;
    private byte trivialValue;

    public int copyToByteArray(byte[] dest, int off) {
        if (this.bytes == null) {
            Arrays.fill(dest, off, off + this.length, this.trivialByte);
            return off + this.length;
        }
        System.arraycopy(this.bytes, 0, dest, off, this.bytes.length);
        return off + this.bytes.length;
    }

    public int getByteLength() {
        if (this.bytes == null) {
            return this.length;
        }
        return this.bytes.length;
    }

    public int getValue(int x, int y, int z) {
        if (this.bytes == null)
            return this.trivialValue;
        int l = y << this.arraySizePlus4 | z << this.arraySizeMaybe | x;
        int i1 = l >> 1;
        int j1 = l & 0x1;

        return j1 == 0 ? this.bytes[i1] & 0xF : this.bytes[i1] >> 4 & 0xF;
    }

    public void setValue(int x, int y, int z, int dataValue) {
        if (this.bytes == null) {
            if (dataValue != this.trivialValue) {
                this.bytes = new byte[this.length];
                if (this.trivialByte != 0)
                    Arrays.fill(this.bytes, this.trivialByte);
            } else {
                return;
            }
        }

        int i1 = y << this.arraySizePlus4 | z << this.arraySizeMaybe | x;
        int j1 = i1 >> 1;
        int k1 = i1 & 0x1;

        if (k1 == 0)
            this.bytes[j1] = ((byte) (this.bytes[j1] & 0xF0 | dataValue & 0xF));
        else
            this.bytes[j1] = ((byte) (this.bytes[j1] & 0xF | (dataValue & 0xF) << 4));
    }
}