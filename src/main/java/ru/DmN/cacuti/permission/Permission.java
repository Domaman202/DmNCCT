package ru.DmN.cacuti.permission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static ru.DmN.cacuti.Main.permissions;

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

    public static void addPermission(String name, String parent, String prefix) {
        for (var permission : permissions)
            if (permission.name.equals(name))
                return;
        permissions.add(new Permission(name, parent, prefix));
    }

    public static boolean checkAccess(String user, String command) {
        for (var permission : permissions)
            if (checkAccess(user, command, permission, permissions, new ArrayList<>(), false))
                return true;
        return false;
    }

    public static boolean checkAccess(String user, String command, Permission permission, Set<Permission> permissions, ArrayList<String> blacklist, boolean p) {
        if (p || permission.players.contains(user)) {
            if (checkAccess(command, permission))
                return true;
            if (!Objects.equals(permission.parent, "_"))
                for (var parent : permissions)
                    if (Objects.equals(permission.parent, parent.name) && !blacklist.contains(parent.name)) {
                        blacklist.add(parent.name);
                        return checkAccess(user, command, parent, permissions, blacklist, true);
                    }
        }
        return false;
    }

    public static boolean checkAccess(String command, Permission permission) {
        for (var cmd : permission.commands)
            if (command.startsWith(cmd))
                return true;
        return false;
    }

    public static String checkPrefix(String user, Set<Permission> permissions) {
        for (var permission : permissions)
            if (permission.players.contains(user))
                return permission.prefix;
        return null;
    }
}
