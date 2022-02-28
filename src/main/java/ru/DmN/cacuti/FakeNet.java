package ru.DmN.cacuti;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import org.jetbrains.annotations.Nullable;

public class FakeNet extends ClientConnection {
    public FakeNet(NetworkSide side) {
        super(side);
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
    }
}
