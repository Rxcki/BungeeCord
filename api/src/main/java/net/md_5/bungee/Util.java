package net.md_5.bungee;

import com.google.common.base.Joiner;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Series of utility classes to perform various operations.
 */
public class Util
{

    public static final int DEFAULT_PORT = 25565;

    /**
     * Method to transform human readable addresses into usable address objects.
     *
     * @param hostline in the format of 'host:port'
     * @return the constructed hostname + port.
     */
    public static InetSocketAddress getAddr(String hostline)
    {
        URI uri;
        try
        {
            uri = new URI( "tcp://" + hostline );
        } catch ( URISyntaxException ex )
        {
            throw new IllegalArgumentException( "Bad hostline", ex );
        }

        return new InetSocketAddress( uri.getHost(), ( uri.getPort() ) == -1 ? DEFAULT_PORT : uri.getPort() );
    }

    /**
     * Formats an integer as a hex value.
     *
     * @param i the integer to format
     * @return the hex representation of the integer
     */
    public static String hex(int i)
    {
        return String.format( "0x%02X", i );
    }

    /**
     * Constructs a pretty one line version of a {@link Throwable}. Useful for
     * debugging.
     *
     * @param t the {@link Throwable} to format.
     * @return a string representing information about the {@link Throwable}
     */
    public static String exception(Throwable t)
    {
        // TODO: We should use clear manually written exceptions
        StackTraceElement[] trace = t.getStackTrace();
        return t.getClass().getSimpleName() + " : " + t.getMessage()
                + ( ( trace.length > 0 ) ? " @ " + t.getStackTrace()[0].getClassName() + ":" + t.getStackTrace()[0].getLineNumber() : "" );
    }

    public static String csv(Iterable<?> objects)
    {
        return format( objects, ", " );
    }

    public static String format(Iterable<?> objects, String separators)
    {
        return Joiner.on( separators ).join( objects );
    }

    /**
     * Converts a String to a UUID
     *
     * @param uuid The string to be converted
     * @return The result
     */
    public static UUID getUUID(String uuid)
    {
        return new UUID( Long.parseUnsignedLong( uuid.substring( 0, 16 ), 16 ), Long.parseUnsignedLong( uuid.substring( 16 ), 16 ) );
    }

    /**
     * Converts a UUID to a Mojang UUID
     *
     * @param uuid The string to be converted
     * @return The result
     */
    @SuppressWarnings( "Duplicates" )
    public static String getMojangUUID(UUID uuid)
    {
        // return toHexBits( uuid.getMostSignificantBits() ) + toHexBits( uuid.getLeastSignificantBits() );
        // inlined method and Strings.padStart() for optimization with StringBuilder - saves 1/3 up to 1/2 time

        StringBuilder stringBuilder = new StringBuilder( 32 );

        UUIDUtil.padStartMax16Chars( stringBuilder, UUIDUtil.toUnsignedStringRadix16_chararray( uuid.getMostSignificantBits() ) );

        // Add missing leading zeros (if necessary)
        UUIDUtil.padStartMax16Chars( stringBuilder, UUIDUtil.toUnsignedStringRadix16_chararray( uuid.getLeastSignificantBits()) );

        return stringBuilder.toString();
    }

    /**
     * provides optimized methods of library methods for creating a mojang uuid only due to removed checks and fixed values
     *
     * yeah this is micro-optimization to the largest extend
     */
    private static final class UUIDUtil
    {
        /**
         * pads a string infront with a '0' to achieve its 16 chars long, make sure the string is not longer than 16 chars or it won't work
         *
         * @param sb       the string builder to append the result to
         * @param toPrefix the string to optionally pad and
         * @see com.google.common.base.Strings#padStart(String, int, char) Strings.padStart(toPrefix, 16, '0')
         */
        private static void padStartMax16Chars(StringBuilder sb, char[] toPrefix)
        {
            // Add missing leading zeros (if necessary)
            if ( toPrefix.length != 16 )
            {
                for ( int i = toPrefix.length; i < 16; i++ )
                {
                    sb.append( '0' );
                }
            }
            sb.append( toPrefix );
        }

        //internal java methods simplified

        private static char[] toUnsignedStringRadix16_chararray(long i)
        {
            if ( i >= 0 )
            {
                return longToStringRadix16NotNegative( i );
            } else
            {
                return longToHexString( i );
            }
        }

        private static final char[] integer_digits = {
            '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
        };

        private static char[] longToHexString(long i)
        {
            // Long.toUnsignedString0(i, 4)
            int mag = Long.SIZE - Long.numberOfLeadingZeros( i );
            int chars = Math.max( ( ( mag + ( 4 - 1 ) ) / 4 ), 1 );
            char[] buf = new char[ chars ];

            formatUnsignedLong( i, 4, buf, chars );
            return buf;
        }

        /**
         * Format a long (treated as unsigned) into a character buffer.
         *
         * @param val   the unsigned long to format
         * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
         * @param buf   the character buffer to write to
         * @param len   the number of characters to write
         * @return the lowest character location used
         */
        private static int formatUnsignedLong(long val, int shift, char[] buf, int len)
        {
            int charPos = len;
            int radix = 1 << shift;
            int mask = radix - 1;
            do
            {
                buf[ --charPos ] = integer_digits[ ( ( int ) val ) & mask ];
                val >>>= shift;
            } while ( val != 0 && charPos > 0 );

            return charPos;
        }

        private static char[] longToStringRadix16NotNegative(long i)
        {
            int radix = 16;
            char[] buf = new char[ 65 ];
            int charPos = 64;

            i = -i;

            while ( i <= -radix )
            {
                buf[ charPos-- ] = integer_digits[ ( int ) ( -( i % radix ) ) ];
                i = i / radix;
            }
            buf[ charPos ] = integer_digits[ ( int ) ( -i ) ];

            return Arrays.copyOfRange( buf, charPos, 65 );
        }
    }
}
