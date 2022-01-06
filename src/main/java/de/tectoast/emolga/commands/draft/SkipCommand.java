package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.draft.Draft;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static de.tectoast.emolga.utils.draft.Draft.getIndex;

public class SkipCommand extends Command {

    public SkipCommand() {
        super("skip", "Überspringt deinen Zug", CommandCategory.Draft, Constants.ASLID);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String msg = e.getMsg();
        TextChannel tco = e.getChannel();
        Member member = e.getMember();
        Draft d = Draft.getDraftByMember(member, tco);
        if (d == null) {
            //tco.sendMessage(member.getAsMention() + " Du bist in keinem Draft drin!").queue();
            return;
        }
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9").getJSONObject(d.name);
        if (!d.tc.getId().equals(tco.getId())) return;
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird !skip nicht unterstützt!");
            return;
        }
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        try {
            d.cooldown.cancel();
        } catch (Exception ignored) {

        }
        int round = d.round;
        if (d.order.get(d.round).size() == 0) {
            if (d.round == 4) {
                tco.sendMessage("Der Draft ist vorbei!").queue();
                d.ended = true;
                //wooloodoc(tierlist, pokemon, d, mem, needed, null, num, round);
                if (d.afterDraft.size() > 0)
                    tco.sendMessage("Reihenfolge zum Nachdraften:\n" + d.afterDraft.stream().map(d::getMention).collect(Collectors.joining("\n"))).queue();
                saveEmolgaJSON();
                Draft.drafts.remove(d);
                return;
            }
            d.round++;
            d.tc.sendMessage("Runde " + d.round + "!").queue();
            league.put("round", d.round);
        }
        System.out.println("d.order = " + d.order);
        System.out.println("d.round = " + d.round);
        d.current = d.order.get(d.round).remove(0);
        league.put("current", d.current.getId());
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9");
        tco.sendMessage(d.getMention(d.current) + " (<@&" + asl.getLongList("roleids").get(getIndex(d.current.getIdLong())) + ">) ist dran! (" + d.points.get(d.current.getIdLong()) + " mögliche Punkte)").queue();
        try {
            d.cooldown.cancel();
        } catch (Exception ignored) {
        }
        d.cooldown = new Timer();
        long delay = calculateASLTimer();
        league.put("cooldown", System.currentTimeMillis() + delay);
        d.cooldown.schedule(new TimerTask() {
            @Override
            public void run() {
                d.timer();
            }
        }, delay);
        saveEmolgaJSON();
    }
}