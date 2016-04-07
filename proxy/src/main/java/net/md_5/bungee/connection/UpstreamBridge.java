package net.md_5.bungee.connection;

import io.netty.buffer.SlicedByteBuf;
import me.minotopia.flexpipe.api.event.SuspiciousPlayerBehaviourEvent;

import com.google.common.base.Preconditions;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.TabCompleteRequest;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.PluginMessage;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.protocol.packet.TabCompleteResponse;

public class UpstreamBridge extends PacketHandler
{

    private long lastTabComplete = -1;
    private final ProxyServer bungee;
    private final UserConnection con;

    public UpstreamBridge(ProxyServer bungee, UserConnection con)
    {
        this.bungee = bungee;
        this.con = con;

        BungeeCord.getInstance().addConnection( con );
        con.getTabListHandler().onConnect();
        con.unsafe().sendPacket( BungeeCord.getInstance().registerChannels() );
    }

    @Override
    public void exception(Throwable t) throws Exception
    {
        con.disconnect( Util.exception( t ) );
    }

    @Override
    public void disconnected(ChannelWrapper channel) throws Exception
    {
        // We lost connection to the client
        PlayerDisconnectEvent event = new PlayerDisconnectEvent( con );
        bungee.getPluginManager().callEvent( event );
        con.getTabListHandler().onDisconnect();
        BungeeCord.getInstance().removeConnection( con );

        if ( con.getServer() != null )
        {
            // Manually remove from everyone's tab list
            // since the packet from the server arrives
            // too late
            // TODO: This should only done with server_unique
            //       tab list (which is the only one supported
            //       currently)
            PlayerListItem packet = new PlayerListItem();
            packet.setAction( PlayerListItem.Action.REMOVE_PLAYER );
            PlayerListItem.Item item = new PlayerListItem.Item();
            item.setUuid( con.getUniqueId() );
            packet.setItems( new PlayerListItem.Item[]
            {
                item
            } );
            for ( ProxiedPlayer player : con.getServer().getInfo().getPlayers() )
            {
                player.unsafe().sendPacket( packet );
            }
            con.getServer().disconnect();
        }
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
        // if we get a SlicedByteBuf we know that entity rewrite is not needed - see MinecraftDecoder
        if ( !( packet.buf instanceof SlicedByteBuf ) )
        {
            con.getEntityRewrite().rewriteServerbound( packet.buf, con.getClientEntityId(), con.getServerEntityId() );
        }
        if ( con.getServer() != null )
        {
            con.getServer().getCh().write( packet );
        }
    }

    @Override
    public boolean isEntityRewritePossiblyNeeded(int packetId)
    {
        return con.getEntityRewrite().isRewriteServerbound( packetId );
    }

    @Override
    public void handle(KeepAlive alive) throws Exception
    {
        if ( alive.getRandomId() == con.getSentPingId() )
        {
            int newPing = (int) ( System.currentTimeMillis() - con.getSentPingTime() );
            con.getTabListHandler().onPingChange( newPing );
            con.setPing( newPing );
        }
    }

    private static final BaseComponent[] CHAT_INVALID_DISCONNECT_REASON = new BaseComponent[]{ new TextComponent( "Outdated Client!" ) };

