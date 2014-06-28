package me.libraryaddict.blocks.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.Deflater;

import org.bukkit.World;

import me.libraryaddict.blocks.LibBlock;
import me.libraryaddict.blocks.LibChunk;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;

public class LibsPacketWriter {

    public static void writeBlockPacket(PacketContainer packet, LibBlock block) {
        if (packet.getType() != PacketType.Play.Server.BLOCK_CHANGE)
            throw new IllegalArgumentException("Expected " + PacketType.Play.Server.BLOCK_CHANGE + ". Received "
                    + packet.getType());
        packet.getIntegers().write(0, block.getX());
        packet.getIntegers().write(1, block.getY());
        packet.getIntegers().write(2, block.getZ());
        packet.getBlocks().write(0, block.getType());
        packet.getIntegers().write(3, (int) block.getData());
    }

    public static void writeBulkChunkPacket(PacketContainer packet, ArrayList<LibChunk> chunks) {
        if (packet.getType() != PacketType.Play.Server.MAP_CHUNK_BULK)
            throw new IllegalArgumentException("Expected " + PacketType.Play.Server.MAP_CHUNK_BULK + ". Received "
                    + packet.getType());
        if (chunks.size() > 5) {
            throw new IllegalArgumentException("Max of 5 chunks can be sent!");
        }
        int[] a = new int[chunks.size()];
        int[] b = new int[chunks.size()];
        int[] c = new int[chunks.size()];
        int[] d = new int[chunks.size()];
        byte[][] buffers = new byte[chunks.size()][];
        for (int i = 0; i < chunks.size(); i++) {
            LibChunk chunk = chunks.get(i);
            ChunkMap chunkmap = writeToChunkMap(chunk, true, 65535);
            a[i] = chunk.getX();
            b[i] = chunk.getZ();
            c[i] = chunkmap.getMinChunkHeight();
            d[i] = chunkmap.getMaxChunkHeight();
            buffers[i] = chunkmap.getBuffer();
        }
        StructureModifier<int[]> ints = packet.getIntegerArrays();
        ints.write(0, a);
        ints.write(1, b);
        ints.write(2, c);
        ints.write(3, d);
        packet.getModifier().write(5, buffers);
    }

    public static PacketContainer writeChunkPacket(LibChunk chunk) {
        PacketContainer packet = new PacketContainer(Play.Server.MAP_CHUNK);
        writeChunkPacket(packet, chunk);
        return packet;
    }

