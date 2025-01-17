package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import org.jsolf.JSONObject;

public class EmolgaDiktaturCommand extends Command {
    public EmolgaDiktaturCommand() {
        super("emolgadiktatur", "EMOLGADIKTATUR", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Guild g = e.getGuild();
        JSONObject members = new JSONObject();
        JSONObject channels = new JSONObject();
        e.getChannel().sendMessage("**Möge die Emolga-Diktatur beginnen!**").queue();
        g.loadMembers().onSuccess(list -> {
            for (Member member : list) {
                if (member.isOwner()) continue;
                if (member.getId().equals(e.getJDA().getSelfUser().getId())) member.modifyNickname("Diktator").queue();
                if (!g.getSelfMember().canInteract(member)) continue;
                members.put(member.getId(), member.getEffectiveName());
                member.modifyNickname("Emolga-Anhänger").queue();
            }
            for (GuildChannel gc : g.getChannels()) {
                gc.getManager().setName("Emolga-" + gc.getName()).queue();
            }
            getEmolgaJSON().getJSONObject("emolgareset").put(g.getId(), members);
            saveEmolgaJSON();
        });
    }
}
