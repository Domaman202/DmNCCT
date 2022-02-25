package ru.DmN.cacuti.mixin_;

import net.minecraft.entity.player.PlayerEntity;

import java.io.FileOutputStream;
import java.io.IOException;

public interface ISHAccess {
    FileOutputStream getDmN() throws IOException;
    PlayerEntity getDmN0();
}
