package me.kiriyaga.nami.event.impl;

import me.kiriyaga.nami.event.Event;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Copyright (c) Meteor Development.
 *
 * @implNote Shouldn't be pooled or put in ThreadLocal to avoid race conditions.
 */
public class ChunkDataEvent extends Event {
    private final WorldChunk chunk;

    public ChunkDataEvent(WorldChunk chunk) {
        this.chunk = chunk;
    }

    public WorldChunk chunk() {
        return chunk;
    }
}
