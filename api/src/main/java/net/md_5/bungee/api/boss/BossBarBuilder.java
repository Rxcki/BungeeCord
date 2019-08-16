package net.md_5.bungee.api.boss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import com.google.common.base.Preconditions;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Represents a builder of {@link BossBar}
 */
public final class BossBarBuilder
{

    private BaseComponent[] title;
    private BossBarColor color;
    private BossBarDivision division;
    private float health;
    private Collection<ProxiedPlayer> players;
    private BossBarFlag[] flags;

    public BossBarBuilder()
    {
        this.title = new ComponentBuilder( "Title not specified" ).create();
        color = BossBarColor.PINK;
        division = BossBarDivision.SOLID;
        health = 1.0f;
        players = new ArrayList<>();
        flags = new BossBarFlag[0];
    }

    public BossBarBuilder title(BaseComponent[] title)
    {
        Preconditions.checkNotNull( title, "title" );
        this.title = title;
        return this;
    }

    public BossBarBuilder color(BossBarColor color)
    {
        this.color = color;
        return this;
    }

    public BossBarBuilder division(BossBarDivision division)
    {
        this.division = division;
        return this;
    }

    public BossBarBuilder health(float health)
    {
        this.health = health;
        return this;
    }

    public BossBarBuilder player(ProxiedPlayer player)
    {
        Preconditions.checkNotNull( player, "player" );
        players.add( player );
        return this;
    }

    public BossBarBuilder players(ProxiedPlayer... players)
    {
        Preconditions.checkNotNull( players, "players" );
        this.players.addAll( Arrays.asList( players ) );
        return this;
    }

    public BossBarBuilder flags(BossBarFlag... flags)
    {
        this.flags = flags;
        return this;
    }

    /**
     * Builds every set boss bar component into a {@link BossBar}
     *
     * @return boss bar
     */
    public BossBar build()
    {
        BossBar bossBar = ProxyServer.getInstance().createBossBar( title, color, division, health );
        if ( flags.length != 0 )
        {
            bossBar.addFlags( flags );
        }
        if ( players.size() != 0 )
        {
            bossBar.addPlayers( players );
        }
        return bossBar;
    }
}
