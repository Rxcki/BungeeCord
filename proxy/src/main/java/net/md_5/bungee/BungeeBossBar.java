package net.md_5.bungee;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
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
    private boolean visible = false;
    private final Set<ProxiedPlayer> players = Collections.newSetFromMap( new WeakHashMap<ProxiedPlayer, Boolean>() );
    private final EnumSet<BossBarFlag> flags = EnumSet.noneOf( BossBarFlag.class );

    @ToString.Exclude
    private final UUID uuid = UUID.randomUUID();

    @ToString.Exclude
    private final BossBar addPacket = new BossBar( uuid, 0 );
    @ToString.Exclude
    private final BossBar removePacket = new BossBar( uuid, 1 );
    @ToString.Exclude
    private final BossBar healthUpdatePacket = new BossBar( uuid, 2 );
    @ToString.Exclude
    private final BossBar titleUpdatePacket = new BossBar( uuid, 3 );
    @ToString.Exclude
    private final BossBar styleUpdatePacket = new BossBar( uuid, 4 );
    @ToString.Exclude
    private final BossBar flagUpdatePacket = new BossBar( uuid, 5 );




    public BungeeBossBar(BaseComponent[] title, BossBarColor color, BossBarDivision division, float health)
    {
        setTitle( title );
        setColor( color );
        setDivision( division );
        setHealth( health );
    }

    @Override
    public void addPlayer(ProxiedPlayer player)
    {
        Preconditions.checkNotNull( player, "player" );
        if ( players.add( player ) && visible ) // order is important
        {
            sendPacket( player, addPacket() );
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
        if ( players.remove( player ) && visible ) // order is important
        {
            sendPacket( player, removePacket() );
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
        sendToAffected( removePacket() );
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
            sendToAffected( titleUpdatePacket() );
        }
    }

    @Override
    public void setHealth(float health)
    {
        Preconditions.checkArgument( 0 <= health && health <= 1, "health may not be lower than 0 or greater than 1" );
        float prevHealth = this.health;
        this.health = health;
        if ( prevHealth != health && visible )
        {
            sendToAffected( healthUpdatePacket() );
        }
    }

    @Override
    public void setColor(BossBarColor color)
    {
        BossBarColor prevColor = this.color;
        this.color = Preconditions.checkNotNull( color, "color" );
        if ( prevColor != color && visible )
        {
            sendToAffected( styleUpdatePacket() );
        }
    }

    @Override
    public void setDivision(BossBarDivision division)
    {
        final BossBarDivision prevDivision = this.division;
        this.division = Preconditions.checkNotNull( division, "division" );
        if ( prevDivision != division && visible )
        {
            sendToAffected( styleUpdatePacket() );
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
    public boolean addFlag(BossBarFlag flag)
    {
        if ( this.flags.add( flag ) )
        {
            if ( visible )
            {
                sendToAffected( flagUpdatePacket() );
            }
            return true;
        }
        return false;
    }

    @Override
    public void addFlags(BossBarFlag... flags)
    {
        if ( this.flags.addAll( Arrays.asList( flags ) ) && visible )
        {
            sendToAffected( flagUpdatePacket() );
        }
    }

    @Override
    public boolean removeFlag(BossBarFlag flag)
    {
        if ( this.flags.remove( flag ) )
        {
            if ( this.visible )
            {
                sendToAffected( flagUpdatePacket() );
            }
            return true;
        }
        return false;
    }

    @Override
    public void removeFlags(BossBarFlag... flags)
    {
        if ( this.flags.removeAll( Arrays.asList( flags ) ) && visible )
        {
            sendToAffected( flagUpdatePacket() );
        }
    }

    @Override
    public boolean canSeeBossbars(ProxiedPlayer player)
    {
        return player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_9;
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

    private BossBar addPacket()
    {
        addPacket.setTitle( ComponentSerializer.toString( title ) );
        addPacket.setColor( color.ordinal() );
        addPacket.setDivision( division.ordinal() );
        addPacket.setHealth( health );
        addPacket.setFlags( serializeFlags() );
        return addPacket;
    }

    private BossBar removePacket()
    {
        return removePacket;
    }

    private BossBar healthUpdatePacket()
    {
        healthUpdatePacket.setHealth( health );
        return healthUpdatePacket;
    }

    private BossBar titleUpdatePacket()
    {
        titleUpdatePacket.setTitle( ComponentSerializer.toString( title ) );
        return titleUpdatePacket;
    }

    private BossBar styleUpdatePacket()
    {
        styleUpdatePacket.setColor( color.ordinal() );
        styleUpdatePacket.setDivision( division.ordinal() );
        return styleUpdatePacket;
    }

    private BossBar flagUpdatePacket()
    {
        flagUpdatePacket.setFlags( serializeFlags() );
        return flagUpdatePacket;
    }

    /**
     * Sends packet to all added players if connected
     *
     * @param packet packet to send
     */
    private void sendToAffected(DefinedPacket packet)
    {
        for ( ProxiedPlayer player : players )
        {
            sendPacket( player, packet );
        }
    }

    /**
     * Send packet if player connected
     *
     * @param player player
     * @param packet packet
     */
    private void sendPacket(ProxiedPlayer player, DefinedPacket packet)
    {
        if ( canSeeBossbars( player ) && player.isConnected() )
        {
            player.unsafe().sendPacket( packet );
        }
    }
}
