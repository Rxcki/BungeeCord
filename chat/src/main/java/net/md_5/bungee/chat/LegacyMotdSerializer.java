package net.md_5.bungee.chat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;


/**
 * This class converts TextComponents (with all its subcomponents) to legacy text and reverse.
 * This is required for various pinging tools which can't properly handle json motds. It just
 * needs to handle TextComponents due to everything is wrapped in a Textcomponent due to a
 * minecraft client bug.
 *
 * This class should only be used for 1.7.x and 1.8.x clients pinging as for 1.9 the pinging
 * tools need to update as vanilla minecraft servers output json motds.
 */
public class LegacyMotdSerializer implements JsonDeserializer<TextComponent>, JsonSerializer<TextComponent>
{
    @Override
    public TextComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        TextComponent component = new TextComponent( ComponentSerializer.parse( json.toString() ) );

        //While this class restores function of various pinging tools, the default chat color turns to white for some
        //reason. This fixes it by setting the default color to GRAY, which is nearly the default color, which is
        //between GRAY and DARK_GRAY
        if (component.getColorRaw() == null)
        {
            component.setColor( ChatColor.GRAY );
        }
        return component;
    }

    @Override
    public JsonElement serialize(TextComponent src, Type typeOfSrc, JsonSerializationContext context)
    {
        if (src.getColorRaw() == null)
        {
            src.setColor( ChatColor.GRAY );
        }

        return new JsonPrimitive( BaseComponent.toLegacyText( src ) );
    }
}
