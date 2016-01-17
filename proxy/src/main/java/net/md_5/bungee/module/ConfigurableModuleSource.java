package net.md_5.bungee.module;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import lombok.Data;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;

@Data
public class ConfigurableModuleSource implements ModuleSource
{

    private String moduleSourceScheme;

    private String getModuleSource(ModuleSpec module, ModuleVersion version)
    {
        if ( moduleSourceScheme == null )
        {
            moduleSourceScheme = ProxyServer.getInstance().getConfigurationAdapter()
                .getString( "module_source_scheme", "http://ci.janmm14.de/job/public~server~FlexPipe/$build_number$/artifact/BungeeCord/module/$maven_module_name$/target/$jar_name$.jar" );
        }
        return moduleSourceScheme
            .replace( "$build_number$", version.getBuild() )
            .replace( "$maven_module_name$", module.getName().replace( '_', '-' ) )
            .replace( "$jar_name$", module.getName() );
    }

    @Override
    public void retrieve(ModuleSpec module, ModuleVersion version)
    {
        String moduleSource = getModuleSource( module, version );
        System.out.println( "Attempting to get module " + module.getName() + " b" + version.getBuild() + " from " + moduleSource );
        try
        {
            URL website = new URL( moduleSource );
            URLConnection con = website.openConnection();
            // 15 second timeout at various stages
            con.setConnectTimeout( 15000 );
            con.setReadTimeout( 15000 );


            Files.write( ByteStreams.toByteArray( con.getInputStream() ), module.getFile() );
            System.out.println( "Download complete" );
        } catch ( IOException ex )
        {
            System.out.println( "Failed to download: " + Util.exception( ex ) );
        }
    }
}
