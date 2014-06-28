package me.libraryaddict.blocks.utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import me.libraryaddict.blocks.LibBlock;
import me.libraryaddict.blocks.LibChunk;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;

public class LibsPacketReader {

    private static LibChunk getChunk(byte[] buffer, int chunkX, int chunkZ, int chunkYMin, int chunkYMax, boolean fillToHeight) {
        int bytesRead = 0;
        boolean hasSky = true;// world.getEnvironment() == Environment.WORLD;
        LibChunkSection[] storageArrays = new LibChunkSection[16];

        for (int i = 0; i < 16; ++i) {
            if ((chunkYMin & 1 << i) != 0) {
                if (storageArrays[i] == null) {
                    storageArrays[i] = new LibChunkSection();
                }

                byte[] var8 = new byte[4096];
                System.arraycopy(buffer, bytesRead, var8, 0, var8.length);
                storageArrays[i].writeIds(var8);
                bytesRead += var8.length;
            } else if (fillToHeight && storageArrays[i] != null) {
                storageArrays[i] = null;
            }
        }

        for (int i = 0; i < storageArrays.length; ++i) {
            if ((chunkYMin & 1 << i) != 0 && storageArrays[i] != null) {
                byte[] data = new byte[2048];
                System.arraycopy(buffer, bytesRead, data, 0, data.length);
                storageArrays[i].setDataArray(data);
                bytesRead += data.length;
            }
        }

        for (int i = 0; i < storageArrays.length; ++i) {
            if ((chunkYMin & 1 << i) != 0 && storageArrays[i] != null) {
                byte[] data = new byte[2048];
                System.arraycopy(buffer, bytesRead, data, 0, data.length);
                storageArrays[i].setBlockLight(data);
                bytesRead += data.length;
            }
        }

        if (hasSky) {
            for (int i = 0; i < storageArrays.length; ++i) {
                if ((chunkYMin & 1 << i) != 0 && storageArrays[i] != null) {
                    byte[] data = new byte[2048];
                    System.arraycopy(buffer, bytesRead, data, 0, data.length);
                    storageArrays[i].setSkyLight(data);
                    bytesRead += data.length;
                }
            }
        }

    //   for (int i = 0; i < storageArrays.length; ++i) {
      //     if ((chunkYMax & 1 << i) != 0) {
                // if (storageArrays[i] == null) {
      //          bytesRead += 2048;
                /* } else {
                     NibbleArray var9 = storageArrays[i].getBlockMSBArray();

                     if (var9 == null) {
                         var9 = storageArrays[i].createBlockMSBArray();
                     }

                     System.arraycopy(buffer, var5, var9.data, 0, var9.data.length);
                     var5 += var9.data.length;
                 }
                } else if (fillToHeight && storageArrays[i] != null && storageArrays[i].getBlockMSBArray() != null) {
                 storageArrays[i].clearMSBArray();*/
         //   }
      //  }
        byte[] biomeData = new byte[256];
        if (fillToHeight) {
            System.arraycopy(buffer, bytesRead, biomeData, 0, biomeData.length);
        }
        return new LibChunk(chunkX, chunkZ, storageArrays, biomeData);
    }

    public static LibBlock readBlock(PacketContainer packet) {
        if (packet.getType() != PacketType.Play.Server.BLOCK_CHANGE)
            throw new IllegalArgumentException("Expected " + PacketType.Play.Server.BLOCK_CHANGE + ". Received "
                    + packet.getType());
        LibBlock b = new LibBlock(packet.getIntegers().read(0), packet.getIntegers().read(1), packet.getIntegers().read(2),
                packet.getBlocks().read(0).getId(), (byte) (int) packet.getIntegers().read(3));
        return b;
    }

    public static LibChunk readChunk(PacketContainer packet) {
        if (packet.getType() != PacketType.Play.Server.MAP_CHUNK)
            throw new IllegalArgumentException("Expected " + PacketType.Play.Server.MAP_CHUNK + ". Received " + packet.getType());
        return LibsPacketReader.getChunk(packet.getByteArrays().read(1), packet.getIntegers().read(0),
                packet.getIntegers().read(1), packet.getIntegers().read(2), packet.getIntegers().read(3), packet.getBooleans()
                        .read(0));
    }

    public static ArrayList<LibChunk> readChunks(PacketContainer packet) {
        if (packet.getType() != PacketType.Play.Server.MAP_CHUNK_BULK)
            throw new IllegalArgumentException("Expected " + PacketType.Play.Server.MAP_CHUNK_BULK + ". Received "
                    + packet.getType());
        StructureModifier<int[]> ints = packet.getIntegerArrays();
        int[] a = ints.read(0);
        int[] b = ints.read(1);
        int[] c = ints.read(2);
        int[] d = ints.read(3);
        byte[][] buffers = (byte[][]) packet.getModifier().read(5);
        ArrayList<LibChunk> chunks = new ArrayList<LibChunk>();
        for (int i = 0; i < a.length; i++) {
            LibChunk chunk = LibsPacketReader.getChunk(buffers[i], a[i], b[i], c[i], d[i], true);
            chunks.add(chunk);
        }
        return chunks;
    }

    public static ArrayList<LibBlock> readMultipleBlocks(PacketContainer packet) {
        if (packet.getType() != PacketType.Play.Server.MULTI_BLOCK_CHANGE)
            throw new IllegalArgumentException("Expected " + PacketType.Play.Server.MULTI_BLOCK_CHANGE + ". Received "
                    + packet.getType());
        ArrayList<LibBlock> blocks = new ArrayList<LibBlock>();
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(packet.getByteArrays().read(0));
            DataInputStream nStream = new DataInputStream(stream);
            boolean doneZero = false;
            for (int i = 0; i < packet.getIntegers().read(0); i++) {
                int loc = nStream.readShort();
                int data = nStream.readShort();
                if (loc == 0 && data == 0) {
                    if (doneZero) {
                        continue;
                    } else {
                        doneZero = true;
                    }
                }
                int x = loc >> 12 & 0xF;
                int y = loc & 0xFF;
                int z = loc >> 8 & 0xF;
                blocks.add(new LibBlock(x, y, z, data >> 4 & 0xFFF, (byte) (data & 0xF)));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return blocks;
    }

}
