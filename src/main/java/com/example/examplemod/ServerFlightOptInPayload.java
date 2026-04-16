package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ServerFlightOptInPayload(boolean allowed) implements CustomPacketPayload {
    public static final Type<ServerFlightOptInPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CreativeFlyMod.MODID, "server_flight_opt_in"));

    public static final StreamCodec<FriendlyByteBuf, ServerFlightOptInPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.allowed),
            buffer -> new ServerFlightOptInPayload(buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
