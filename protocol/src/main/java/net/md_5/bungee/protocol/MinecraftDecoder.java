package net.md_5.bungee.protocol;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import net.md_5.bungee.protocol.util.SliceDecider;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class MinecraftDecoder extends ChannelInboundHandlerAdapter
{

    @Getter
    @Setter
    private Protocol protocol;
    private final boolean server;
    @Setter
    private int protocolVersion;
    private final SliceDecider sliceDecider;

    private Object decode(ByteBuf in) throws Exception
    {
        Protocol.DirectionData prot = ( server ) ? protocol.TO_SERVER : protocol.TO_CLIENT;
        ByteBuf newBuf = null;

        try
        {
            int originalReaderIndex = in.readerIndex();
            int originalReadableBytes = in.readableBytes();

            int packetId = DefinedPacket.readVarInt( in );

            if ( sliceDecider.shouldNotSlice( packetId ) )
            {
                newBuf = in.copy( originalReaderIndex, originalReadableBytes );
            } else
            {
                newBuf = in.slice( originalReaderIndex, originalReadableBytes ).retain();
            }

            DefinedPacket packet = prot.createPacket( packetId, protocolVersion );
            if ( packet != null )
            {
                packet.read( in, prot.getDirection(), protocolVersion );

                if ( in.isReadable() )
                {
                    throw new BadPacketException( "Did not read all bytes from packet " + packet.getClass() + " " + packetId + " Protocol " + protocol + " Direction " + prot );
                }
            } else
            {
                in.skipBytes( in.readableBytes() );
            }

            PacketWrapper packetWrapper = new PacketWrapper( packet, newBuf );
            newBuf = null;
            return packetWrapper;
        } finally
        {
            if ( newBuf != null )
            {
                newBuf.release();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        Object out = null;
        try
        {
            if ( msg instanceof ByteBuf )
            {
                ByteBuf cast = ( ByteBuf ) msg;
                try
                {
                    out = decode( cast );
                } finally
                {
                    cast.release();
                }
            } else
            {
                out = msg;
            }
        } catch ( DecoderException e )
        {
            throw e;
        } catch ( Exception e )
        {
            throw new DecoderException( e );
        } finally
        {
            if ( out != null )
            {
                ctx.fireChannelRead( out );
            }
        }
    }
}
