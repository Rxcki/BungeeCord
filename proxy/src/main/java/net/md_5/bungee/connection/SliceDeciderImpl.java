package net.md_5.bungee.connection;

import lombok.RequiredArgsConstructor;

import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.util.SliceDecider;

@RequiredArgsConstructor
public class SliceDeciderImpl implements SliceDecider
{
    private final HandlerBoss handlerBoss;
    
    @Override
    public boolean shouldNotSlice(int packetId)
    {
        return handlerBoss != null && handlerBoss.getHandler().isEntityRewritePossiblyNeeded( packetId );
    }
}
