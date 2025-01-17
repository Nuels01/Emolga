package de.tectoast.emolga.commands;

import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.Google;
import de.tectoast.emolga.utils.PrivateCommand;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.emolga.utils.records.Coord;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.entities.*;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;

public class PrivateCommands {

    private static final Logger logger = LoggerFactory.getLogger(PrivateCommands.class);

    @PrivateCommand(name = "updatetierlist")
    public static void updateTierlist(GenericCommandEvent e) {
        Tierlist.setup();
        e.reply("Die Tierliste wurde aktualisiert!");
    }

    @PrivateCommand(name = "checkguild")
    public static void checkGuild(GenericCommandEvent e) {
        e.reply(e.getJDA().getGuildById(e.getArg(0)).getName());
    }

    @PrivateCommand(name = "edit")
    public static void edit(GenericCommandEvent e) {
        e.getJDA().getTextChannelById(e.getArg(0)).editMessageById(e.getArg(1), e.getMsg().substring(43)).queue();
    }

    @PrivateCommand(name = "send")
    public static void send(GenericCommandEvent e) {
        Message message = e.getMessage();
        String[] split = message.getContentDisplay().split(" ");
        logger.info(message.getContentRaw());
        String s = message.getContentRaw().substring(24).replaceAll("\\\\", "");
        TextChannel tc = e.getJDA().getTextChannelById(e.getArg(0));
        Guild g = tc.getGuild();
        for (ListedEmote emote : g.retrieveEmotes().complete()) {
            s = s.replace("<<" + emote.getName() + ">>", emote.getAsMention());
        }
        tc.sendMessage(s).queue();
    }

    @PrivateCommand(name = "sendpn")
    public static void sendPN(GenericCommandEvent e) {
        Message message = e.getMessage();
        String[] split = message.getContentDisplay().split(" ");
        logger.info(message.getContentRaw());
        String s = message.getContentRaw().substring(26).replaceAll("\\\\", "");
        String userid = e.getArg(0);
        sendToUser(Long.parseLong(userid), s);
    }

    @PrivateCommand(name = "react")
    public static void react(GenericCommandEvent e) {
        String msg = e.getMsg();
        String s = msg.substring(45);
        TextChannel tc = e.getJDA().getTextChannelById(e.getArg(0));
        Message m = tc.retrieveMessageById(e.getArg(1)).complete();
        assert (m != null);
        if (s.contains("<")) {
            s = s.substring(1);
            logger.info("s = " + s);
            String finalS = s;
            tc.getGuild().retrieveEmotes().complete().stream().filter(emote -> emote.getName().equalsIgnoreCase(finalS)).forEach(emote -> m.addReaction(emote).queue());
        } else {
            m.addReaction(s).queue();
        }
    }

    @PrivateCommand(name = "ban")
    public static void ban(GenericCommandEvent e) {
        e.getJDA().getGuildById(e.getArg(0)).ban(e.getArg(1), 0).queue();
    }

    @PrivateCommand(name = "banwithreason")
    public static void banwithreason(GenericCommandEvent e) {
        e.getJDA().getGuildById(e.getArg(0)).ban(e.getArg(1), 0, e.getMsg().substring(53)).queue();
    }

    @PrivateCommand(name = "updatedatabase")
    public static void updateDatabase(GenericCommandEvent e) {
        loadJSONFiles();
        e.done();
    }

    @PrivateCommand(name = "emolgajson", aliases = {"ej"})
    public static void emolgajson(GenericCommandEvent e) {
        emolgajson = load("./emolgadata.json");
        e.done();
    }

    @PrivateCommand(name = "del")
    public static void del(GenericCommandEvent e) {
        e.getJDA().getTextChannelById(e.getArg(0)).deleteMessageById(e.getArg(1)).queue();
    }

    @PrivateCommand(name = "sortzbs")
    public static void sortZBSCmd(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL" + e.getArg(0));
        sortZBS(league.getString("sid"), "Liga " + e.getArg(0), league);
    }

    @PrivateCommand(name = "sortwooloo")
    public static void sortWoolooCmd(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("WoolooCupS3L" + e.getArg(0));
        sortWooloo(league.getString("sid"), league);
    }

