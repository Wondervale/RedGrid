package com.foxxite.RedGrid.commands;

import java.sql.SQLException;

import com.foxxite.RedGrid.RedGrid;
import com.foxxite.RedGrid.models.Channel;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;

public class ChannelArgument extends ArgumentResolver<CommandSender, Channel> {

    @Override
    @Async
    protected ParseResult<Channel> parse(
            Invocation<CommandSender> invocation,
            Argument<Channel> argument,
            String string
    ) {
        Channel channel = RedGrid.getInstance().getDatabaseManager().getChannelByName(string);

        if (channel == null) {
            return ParseResult.failure("Channel not found");
        }

        return ParseResult.success(channel);
    }

    @Override
    @Async
    public SuggestionResult suggest(
            Invocation<CommandSender> invocation,
            Argument<Channel> argument,
            SuggestionContext context
    ) {
        try {
            return RedGrid.getInstance()
                          .getDatabaseManager()
                          .getChannelDao()
                          .queryForAll()
                          .stream()
                          .map(Channel::getName)
                          .collect(SuggestionResult.collector());
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
