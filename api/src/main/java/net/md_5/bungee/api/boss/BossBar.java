package net.md_5.bungee.api.boss;

import java.util.Collection;
import java.util.EnumSet;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public interface BossBar
{

    void addPlayer(ProxiedPlayer player);

    void addPlayers(Iterable<ProxiedPlayer> players);

    void removePlayer(ProxiedPlayer player);

    void removePlayers(Iterable<ProxiedPlayer> players);

    void removeAllPlayers();

    Collection<ProxiedPlayer> getPlayers();

    BaseComponent[] getTitle();

    void setTitle(BaseComponent[] title);

    float getHealth();

    void setHealth(float health);

    BossBarColor getColor();

    void setColor(BossBarColor color);

    BossBarDivision getDivision();

    void setDivision(BossBarDivision division);

    boolean isVisible();

    void setVisible(boolean visible);

    EnumSet<BossBarFlag> getFlags();

    void addFlags(BossBarFlag... flags);

    void removeFlag(BossBarFlag flag);

    void removeFlags(BossBarFlag... flags);

}
