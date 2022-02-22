package ru.DmN.cacuti;

import net.minecraft.entity.player.PlayerEntity;

import java.io.FileOutputStream;
import java.io.IOException;

public interface ISHAccess {
    FileOutputStream getDmN() throws IOException;
    PlayerEntity getDmN0();
}
