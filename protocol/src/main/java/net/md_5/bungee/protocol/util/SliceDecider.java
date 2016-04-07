package net.md_5.bungee.protocol.util;

public interface SliceDecider
{
    boolean shouldNotSlice(int packetId);
}