    @PrivateCommand(name = "troll")
    public static void troll(GenericCommandEvent e) {
        Category category = e.getJDA().getCategoryById(e.getArg(0));
        Guild g = category.getGuild();
        Member user = g.retrieveMemberById(e.getArg(1)).complete();
        ArrayList<VoiceChannel> list = new ArrayList<>(category.getVoiceChannels());
        Collections.shuffle(list);
        VoiceChannel old = user.getVoiceState().getChannel();
        list.remove(old);
        for (VoiceChannel voiceChannel : list) {
            g.moveVoiceMember(user, voiceChannel).queue();
            try {
                Thread.sleep(500);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
        g.moveVoiceMember(user, old).queue();
    }

    @PrivateCommand(name = "addreactions")
    public static void addReactions(GenericCommandEvent e) {
        Message message = e.getMessage();
        e.getJDA().getTextChannelById(e.getArg(0)).retrieveMessageById(e.getArg(1)).queue(m -> {
            m.getReactions().forEach(mr -> {
                MessageReaction.ReactionEmote emote = mr.getReactionEmote();
                if (emote.isEmoji()) m.addReaction(emote.getEmoji()).queue();
                else m.addReaction(emote.getEmote()).queue();
            });
            e.done();
        });
    }

    @PrivateCommand(name = "replaceplayer")
    public static void replacePlayer(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(e.getArg(0));
        JSONObject battleOrder = league.getJSONObject("battleorder");
        String oldid = e.getArg(1);
        String newid = e.getArg(2);
        for (int i = 1; i <= battleOrder.length(); i++) {
            battleOrder.put(String.valueOf(i), battleOrder.getString(String.valueOf(i)).replace(oldid, newid));
        }
        league.put("table", league.getString("table").replace(oldid, newid));
        JSONObject order = league.getJSONObject("order");
        for (int i = 1; i <= order.length(); i++) {
            order.put(i, order.getString(String.valueOf(i)).replace(oldid, newid));
        }
        JSONObject picks = league.getJSONObject("picks");
        picks.put(newid, picks.getJSONArray(oldid));
        picks.remove(oldid);
        if (league.has("results")) {
            JSONObject results = league.getJSONObject("results");
            ArrayList<String> keys = new ArrayList<>(results.keySet());
            for (String key : keys) {
                if (key.contains(oldid)) {
                    results.put(key.replace(oldid, newid), results.getString(key));
                    results.remove(key);
                }
            }
        }
        saveEmolgaJSON();
        e.done();
    }

    @PrivateCommand(name = "saveemolgajson")
    public static void saveEmolga(GenericCommandEvent e) {
        saveEmolgaJSON();
        e.done();
    }

    @PrivateCommand(name = "pdg")
    public static void pdg(GenericCommandEvent e) {
        evaluatePredictions(getEmolgaJSON().getJSONObject("drafts").getJSONObject(e.getArg(0)), Boolean.parseBoolean(e.getArg(1)), Integer.parseInt(e.getArg(2)),
                e.getArg(3), e.getArg(4));
        e.done();
    }

    @PrivateCommand(name = "incrpdg")
    public static void incrPdg(GenericCommandEvent e) {
        for (String arg : e.getArgs()) {
            Database.incrementPredictionCounter(Long.parseLong(arg));
        }
    }

    @PrivateCommand(name = "silentmove")
    public static void silentMove(GenericCommandEvent e) {
        VoiceChannel from = e.getJDA().getVoiceChannelById(e.getArg(0));
    }

    @PrivateCommand(name = "testvolume")
    public static void testVolume(GenericCommandEvent e) {
        logger.info("Start!");
        musicManagers.get(673833176036147210L).player.setVolume(Integer.parseInt(e.getArg(0)));
        logger.info("musicManagers.get(673833176036147210L).player.getVolume() = " + musicManagers.get(673833176036147210L).player.getVolume());
    }

    @PrivateCommand(name = "printcache")
    public static void printCache(GenericCommandEvent e) {
        translationsCacheGerman.forEach((str, t) -> {
            logger.info(str);
            t.print();
            logger.info("=====");
        });
        logger.info(String.valueOf(translationsCacheOrderGerman));
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>");
        translationsCacheEnglish.forEach((str, t) -> {
            logger.info(str);
            t.print();
            logger.info("=====");
        });
        logger.info(String.valueOf(translationsCacheOrderEnglish));
        e.done();
    }

    @PrivateCommand(name = "clearcache")
    public static void clearCache(GenericCommandEvent e) {
        translationsCacheGerman.clear();
        translationsCacheOrderGerman.clear();
        translationsCacheEnglish.clear();
        translationsCacheOrderEnglish.clear();
        e.done();
    }

    @PrivateCommand(name = "getguildbytc")
    public static void getGuildCmd(GenericCommandEvent e) {
        e.reply(e.getJDA().getTextChannelById(e.getArg(0)).getGuild().getName());
    }


    @PrivateCommand(name = "inviteme")
    public static void inviteMe(GenericCommandEvent e) {
        for (Guild guild : e.getJDA().getGuilds()) {
            try {
                guild.retrieveMemberById(Constants.FLOID).complete();
            } catch (Exception exception) {
                sendToMe(guild.getTextChannels().get(0).createInvite().complete().getUrl());
            }
        }
    }

    @PrivateCommand(name = "removeduplicates")
    public static void removeDuplicates(GenericCommandEvent e) {
        DBManagers.TRANSLATIONS.removeDuplicates();
    }

    @PrivateCommand(name = "ndsnominate")
    public static void ndsNominate(GenericCommandEvent e) {
        Draft.setupNDSNominate();
    }

    @PrivateCommand(name = "ndsprediction")
    public static void ndsPrediction(GenericCommandEvent e) {
        Draft.doNDSPredictionGame();
    }

    @PrivateCommand(name = "matchups")
    public static void matchUps(GenericCommandEvent e) {
        JSONObject nds = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS");
        RequestBuilder b = new RequestBuilder(nds.getString("sid"));
        String str = nds.getJSONObject("battleorder").getString(e.getArg(0));
        JSONObject teamnames = nds.getJSONObject("teamnames");
        for (String s : str.split(";")) {
            String[] split = s.split(":");
            String t1 = teamnames.getString(split[0]);
            String t2 = teamnames.getString(split[1]);
            b.addSingle(t1 + "!Q15", "=IMPORTRANGE(\"https://docs.google.com/spreadsheets/d/1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU/edit\"; \"" + t2 + "!B15:O29\")");
            b.addSingle(t1 + "!Q13", "=CONCAT(\"VS. Spieler \"; '" + t2 + "'!O2)");
            b.addSingle(t2 + "!Q15", "=IMPORTRANGE(\"https://docs.google.com/spreadsheets/d/1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU/edit\"; \"" + t1 + "!B15:O29\")");
            b.addSingle(t2 + "!Q13", "=CONCAT(\"VS. Spieler \"; '" + t1 + "'!O2)");
        }
        b.execute();
    }

    @PrivateCommand(name = "sortnds")
    public static void sortNDSCmd(GenericCommandEvent e) {
        sortNDS("1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU", getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS"));
        e.done();
    }

    @PrivateCommand(name = "ndskilllist")
    public static void ndskilllist(GenericCommandEvent e) {
        List<List<Object>> send = new LinkedList<>();
        String sid = "1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU";
        JSONObject nds = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS");
        JSONObject picks = nds.getJSONObject("picks");
        JSONObject teamnames = nds.getJSONObject("teamnames");
        //BufferedWriter writer = new BufferedWriter(new FileWriter("ndskilllistorder.txt"));
        for (String s : picks.keySet()) {
            List<String> mons = getPicksAsList(picks.getJSONArray(s));
            List<List<Object>> lists = Google.get(sid, "%s!B200:K%d".formatted(teamnames.getString(s), mons.size() + 199), false, false);
            for (int j = 0; j < mons.size(); j++) {
                send.add(lists.get(j));
            }
        }
        RequestBuilder.updateAll(sid, "Killliste!S1001", send);
    }


    @PrivateCommand(name = "ndsrr")
    public static void ndsrr(GenericCommandEvent e) {
        JSONObject lastNom = load("ndsdraft.json").getJSONObject("hinrunde").getJSONObject("nominations").getJSONObject("5");
        String sid = "1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU";
        //String sid = "1Lbeko-7ZFuuVon_qmgavDsht5JoWQPVk2TLMN6cCROo";
        String current = "";
        JSONObject nds = emolgajson.getJSONObject("drafts").getJSONObject("NDS");
        JSONObject allpicks = nds.getJSONObject("picks");
        JSONObject toid = nds.getJSONObject("nametoid");
        JSONObject teamnames = nds.getJSONObject("teamnames");
        RequestBuilder b = new RequestBuilder(sid);
        Tierlist tierlist = Tierlist.getByGuild(Constants.NDSID);
        HashMap<String, List<Integer>> currkills = new HashMap<>();
        HashMap<String, List<Integer>> currdeaths = new HashMap<>();
        HashMap<String, AtomicInteger> killstoadd = new HashMap<>();
        HashMap<String, AtomicInteger> deathstoadd = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            List<List<Object>> l = Google.get(sid, "RR Draft!%s5:%s28".formatted(getAsXCoord(i * 4 + 2), getAsXCoord(i * 4 + 4)), false, false);
            int x = 0;
            for (List<Object> objects : l) {
                if (x % 2 == 0) current = toid.getString(((String) objects.get(0)).trim());
                else {
                    List<String> currorder = Arrays.stream(lastNom.getString(current).split("###")).flatMap(s -> Arrays.stream(s.split(";"))).map(s -> s.split(",")[0]).collect(Collectors.toList());
                    String teamname = teamnames.getString(current);
                    if (!currkills.containsKey(current))
                        currkills.put(current, Google.get(sid, "%s!L200:L214".formatted(teamname), false, false).stream().map(li -> Integer.parseInt((String) li.get(0))).collect(Collectors.toList()));
                    if (!currdeaths.containsKey(current))
                        currdeaths.put(current, Google.get(sid, "%s!X200:X214".formatted(teamname), false, false).stream().map(li -> Integer.parseInt((String) li.get(0))).collect(Collectors.toList()));
                    if (!killstoadd.containsKey(current))
                        killstoadd.put(current, new AtomicInteger());
                    if (!deathstoadd.containsKey(current))
                        deathstoadd.put(current, new AtomicInteger());
                    String raus = (String) objects.get(0);
                    String rein = (String) objects.get(2);
                    JSONArray arr = allpicks.getJSONArray(current);
                    if (!raus.trim().equals("/") && !rein.trim().equals("/")) {
                        List<String> picks = getPicksAsList(arr);
                        logger.info("picks = " + picks);
                        logger.info("raus = " + raus);
                        logger.info("rein = " + rein);
                        int index = picks.indexOf(raus);
                        killstoadd.get(current).addAndGet(currkills.get(current).get(index));
                        deathstoadd.get(current).addAndGet(currdeaths.get(current).get(index));
                        JSONObject o = arr.getJSONObject(index);
                        o.put("name", rein);
                        o.put("tier", tierlist.getTierOf(rein));
                        String sdName = getSDName(rein);
                        JSONObject data = getDataJSON().getJSONObject(sdName);
                        int currindex = currorder.indexOf(raus) + 15;
                        Coord outloc = getTierlistLocation(raus, tierlist);
                        Coord inloc = getTierlistLocation(rein, tierlist);
                        b
                                .addSingle(teamname + "!B" + currindex, getGen5Sprite(data))
                                .addSingle(teamname + "!D" + currindex, rein);

                        if (outloc != null) {
                            b.addSingle("Tierliste!" + getAsXCoord((outloc.x() + 1) * 6) + (outloc.y() + 4), "-frei-");
                        }
                        if (inloc != null) {
                            b.addSingle("Tierliste!" + getAsXCoord((inloc.x() + 1) * 6) + (inloc.y() + 4), "='" + teamname + "'!B2");
                        }
                        b.addRow(teamname + "!A" + (index + 200), Arrays.asList(rein, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
                        b.addRow(teamname + "!N" + (index + 200), Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
                        List<Object> t = data.getStringList("types").stream().map(s -> getTypeIcons().getString(s)).collect(Collectors.toCollection(LinkedList::new));
                        if (t.size() == 1) t.add("/");
                        b.addRow(teamname + "!F" + currindex, t);
                        b.addSingle(teamname + "!H" + currindex, data.getJSONObject("baseStats").getInt("spe"));
                        b.addSingle(teamname + "!I" + currindex, tierlist.getPointsNeeded(rein));
                        b.addSingle(teamname + "!J" + currindex, "2");
                        b.addRow(teamname + "!L" + currindex, Arrays.asList(canLearnNDS(sdName, "stealthrock"), canLearnNDS(sdName, "defog"), canLearnNDS(sdName, "rapidspin"), canLearnNDS(sdName, "voltswitch", "uturn", "flipturn", "batonpass", "teleport")));
                    }
                }
                x++;
            }
        }
        for (String s : killstoadd.keySet()) {
            String teamname = teamnames.getString(s);
            b.addSingle("%s!L215".formatted(teamname), "=SUMME(L199:L214)");
            b.addSingle("%s!L199".formatted(teamname), killstoadd.get(s).get());
            b.addSingle("%s!X215".formatted(teamname), "=SUMME(X199:X214)");
            b.addSingle("%s!X199".formatted(teamname), deathstoadd.get(s).get());
        }
        save(emolgajson, "ndstestemolga.json");
        b.execute();
        e.done();
    }

    @PrivateCommand(name = "checktierlist")
    public static void checkTierlist(GenericCommandEvent e) {
        Tierlist tierlist = Tierlist.getByGuild(e.getArg(0));
        List<String> mons = new LinkedList<>();
        for (String s : tierlist.order) {
            for (String str : tierlist.tierlist.get(s)) {
                if (!getDraftGerName(str).isFromType(Translation.Type.POKEMON)) {
                    mons.add(str);
                }
            }
        }
        if (mons.isEmpty()) e.reply("Oh, du bist wohl kein Dasor :3");
        else
            e.reply(String.join("\n", mons));
    }

    @PrivateCommand(name = "converttierlist")
    public static void convertTierlist(GenericCommandEvent e) throws IOException {
        JSONArray arr = new JSONArray();
        List<String> curr = new LinkedList<>();
        for (String tiercolumn : Files.readAllLines(Paths.get("Tierlists", e.getArg(0), "tiercolumns.txt"))) {
            if (tiercolumn.equals("NEXT")) {
                arr.put(curr);
                curr.clear();
            } else {
                curr.add(tiercolumn);
            }
        }
        arr.put(curr);
        logger.info(arr.toString());
    }

    @PrivateCommand(name = "asltierlist")
    public static void asltierlist(GenericCommandEvent e) {
        Tierlist t = Tierlist.getByGuild(518008523653775366L);
        List<String> curr = new LinkedList<>();
        RequestBuilder b = new RequestBuilder("1wI291CWkKkWqQhY_KNu7GVfdincRz74omnCOvEVTDrc").withAdditionalSheets(
                "1tFLd9Atl9QpMgCQBclpeU1WlMqSRGMeX8COUVDIf4TU",
                "1A040AYoiqTus1wSq_3CXgZpgcY3ECpphVRJWHmXyxsQ",
                "1p8DSvd3vS5s5z-1UGPjKUhFVskYjQyrn-HbvU0pb5WE",
                "1nEJvV5UESfuJvsJplXi_RXXnq9lY2vD5NyrTF3ObcvU"
        );
        int x = 0;
        for (String s : t.order) {
            LinkedList<Object> mons = t.tierlist.get(s).stream().map(str -> {
                if (str.startsWith("M-")) {
                    if (str.endsWith("-X")) return "M-" + getEnglName(str.substring(2, str.length() - 2)) + "-X";
                    if (str.endsWith("-Y")) return "M-" + getEnglName(str.substring(2, str.length() - 2)) + "-Y";
                    return "M-" + getEnglName(str.substring(2));
                }
                if (str.startsWith("A-")) return "A-" + getEnglName(str.substring(2));
                if (str.startsWith("G-")) return "G-" + getEnglName(str.substring(2));
                Translation engl = getEnglNameWithType(str);
                if (engl.isSuccess()) return engl.getTranslation();
                logger.info("str = {}", str);
                return switch (str) {
                    case "Kapu-Riki" -> "Tapu Koko";
                    case "Kapu-Toro" -> "Tapu Bulu";
                    case "Kapu-Kime" -> "Tapu Fini";
                    default -> getEnglName(str.split("-")[0]) + "-" + str.split("-")[1];
                };
            }).sorted().collect(Collectors.toCollection(LinkedList::new));
            if (!s.equals("D"))
                b.addColumn("Tierliste [englisch]!%s%d".formatted(getAsXCoord(x * 2 + 1), 5), mons);
            else {
                int size = mons.size() / 3;
                for (int i = 0; i < 3; i++) {
                    List<Object> col = new LinkedList<>();
                    for (int j = 0; j < size; j++) {
                        col.add(mons.removeFirst());
                    }
                    b.addColumn("Tierliste [englisch]!%s%d".formatted(getAsXCoord(x++ * 2 + 1), 5), col);
                }
            }
            x++;
        }
        b.execute();
    }

    public static void execute(Message message) {
        String msg = message.getContentRaw();
        for (Method method : PrivateCommands.class.getDeclaredMethods()) {
            PrivateCommand a = method.getAnnotation(PrivateCommand.class);
            if (a == null) continue;
            if (msg.toLowerCase().startsWith("!" + a.name().toLowerCase() + " ") || msg.equalsIgnoreCase("!" + a.name()) || Arrays.stream(a.aliases()).anyMatch(s -> msg.startsWith("!" + s + " ") || msg.equalsIgnoreCase("!" + s))) {
                new Thread(() -> {
                    try {
                        method.invoke(null, new PrivateCommandEvent(message));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }, "PrivateCommand " + a.name()).start();
            }
        }
    }
}
