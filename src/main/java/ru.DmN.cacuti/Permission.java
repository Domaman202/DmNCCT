package ru.DmN.cacuti;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class Permission implements Serializable {
    public final Set<String> players, commands;
    public String name, parent, prefix;

    public Permission(String name, String parent, String prefix) {
        this.name = name;
        this.parent = parent;
        this.prefix = prefix;
        this.players = new LinkedHashSet<>();
        this.commands = new LinkedHashSet<>();
    }
}
