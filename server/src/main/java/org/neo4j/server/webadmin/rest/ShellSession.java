/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.webadmin.rest;

import java.rmi.RemoteException;

import org.neo4j.helpers.Pair;
import org.neo4j.helpers.Service;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.KernelExtension;
import org.neo4j.server.webadmin.console.ScriptSession;
import org.neo4j.shell.ShellClient;
import org.neo4j.shell.ShellException;
import org.neo4j.shell.ShellServer;
import org.neo4j.shell.impl.AbstractClient;
import org.neo4j.shell.impl.CollectingOutput;
import org.neo4j.shell.impl.SameJvmClient;
import org.neo4j.shell.impl.ShellServerExtension;

public class ShellSession implements ScriptSession
{
    private final ShellClient client;
    private final CollectingOutput output;
    
    public ShellSession( AbstractGraphDatabase graph )
    {
        ShellServerExtension shell = (ShellServerExtension) Service.load( KernelExtension.class, "shell" );
        if ( shell == null ) throw new UnsupportedOperationException( "Shell server not found" );
        ShellServer server = shell.getShellServer( graph.getKernelData() );
        if ( server == null ) throw new IllegalStateException( "Shell server null" );
        try
        {
            output = new CollectingOutput();
            client = new SameJvmClient( server, output );
            output.asString();
        }
        catch ( RemoteException e )
        {
            throw new RuntimeException( "Unable to start shell client", e );
        }

//        try
//        {
//            output = new CollectingOutput();
//            client = new RemoteClient( RmiLocation.location( "localhost",
//                    AbstractServer.DEFAULT_PORT, AbstractServer.DEFAULT_NAME ), output );
//        }
//        catch ( ShellException e )
//        {
//            throw new RuntimeException( "Unable to start shell client", e );
//        }
//        catch ( RemoteException e )
//        {
//            throw new RuntimeException( "Unable to start shell client", e );
//        }
    }

    @Override
    public Pair<String, String> evaluate( String script )
    {
        if ( script.equals( "init()" ) ) return Pair.of( "", client.getPrompt() );
        if ( script.equals( "exit" ) || script.equals( "quit" ) ) return Pair.of( "No you don't", client.getPrompt() );
        try
        {
            client.evaluate( removeFirstEnter( script ) );
            return Pair.of( output.asString(), client.getPrompt() );
        }
        catch ( ShellException e )
        {
            String message = ((AbstractClient)client).shouldPrintStackTraces() ?
                    ShellException.stackTraceAsString( e ) : ShellException.getFirstMessage( e );
            return Pair.of( message, client.getPrompt() );
        }
    }

    private String removeFirstEnter( String script )
    {
        return script != null && script.startsWith( "\n" ) ? script.substring( 1 ) : script;
    }
}
