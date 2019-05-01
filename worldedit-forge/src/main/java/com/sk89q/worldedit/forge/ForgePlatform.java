/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.forge;

import com.sk89q.worldedit.command.util.PermissionCondition;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.MultiUserPlatform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.Registries;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.enginehub.piston.Command;
import org.enginehub.piston.CommandManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

class ForgePlatform extends AbstractPlatform implements MultiUserPlatform {

    private final ForgeWorldEdit mod;
    private final MinecraftServer server;
    private boolean hookingEvents = false;

    ForgePlatform(ForgeWorldEdit mod) {
        this.mod = mod;
        this.server = ServerLifecycleHooks.getCurrentServer();
    }

    boolean isHookingEvents() {
        return hookingEvents;
    }

    @Override
    public Registries getRegistries() {
        return ForgeRegistries.getInstance();
    }

    @Override
    public int getDataVersion() {
        // TODO switch to SharedConstants in 1.14
        return 1631;
    }

    @Override
    public boolean isValidMobType(String type) {
        return net.minecraftforge.registries.ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(type));
    }

    @Override
    public void reload() {
        getConfiguration().load();
    }

    @Override
    public int schedule(long delay, long period, Runnable task) {
        return -1;
    }

    @Override
    public List<? extends com.sk89q.worldedit.world.World> getWorlds() {
        Iterable<WorldServer> worlds = server.getWorlds();
        List<com.sk89q.worldedit.world.World> ret = new ArrayList<>();
        for (WorldServer world : worlds) {
            ret.add(new ForgeWorld(world));
        }
        return ret;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof ForgePlayer) {
            return player;
        } else {
            EntityPlayerMP entity = server.getPlayerList().getPlayerByUsername(player.getName());
            return entity != null ? new ForgePlayer(entity) : null;
        }
    }

    @Nullable
    @Override
    public World matchWorld(World world) {
        if (world instanceof ForgeWorld) {
            return world;
        } else {
            for (WorldServer ws : server.getWorlds()) {
                if (ws.getWorldInfo().getWorldName().equals(world.getName())) {
                    return new ForgeWorld(ws);
                }
            }

            return null;
        }
    }

    @Override
    public void registerCommands(CommandManager manager) {
        if (server == null) return;
        Commands mcMan = server.getCommandManager();

        for (Command command : manager.getAllCommands().collect(toList())) {
            CommandWrapper.register(mcMan.getDispatcher(), command);
            Set<String> perms = command.getCondition().as(PermissionCondition.class)
                .map(PermissionCondition::getPermissions)
                .orElseGet(Collections::emptySet);
            if (perms.size() > 0) {
                perms.forEach(ForgeWorldEdit.inst.getPermissionsProvider()::registerPermission);
            }
        }
    }

    @Override
    public void registerGameHooks() {
        // We registered the events already anyway, so we just 'turn them on'
        hookingEvents = true;
    }

    @Override
    public ForgeConfiguration getConfiguration() {
        return mod.getConfig();
    }

    @Override
    public String getVersion() {
        return mod.getInternalVersion();
    }

    @Override
    public String getPlatformName() {
        return "Forge-Official";
    }

    @Override
    public String getPlatformVersion() {
        return mod.getInternalVersion();
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.PREFER_OTHERS);
        capabilities.put(Capability.WORLDEDIT_CUI, Preference.NORMAL);
        capabilities.put(Capability.GAME_HOOKS, Preference.NORMAL);
        capabilities.put(Capability.PERMISSIONS, Preference.NORMAL);
        capabilities.put(Capability.USER_COMMANDS, Preference.NORMAL);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFERRED);
        return capabilities;
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        List<Actor> users = new ArrayList<>();
        PlayerList scm = server.getPlayerList();
        for (EntityPlayerMP entity : scm.getPlayers()) {
            if (entity != null) {
                users.add(new ForgePlayer(entity));
            }
        }
        return users;
    }
}
