/**
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2008-2013, Sebastian Staudt
 */

package com.github.koraktor.steamcondenser.servers.packets;

import com.github.koraktor.steamcondenser.Helper;

/**
 * The A2S_INFO_Packet class represents a A2S_INFO request send to the server
 *
 * @author Sebastian Staudt
 * @see com.github.koraktor.steamcondenser.servers.GameServer#updateServerInfo
 */
public class A2S_INFO_Packet extends SteamPacket {

    /**
     * Creates a new A2S_INFO request object without a challenge
     */
    public A2S_INFO_Packet() {
        super(SteamPacket.A2S_INFO_HEADER, "Source Engine Query\0".getBytes());
    }

    /**
     * Creates a new A2S_INFO request object with a challenge
     */
    public A2S_INFO_Packet(int challengeNumber) {
        super(SteamPacket.A2S_INFO_HEADER, generateContentBytes(challengeNumber));
    }

    // this is the worst workaround i've ever had to make
    private static byte[] generateContentBytes(int challengeNumber)
    {
        byte[] payload = "Source Engine Query\0".getBytes();
        byte[] challenge = Helper.byteArrayFromInteger(Integer.reverseBytes(challengeNumber));
        byte[] destination = new byte[payload.length + challenge.length];

        System.arraycopy(payload, 0, destination, 0, payload.length);
        System.arraycopy(challenge, 0, destination, payload.length, challenge.length);

        return destination;
    }
}
