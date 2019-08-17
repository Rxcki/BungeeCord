package net.md_5.bungee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.boss.BossBarColor;
import net.md_5.bungee.api.boss.BossBarDivision;
import net.md_5.bungee.api.boss.BossBarFlag;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.BossBar;

@ToString
@EqualsAndHashCode
public class BungeeBossBar implements net.md_5.bungee.api.boss.BossBar
{

    @Getter
    private BaseComponent[] title;
    @Getter
    private BossBarColor color;
    @Getter
    private BossBarDivision division;
    @Getter
    private float health;
    @Getter
    private boolean visible;

    private List<ProxiedPlayer> players;
    private EnumSet<BossBarFlag> flags;

    @ToString.Exclude
    private UUID uuid;

    public BungeeBossBar(BaseComponent[] title, BossBarColor color, BossBarDivision division, float health)
    {
        this.title = title;
        this.color = color;
        this.division = division;
        this.health = health;
        this.visible = true;
        this.players = new ArrayList<>();
        this.flags = EnumSet.noneOf( BossBarFlag.class );
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void addPlayer(ProxiedPlayer player)
    {
        Preconditions.checkNotNull( player, "player" );
        if ( !players.contains( player ) )
        {
            players.add( player );
        }
        if ( player.isConnected() && visible )
        {
            player.unsafe().sendPacket( addPacket() );
        }
    }

    @Override
    public void addPlayers(Iterable<ProxiedPlayer> players)
    {
        Preconditions.checkNotNull( players, "players" );
        for ( ProxiedPlayer player : players )
        {
            addPlayer( player );
        }
    }

    @Override
    public void removePlayer(ProxiedPlayer player)
    {
        Preconditions.checkNotNull( player, "player" );
        players.remove( player );
        if ( player.isConnected() && visible )
        {
            player.unsafe().sendPacket( removePacket() );
        }
    }

    @Override
    public void removePlayers(Iterable<ProxiedPlayer> players)
    {
        Preconditions.checkNotNull( players, "players" );
        for ( ProxiedPlayer player : players )
        {
            removePlayer( player );
        }
    }

    @Override
    public void removeAllPlayers()
    {
        removePlayers( players );
        players.clear();
    }

    @Override
    public Collection<ProxiedPlayer> getPlayers()
    {
        return ImmutableList.copyOf( players );
    }

    @Override
    public void setTitle(BaseComponent[] title)
    {
        this.title = Preconditions.checkNotNull( title, "title" );
        if ( visible )
        {
            BossBar packet = new BossBar( uuid, 3 );
            packet.setTitle( ComponentSerializer.toString( title ) );
            sendToAffected( packet );
        }
    }

    @Override
    public void setHealth(float health)
    {
        Preconditions.checkArgument( health < 1 && health > 0, "Health must not be under 0 and after 1" );
        this.health = health;
        if ( visible )
        {
            BossBar packet = new BossBar( uuid, 2 );
            packet.setHealth( health );
            sendToAffected( packet );
        }
    }

    @Override
    public void setColor(BossBarColor color)
    {
        this.color = Preconditions.checkNotNull( color, "color" );
        if ( visible )
        {
            setDivisions( color, division );
        }
    }

    @Override
    public void setDivision(BossBarDivision division)
    {
        this.division = Preconditions.checkNotNull( division, "division" );
        if ( visible )
        {
            setDivisions( color, division );
        }
    }

    @Override
    public void setVisible(boolean visible)
    {
        boolean previous = this.visible;
        if ( previous && !visible )
        {
            sendToAffected( removePacket() );
        } else if ( !previous && visible )
        {
            sendToAffected( addPacket() );
        }
        this.visible = visible;
    }

    @Override
    public Collection<BossBarFlag> getFlags()
    {
        return ImmutableList.copyOf( flags );
    }

    @Override
    public void addFlags(BossBarFlag... flags)
    {
        if ( this.flags.addAll( Arrays.asList( flags ) ) && visible )
        {
            sendToAffected( updateFlags() );
        }
    }

    @Override
    public void removeFlag(BossBarFlag flag)
    {
        if ( flags.remove( flag ) && visible )
        {
            sendToAffected( updateFlags() );
        }
    }

    @Override
    public void removeFlags(BossBarFlag... flags)
    {
        if ( this.flags.removeAll( Arrays.asList( flags ) ) && visible )
        {
            sendToAffected( updateFlags() );
        }
    }

    private byte serializeFlags()
    {
        byte flagMask = 0x0;
        if ( flags.contains( BossBarFlag.DARKEN_SCREEN ) )
        {
            flagMask |= 0x1;
        }
        if ( flags.contains( BossBarFlag.PLAY_BOSS_MUSIC ) )
        {
            flagMask |= 0x2;
        }
        if ( flags.contains( BossBarFlag.CREATE_WORLD_FOG ) )
        {
            flagMask |= 0x4;
        }
        return flagMask;
    }

    private void setDivisions(BossBarColor color, BossBarDivision division)
    {
        BossBar packet = new BossBar( uuid, 4 );
        packet.setColor( color.ordinal() );
        packet.setDivision( division.ordinal() );
        sendToAffected( packet );
    }

    private BossBar updateFlags()
    {
        BossBar packet = new BossBar( uuid, 5 );
        packet.setFlags( serializeFlags() );
        return packet;
    }

    private BossBar addPacket()
    {
        BossBar packet = new BossBar( uuid, 0 );
        packet.setTitle( ComponentSerializer.toString( title ) );
        packet.setColor( color.ordinal() );
        packet.setDivision( division.ordinal() );
        packet.setHealth( health );
        packet.setFlags( serializeFlags() );
        return packet;
    }

    private void sendToAffected(DefinedPacket packet)
    {
        for ( ProxiedPlayer player : players )
        {
            if ( player.isConnected() && visible )
            {
                sendPacket( player, packet );
            }
        }
    }

    private void sendPacket(ProxiedPlayer player, DefinedPacket packet)
    {
        if ( player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_9 )
        {
            player.unsafe().sendPacket( packet );
        }
    }

    private BossBar removePacket()
    {
        return new BossBar( uuid, 1 );
    }
}