    public static PacketContainer writeBulkChunkPacket(World world, ArrayList<LibChunk> chunks) {
        PacketContainer packet = new PacketContainer(Play.Server.MAP_CHUNK_BULK);
        try {
            packet.getModifier().write(9, world.getClass().getMethod("getHandle").invoke(world));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        packet.getBooleans().write(0, true);
        writeBulkChunkPacket(packet, chunks);
        return packet;
    }

    public static void writeChunkPacket(PacketContainer packet, LibChunk chunk) {
        if (packet.getType() != PacketType.Play.Server.MAP_CHUNK)
            throw new IllegalArgumentException("Expected " + PacketType.Play.Server.MAP_CHUNK + ". Received " + packet.getType());
        int bufferSize = 0;
        for (int i = 0; i < 16; i++) {
            if (chunk.getChunkSections()[i] != null /*&& !chunk.getChunkSections()[i].isEmpty()*/) {
                bufferSize |= 1 << i;
            }
        }
        ChunkMap map = writeToChunkMap(chunk, bufferSize == 0 || bufferSize == 0xFFFF, bufferSize);

        Deflater deflater = new Deflater(4);
        try {
            deflater.setInput(map.getBuffer(), 0, map.getBuffer().length);
            deflater.finish();
            byte[] e = new byte[map.getBuffer().length];
            int size = deflater.deflate(e);
            packet.getByteArrays().write(0, e);
            packet.getByteArrays().write(1, map.getBuffer());
            StructureModifier<Integer> ints = packet.getIntegers();
            ints.write(0, chunk.getX());
            ints.write(1, chunk.getZ());
            ints.write(2, map.getMinChunkHeight());
            ints.write(3, map.getMaxChunkHeight());
            ints.write(4, size);
            packet.getBooleans().write(0, bufferSize == 0 || bufferSize == 0xFFF);
        } finally {
            deflater.end();
        }
    }

    public static void writeMultipleBlocksPacket(PacketContainer packet, ArrayList<LibBlock> blocks) {
        if (packet.getType() != PacketType.Play.Server.MULTI_BLOCK_CHANGE)
            throw new IllegalArgumentException("Expected " + PacketType.Play.Server.MULTI_BLOCK_CHANGE + ". Received "
                    + packet.getType());
        packet.getIntegers().write(0, blocks.size());
        try {
            ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream(blocks.size() * 4);
            DataOutputStream localDataOutputStream = new DataOutputStream(localByteArrayOutputStream);

            for (int j = 0; j < blocks.size(); j++) {
                LibBlock b = blocks.get(j);
                localDataOutputStream.writeShort((short) (b.getX() << 12 | b.getZ() << 8 | b.getY()));
                localDataOutputStream.writeShort((short) ((b.getTypeId() & 0xFFF) << 4 | b.getData() & 0xF));
            }
            byte[] c = localByteArrayOutputStream.toByteArray();
            packet.getByteArrays().write(0, c);
        } catch (IOException localIOException) {
            System.out.print("Couldn't create bulk block update packet");
            localIOException.printStackTrace();
        }
    }

    private static ChunkMap writeToChunkMap(LibChunk chunk, boolean sendEntireChunk, int howHighToSend) {
        int j = 0;
        LibChunkSection[] chunkSections = chunk.getChunkSections();
        /**
         * Chunk map is just storage
         */
        ChunkMap chunkmap = new ChunkMap();
        byte[] buffer = new byte[196864];

        /**
         * This is checking how far upwards to send this chunk
         */
        for (int l = 0; l < chunkSections.length; l++) {
            if ((chunkSections[l] != null) && ((!sendEntireChunk) || (!chunkSections[l].isEmpty()))
                    && ((howHighToSend & 1 << l) != 0)) {
                chunkmap.setMinChunkHeight(chunkmap.getMinChunkHeight() | 1 << l);
                // if (chunkSections[l].getExtendedIdArray() != null) {
                // chunkmap.extendedIdArraysToSend |= 1 << l;
                // chunkmap.setMaxChunkHeight(chunkmap.getMinChunkHeight() | 1 << l);
                // k++;
                // }
            }
        }

        for (int l = 0; l < chunkSections.length; l++) {
            if ((chunkSections[l] != null) && ((!sendEntireChunk) || (!chunkSections[l].isEmpty()))
                    && ((howHighToSend & 1 << l) != 0)) {
                byte[] idArray = chunkSections[l].getIds();

                System.arraycopy(idArray, 0, buffer, j, idArray.length);
                j += idArray.length;
            }

        }

        for (int l = 0; l < chunkSections.length; l++) {
            if ((chunkSections[l] != null) && ((!sendEntireChunk) || (!chunkSections[l].isEmpty()))
                    && ((howHighToSend & 1 << l) != 0)) {
                NibbleArray nibblearray = chunkSections[l].getDataArray();
                nibblearray.copyToByteArray(buffer, j);
                j += nibblearray.getByteLength();
            }
        }

        for (int l = 0; l < chunkSections.length; l++) {
            if ((chunkSections[l] != null) && ((!sendEntireChunk) || (!chunkSections[l].isEmpty()))
                    && ((howHighToSend & 1 << l) != 0)) {
                NibbleArray nibblearray = chunkSections[l].getEmittedLightArray();
                nibblearray.copyToByteArray(buffer, j);
                j += nibblearray.getByteLength();
            }
        }

        /**
         * If the world isn't the nether and has a sky
         */
        // if (chunk.getWorld().getEnvironment() == Environment.WORLD) {
        for (int l = 0; l < chunkSections.length; l++) {
            if ((chunkSections[l] != null) && ((!sendEntireChunk) || (!chunkSections[l].isEmpty()))
                    && ((howHighToSend & 1 << l) != 0)) {
                NibbleArray nibblearray = chunkSections[l].getSkyLightArray();
                nibblearray.copyToByteArray(buffer, j);
                j += nibblearray.getByteLength();
            }
        }
        // }

        /* if (k > 0) {
             for (int l = 0; l < chunkSections.length; l++) {
                 if ((chunkSections[l] != null) && ((!sendEntireChunk) || (!chunkSections[l].isEmpty()))
                         && (chunkSections[l].getExtendedIdArray() != null) && ((howHighToSend & 1 << l) != 0)) {
                     NibbleArray nibblearray = chunkSections[l].getExtendedIdArray();
                     nibblearray.copyToByteArray(abyte, j);
                     j += nibblearray.getByteLength();
                 }
             }
         }*/

        if (sendEntireChunk) {
            // Write biome data

            System.arraycopy(chunk.getBiomeData(), 0, buffer, j, chunk.getBiomeData().length);
            j += chunk.getBiomeData().length;
        }
        chunkmap.setBuffer(new byte[j]);
        System.arraycopy(buffer, 0, chunkmap.getBuffer(), 0, j);
        return chunkmap;
    }
}
