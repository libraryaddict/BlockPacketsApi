package me.libraryaddict.blocks.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import me.libraryaddict.blocks.LibChunk;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;

public class LibsPacketSender {
    private static String bukkitVersion = Bukkit.getServer().getClass().getName().split("\\.")[3];

    private static void sendPackets(Player p, HashMap<Chunk, ArrayList<PacketContainer>> sending) {
        try {
            ArrayList<LibChunk> chunks = new ArrayList<LibChunk>();
            for (Chunk c : sending.keySet()) {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p,
                        LibsPacketWriter.writeChunkPacket(LibChunk.copyFrom(c, true, false, true)));
            }
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, LibsPacketWriter.writeBulkChunkPacket(p.getWorld(), chunks));
            for (ArrayList<PacketContainer> tilePackets : sending.values()) {
                for (PacketContainer packet : tilePackets) {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
                }
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static Class getClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + bukkitVersion + "." + name);
    }

    private static ArrayList<Chunk> getVisible(Player player) {
        try {
            Object chunkMap = getClass("WorldServer").getMethod("getPlayerChunkMap").invoke(
                    player.getWorld().getClass().getMethod("getHandle").invoke(player.getWorld()));
            Field f = getClass("PlayerChunkMap").getDeclaredField("f");
            f.setAccessible(true);
            Queue chunks = (Queue) f.get(chunkMap);
            Field list = getClass("PlayerChunk").getDeclaredField("b");
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            ArrayList<Chunk> visibleChunks = new ArrayList<Chunk>();
            Field cords = getClass("PlayerChunk").getDeclaredField("location");
            Field xCord = getClass("ChunkCoordIntPair").getField("x");
            Field zCord = getClass("ChunkCoordIntPair").getField("z");
            List cordPair = (List) nmsPlayer.getClass().getField("chunkCoordIntPairQueue").get(nmsPlayer);
            double pX = player.getLocation().getBlockX();
            double pZ = player.getLocation().getBlockZ();
            int minX = (int) Math.floor((pX - 10D) / 16D);
            int maxX = (int) Math.ceil((pX + 10D) / 16D);
            int minZ = (int) Math.floor((pZ - 10D) / 16D);
            int maxZ = (int) Math.ceil((pZ + 10D) / 16D);
            for (Object chunk : chunks) {
                list.setAccessible(true);
                List l = (List) list.get(chunk);
                if (l.contains(nmsPlayer)) {
                    cords.setAccessible(true);
                    Object c = cords.get(chunk);
                    int x = xCord.getInt(c);
                    int z = zCord.getInt(c);
                    if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                        visibleChunks.add(player.getWorld().getChunkAt(xCord.getInt(c), zCord.getInt(c)));
                    } else if (!cordPair.contains(c)) {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player,
                                LibsPacketWriter.writeChunkPacket(new LibChunk(x, z)));
                        cordPair.add(c);
                    }
                }
            }
            return visibleChunks;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void resendChunks(final Player p) {
        try {
            HashMap<Chunk, ArrayList<PacketContainer>> packetsToSend = new HashMap<Chunk, ArrayList<PacketContainer>>();
            Object worldServer = p.getWorld().getClass().getMethod("getHandle").invoke(p.getWorld());
            Method getTileEntity = getClass("WorldServer").getMethod("getTileEntity", int.class, int.class, int.class);
            Method getUpdatePacket = getClass("TileEntity").getMethod("getUpdatePacket");
            ArrayList<Chunk> chunks = getVisible(p);
            for (Chunk chunk : chunks) {
                ArrayList<PacketContainer> tilePackets = new ArrayList<PacketContainer>();
                for (BlockState state : chunk.getTileEntities()) {
                    Object tile = getTileEntity.invoke(worldServer, state.getX(), state.getY(), state.getZ());
                    if (tile != null) {
                        Object nmsPacket = getUpdatePacket.invoke(tile);
                        if (nmsPacket != null) {
                            PacketContainer packet = PacketContainer.fromPacket(nmsPacket);
                            tilePackets.add(packet);
                        }
                    }
                }
                packetsToSend.put(chunk, tilePackets);
            }
            sendPackets(p, packetsToSend);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