    @Override
    public void handle(Chat chat) throws Exception
    {
        if (chat.getMessage().trim().isEmpty()) // Yes, I'm sorry but these are actually sent out by some bot tools
        {
            con.disconnect( CHAT_INVALID_DISCONNECT_REASON );
            BungeeCord.getInstance().getPluginManager().callEvent( new SuspiciousPlayerBehaviourEvent( con, SuspiciousPlayerBehaviourEvent.Check.CHAT_EMPTY ) );
            return;
        }
        if (chat.getMessage().length() > 100) // Mojang limit, check on updates
        {
            con.disconnect( CHAT_INVALID_DISCONNECT_REASON );
            BungeeCord.getInstance().getPluginManager().callEvent( new SuspiciousPlayerBehaviourEvent( con, SuspiciousPlayerBehaviourEvent.Check.CHAT_TOO_LONG ) );
            return;
        }

        // Users can't chat if they were never on a server yet after joining
        // Users also would get kicked if the command is not handled by the bungee with an npe, but this stops some unneccessary cpu cycles
        if(con.getServer() == null)
        {
            con.disconnect( CHAT_INVALID_DISCONNECT_REASON );
            BungeeCord.getInstance().getPluginManager().callEvent( new SuspiciousPlayerBehaviourEvent( con, SuspiciousPlayerBehaviourEvent.Check.CHAT_TOO_EARLY ) );
            return;
        }

        ChatEvent chatEvent = new ChatEvent( con, con.getServer(), chat.getMessage() );
        if ( !bungee.getPluginManager().callEvent( chatEvent ).isCancelled() )
        {
            chat.setMessage( chatEvent.getMessage() );
            if ( !chatEvent.isCommand() || !bungee.getPluginManager().dispatchCommand( con, chat.getMessage().substring( 1 ) ) )
            {
                con.getServer().unsafe().sendPacket( chat );
            }
        }
        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(TabCompleteRequest tabComplete) throws Exception
    {
        if ( bungee.getConfig().getTabCompleteThrottle() > 0 )
        {
            long now = System.currentTimeMillis();
            if ( lastTabComplete != -1 && ( now - lastTabComplete ) <= bungee.getConfig().getTabCompleteThrottle() )
            {
                throw CancelSendSignal.INSTANCE;
            }
            lastTabComplete = now;
        }
        List<String> suggestions = new ArrayList<>();

        if ( tabComplete.getCursor().startsWith( "/" ) )
        {
            bungee.getPluginManager().dispatchCommand( con, tabComplete.getCursor().substring( 1 ), suggestions );
        }

        TabCompleteEvent tabCompleteEvent = new TabCompleteEvent( con, con.getServer(), tabComplete.getCursor(), suggestions );
        bungee.getPluginManager().callEvent( tabCompleteEvent );

        if ( tabCompleteEvent.isCancelled() )
        {
            throw CancelSendSignal.INSTANCE;
        }

        List<String> results = tabCompleteEvent.getSuggestions();
        if ( !results.isEmpty() )
        {
            con.unsafe().sendPacket( new TabCompleteResponse( results ) );
            throw CancelSendSignal.INSTANCE;
        }
    }

    @Override
    public void handle(ClientSettings settings) throws Exception
    {
        con.setSettings( settings );
    }

    @Override
    public void handle(PluginMessage pluginMessage) throws Exception
    {
        if ( pluginMessage.getTag().equals( "BungeeCord" ) )
        {
            throw CancelSendSignal.INSTANCE;
        }
        // Hack around Forge race conditions
        if ( pluginMessage.getTag().equals( "FML" ) && pluginMessage.getStream().readUnsignedByte() == 1 )
        {
            throw CancelSendSignal.INSTANCE;
        }

        // We handle forge handshake messages if forge support is enabled.
        if ( pluginMessage.getTag().equals( ForgeConstants.FML_HANDSHAKE_TAG ) )
        {
            // Let our forge client handler deal with this packet.
            con.getForgeClientHandler().handle( pluginMessage );
            throw CancelSendSignal.INSTANCE;
        }

        if ( con.getServer() != null && !con.getServer().isForgeServer() && pluginMessage.getData().length > Short.MAX_VALUE )
        {
            // Drop the packet if the server is not a Forge server and the message was > 32kiB (as suggested by @jk-5)
            // Do this AFTER the mod list, so we get that even if the intial server isn't modded.
            throw CancelSendSignal.INSTANCE;
        }

        PluginMessageEvent event = new PluginMessageEvent( con, con.getServer(), pluginMessage.getTag(), pluginMessage.getData().clone() );
        if ( bungee.getPluginManager().callEvent( event ).isCancelled() )
        {
            throw CancelSendSignal.INSTANCE;
        }

        // TODO: Unregister as well?
        if ( pluginMessage.getTag().equals( "REGISTER" ) )
        {
            con.getPendingConnection().getRegisterMessages().add( pluginMessage );
        }
    }

    @Override
    public String toString()
    {
        return '[' + con.getAddress().toString() + '|' + con.getName() + "] -> UpstreamBridge";
    }
}
