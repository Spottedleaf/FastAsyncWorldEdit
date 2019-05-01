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

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sk89q.worldedit.command.util.PermissionCondition;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.minecraft.command.Commands.literal;

public final class CommandWrapper {
    private CommandWrapper() {
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher, org.enginehub.piston.Command command) {
        ImmutableList.Builder<String> aliases = ImmutableList.builder();
        aliases.add(command.getName()).addAll(command.getAliases());
        for (String alias : aliases.build()) {
            LiteralArgumentBuilder<CommandSource> base = literal(alias)
                .executes(FAKE_COMMAND);
            if (command.getCondition().as(PermissionCondition.class)
                .filter(p -> p.getPermissions().size() > 0).isPresent()) {
                base.requires(requirementsFor(command));
            }
            dispatcher.register(base);
        }
    }

    public static final Command<CommandSource> FAKE_COMMAND = ctx -> {
        EntityPlayerMP player = ctx.getSource().asPlayer();
        if (player.world.isRemote()) {
            return 0;
        }
        return 1;
    };

    private static Predicate<CommandSource> requirementsFor(org.enginehub.piston.Command mapping) {
        return ctx -> {
            ForgePermissionsProvider permsProvider = ForgeWorldEdit.inst.getPermissionsProvider();
            return ctx.getEntity() instanceof EntityPlayerMP &&
                mapping.getCondition().as(PermissionCondition.class)
                    .map(PermissionCondition::getPermissions)
                    .map(Set::stream)
                    .orElseGet(Stream::empty)
                    .allMatch(perm -> permsProvider.hasPermission(
                        (EntityPlayerMP) ctx.getEntity(), perm
                    ));
        };
    }

}
