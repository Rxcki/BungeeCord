package me.minotopia.flexpipe.api.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

@Getter
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class SuspiciousPlayerBehaviourEvent extends Event
{
    private final Connection connection;
    private final Check checkFailed;

    @Getter
    @RequiredArgsConstructor
    public enum Check
    {
        CHAT_TOO_EARLY( Connection.class ),
        CHAT_EMPTY( ProxiedPlayer.class ),
        CHAT_TOO_LONG( ProxiedPlayer.class ),
        JOIN_THROTTLE_TRIGGERED( PendingConnection.class );

        /**
         * The class you can safely cast the connection to without any instanceof check.
         */
        private final Class<? extends Connection> connectionClass;
    }
}
