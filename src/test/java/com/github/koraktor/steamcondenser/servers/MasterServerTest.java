/*
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2012-2020, Sebastian Staudt
 */

package com.github.koraktor.steamcondenser.servers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentMatcher;

import com.github.koraktor.steamcondenser.servers.packets.A2M_GET_SERVERS_BATCH2_Packet;
import com.github.koraktor.steamcondenser.servers.packets.M2A_SERVER_BATCH_Packet;
import com.github.koraktor.steamcondenser.servers.sockets.MasterServerSocket;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sebastian Staudt
 */
public class MasterServerTest {

    private MasterServer server;

    @Before
    public void setup() throws Exception {
        server = spy(new MasterServer(InetAddress.getByAddress(new byte[] { 0x7f, 0x0, 0x0, 0x1 }), 27015));
        server.socket = mock(MasterServerSocket.class);

        doReturn(true).when(server).rotateIp();
    }

    @Test
    public void testGetServers() throws Exception {
        M2A_SERVER_BATCH_Packet packet1 = mock(M2A_SERVER_BATCH_Packet.class);
        Vector<String> servers1 = new Vector<>();
        servers1.add("127.0.0.1:27015");
        servers1.add("127.0.0.2:27015");
        servers1.add("127.0.0.3:27015");
        when(packet1.getServers()).thenReturn(servers1);
        M2A_SERVER_BATCH_Packet packet2 = mock(M2A_SERVER_BATCH_Packet.class);
        Vector<String> servers2 = new Vector<>();
        servers2.add("127.0.0.4:27015");
        servers2.add("0.0.0.0:0");
        when(packet2.getServers()).thenReturn(servers2);
        when(server.socket.getReply()).thenReturn(packet1).thenReturn(packet2);

        Set<InetSocketAddress> servers = new HashSet<>();
        servers.add(new InetSocketAddress("127.0.0.1", 27015));
        servers.add(new InetSocketAddress("127.0.0.2", 27015));
        servers.add(new InetSocketAddress("127.0.0.3", 27015));
        servers.add(new InetSocketAddress("127.0.0.4", 27015));

        assertThat(server.getServers(MasterServer.REGION_EUROPE, "filter"), is(equalTo(servers)));

        verify(server.socket).
                send(argThat(matchesPacketBytes("\u0031\u00030.0.0.0:0\0filter\0")));
        verify(server.socket).
                send(argThat(matchesPacketBytes("\u0031\u0003127.0.0.3:27015\0filter\0")));
    }

    @Test
    public void testGetServersForced() throws Exception {
        MasterServer.setRetries(1);

        M2A_SERVER_BATCH_Packet packet1 = mock(M2A_SERVER_BATCH_Packet.class);
        Vector<String> servers1 = new Vector<>();
        servers1.add("127.0.0.1:27015");
        servers1.add("127.0.0.2:27015");
        servers1.add("127.0.0.3:27015");
        when(packet1.getServers()).thenReturn(servers1);
        when(server.socket.getReply()).thenReturn(packet1).thenThrow(new TimeoutException());

        Set<InetSocketAddress> servers = new HashSet<>();
        servers.add(new InetSocketAddress("127.0.0.1", 27015));
        servers.add(new InetSocketAddress("127.0.0.2", 27015));
        servers.add(new InetSocketAddress("127.0.0.3", 27015));

        assertThat(server.getServers(MasterServer.REGION_EUROPE, "filter", true), is(equalTo(servers)));

        verify(server.socket).
                send(argThat(matchesPacketBytes("\u0031\u00030.0.0.0:0\0filter\0")));
        verify(server.socket).
                send(argThat(matchesPacketBytes("\u0031\u0003127.0.0.3:27015\0filter\0")));
    }

    @Test
    public void testGetServersSwapIp() throws Exception {
        M2A_SERVER_BATCH_Packet packet1 = mock(M2A_SERVER_BATCH_Packet.class);
        Vector<String> servers1 = new Vector<>();
        servers1.add("127.0.0.1:27015");
        servers1.add("127.0.0.2:27015");
        servers1.add("127.0.0.3:27015");
        when(packet1.getServers()).thenReturn(servers1);
        M2A_SERVER_BATCH_Packet packet2 = mock(M2A_SERVER_BATCH_Packet.class);
        Vector<String> servers2 = new Vector<>();
        servers2.add("127.0.0.4:27015");
        servers2.add("0.0.0.0:0");
        when(packet2.getServers()).thenReturn(servers2);
        when(server.socket.getReply())
            .thenReturn(packet1)
            .thenThrow(new TimeoutException())
            .thenThrow(new TimeoutException())
            .thenThrow(new TimeoutException())
            .thenReturn(packet2);

        when(server.rotateIp()).thenReturn(false);

        Set<InetSocketAddress> servers = new HashSet<>();
        servers.add(new InetSocketAddress("127.0.0.1", 27015));
        servers.add(new InetSocketAddress("127.0.0.2", 27015));
        servers.add(new InetSocketAddress("127.0.0.3", 27015));
        servers.add(new InetSocketAddress("127.0.0.4", 27015));

        assertThat(server.getServers(MasterServer.REGION_EUROPE, "filter"), is(equalTo(servers)));

        verify(server, times(3)).rotateIp();
        verify(server.socket).
                send(argThat(matchesPacketBytes("\u0031\u00030.0.0.0:0\0filter\0")));
        verify(server.socket, times(4)).
                send(argThat(matchesPacketBytes("\u0031\u0003127.0.0.3:27015\0filter\0")));
    }

    @Test
    public void testGetServersTimeout() throws Exception {
        int retries = new Random().nextInt(4) + 1;
        MasterServer.setRetries(retries);

        when(server.socket.getReply()).thenThrow(new TimeoutException());

        try {
            server.getServers();
            fail();
        } catch (TimeoutException ignored) {}

        verify(server.socket, times(retries)).
                send(argThat(matchesPacketBytes(new byte[] { 0x31, (byte) 0xFF, 0x30, 0x2E, 0x30, 0x2E, 0x30, 0x2E, 0x30, 0x3a, 0x30, 0x00, 0x00 })));
    }

    private static A2M_GET_SERVERS_BATCH2_PacketMatcher matchesPacketBytes(String byteString) {
        return new A2M_GET_SERVERS_BATCH2_PacketMatcher(byteString.getBytes());
    }

    private static A2M_GET_SERVERS_BATCH2_PacketMatcher matchesPacketBytes(byte[] bytes) {
        return new A2M_GET_SERVERS_BATCH2_PacketMatcher(bytes);
    }

    private static class A2M_GET_SERVERS_BATCH2_PacketMatcher
            implements ArgumentMatcher<A2M_GET_SERVERS_BATCH2_Packet> {

        private final byte[] bytes;

        A2M_GET_SERVERS_BATCH2_PacketMatcher(byte[] bytes) {
            this.bytes =  bytes;
        }

        @Override
        public boolean matches(A2M_GET_SERVERS_BATCH2_Packet packet) {
            return Arrays.equals(packet.getBytes(), bytes);
        }
    }

}
