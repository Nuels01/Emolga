package de.tectoast.emolga.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import static de.tectoast.emolga.utils.Constants.FLOID;

public class GuildCommandEvent extends GenericCommandEvent {
    private final Member member;
    private final Guild guild;
    private final TextChannel tco;
    private final GuildMessageReceivedEvent event;
    private final String usedName;
    private Command.ArgumentManager manager;

    public GuildCommandEvent(Command c, GuildMessageReceivedEvent e) throws Exception {
        super(e.getMessage());
        this.member = e.getMember();
        this.guild = e.getGuild();
        this.tco = e.getChannel();
        event = e;
        Command.ArgumentManagerTemplate template = c.getArgumentTemplate();
        if (template != null)
            this.manager = template.construct(e);
        this.usedName = getMsg().split("\\s+")[0].substring(c.getPrefix().length());
        new Thread(() -> {
            try {
                c.process(this);
            } catch (Exception ex) {
                ex.printStackTrace();
                tco.sendMessage("Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo (Flooo#2535).\n" + c.getHelp(e.getGuild()) + (member.getIdLong() == FLOID ? "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" : "")).queue();
            }
        }, "CMD " + c.getName()).start();
    }

    public GuildCommandEvent(Command c, SlashCommandEvent e) throws Exception {
        super(e);
        this.member = e.getMember();
        this.guild = e.getGuild();
        this.tco = e.getTextChannel();
        this.event = null;
        this.usedName = e.getName();
        Command.ArgumentManagerTemplate template = c.getArgumentTemplate();
        if(template != null)
            this.manager = template.construct(e);
        c.process(this);
    }


    public String getUsedName() {
        return usedName;
    }

    public Member getMember() {
        return member;
    }

    public Guild getGuild() {
        return guild;
    }

    public Command.ArgumentManager getArguments() {
        return manager;
    }

    public TextChannel getChannel() {
        return tco;
    }

    public GuildMessageReceivedEvent getEvent() {
        return event;
    }

    public void deleteMessage() {
        this.getMessage().delete().queue();
    }
}
