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

    /**
     * Create a fresh boss bar builder
     */
    public BossBarBuilder()
    {
        initDefault();
    }

    /**
     * Creates a BossBarBuilder with the given BossBarBuilder to clone
     * it.
     *
     * @param original original builder
     */
    public BossBarBuilder(BossBarBuilder original)
    {
        initCopy( original );
    }

    /**
     * Creates a boss bar builder with the specified title
     *
     * @param title the boss bar title you wish to create a boss bar builder with
     */
    public BossBarBuilder(BaseComponent[] title)
    {
        initTitle( title );
    }

    /**
     * Set the current boss bar's title
     *
     * @param title the title you wish to set
     * @return this BossBarBuilder for chaining
     */
    public BossBarBuilder title(BaseComponent[] title)
    {
        Preconditions.checkNotNull( title, "title" );
        this.title = title;
        return this;
    }

    /**
     * Set the current boss bar's color
     *
     * @param color the color you wish to set
     * @return this BossBarBuilder for chaining
     */
    public BossBarBuilder color(BossBarColor color)
    {
        this.color = color;
        return this;
    }

    /**
     * Set the current boss bar's division (style)
     *
     * @param division the division (style) you wish to set
     * @return this BossBarBuilder for chaining
     */
    public BossBarBuilder division(BossBarDivision division)
    {
        this.division = division;
        return this;
    }

    /**
     * Set the current boss bar's health (progress). The number specified should be
     * between 0 and 1 including.
     *
     * @param health the health (progress) you wish to set.
     * @return this BossBarBuilder for chaining
     */
    public BossBarBuilder health(float health)
    {
        this.health = health;
        return this;
    }

    /**
     * Adds a player to the boss bar.
     *
     * @param player the player you wish to add
     * @return this BossBarBuilder for chaining
     */
    public BossBarBuilder player(ProxiedPlayer player)
    {
        Preconditions.checkNotNull( player, "player" );
        players.add( player );
        return this;
    }

    /**
     * Adds the specified players to the boss bar
     *
     * @param players the players you wish to add
     * @return this BossBarBuilder for chaining
     */
    public BossBarBuilder players(ProxiedPlayer... players)
    {
        Preconditions.checkNotNull( players, "players" );
        this.players.addAll( Arrays.asList( players ) );
        return this;
    }

    /**
     * Adds the specified players to the boss bar
     *
     * @param players the players you wish to add
     * @return this BossBarBuilder for chaining
     */
    public BossBarBuilder players(Iterable<ProxiedPlayer> players)
    {
        for ( ProxiedPlayer player : players )
        {
            player( player );
        }
        return this;
    }

    /**
     * Adds the specified flag(s) to the boss bar.
     *
     * @param flags the flag(s) you wish to add
     * @return this BossBarBuilder for chaining
     */
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

    //
    private void initDefault()
    {
        this.title = new ComponentBuilder( "Title not specified" ).create();
        color = BossBarColor.PINK;
        division = BossBarDivision.SOLID;
        health = 1.0f;
        players = new ArrayList<>();
        flags = new BossBarFlag[0];
    }

    private void initTitle(BaseComponent[] title)
    {
        initDefault();
        this.title = title;
    }

    private void initCopy(BossBarBuilder builder)
    {
        this.title = builder.title;
        this.color = builder.color;
        this.division = builder.division;
        this.health = builder.health;
        this.players = builder.players;
        this.flags = builder.flags;
    }
    //
}
