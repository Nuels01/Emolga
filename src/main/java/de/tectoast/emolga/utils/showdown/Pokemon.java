package de.tectoast.emolga.utils.showdown;


import java.util.List;

public class Pokemon {
    private final Player player;
    private String pokemon;
    private int kills;
    private Pokemon statusedBy, bindedBy, cursedBy, seededBy, nightmaredBy, confusedBy, lastDmgBy, perishedBy;
    private boolean dead = false;
    private final List<Integer> zoroTurns;
    private final List<String> game;
    private int hp = 100;
    private String ability = "";


    public Pokemon(String poke, Player player, List<Integer> zoroTurns, List<String> game) {
        pokemon = poke;
        this.zoroTurns = zoroTurns;
        this.game = game;
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void setAbility(String ability) {
        this.ability = ability;
        System.out.println("Set Ability from " + this.getPokemon() + " to " + this.ability);
    }

    public boolean noAbilityTrigger(int line) {
        return !game.get(line + 1).contains("[from] ability: " + this.ability);
    }

    public boolean checkHPZoro(int hp) {
        return this.hp != hp;
    }

    public Pokemon getSeededBy() {
        return seededBy;
    }

    public void setSeededBy(Pokemon seededBy) {
        this.seededBy = seededBy;
    }

    public Pokemon getNightmaredBy() {
        return nightmaredBy;
    }

    public void setNightmaredBy(Pokemon nightmaredBy) {
        this.nightmaredBy = nightmaredBy;
    }

    public Pokemon getConfusedBy() {
        return confusedBy;
    }

    public void setConfusedBy(Pokemon confusedBy) {
        this.confusedBy = confusedBy;
    }

    public Pokemon getCursedBy() {
        return cursedBy;
    }

    public void setCursedBy(Pokemon cursedBy) {
        this.cursedBy = cursedBy;
    }

    public Pokemon getBindedBy() {
        return bindedBy;
    }

    public void setBindedBy(Pokemon bindedBy) {
        this.bindedBy = bindedBy;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(int line) {
        if(game.get(line + 1).contains("|replace|") && game.get(line + 1).contains("|Zor")) {
            player.getMons().stream().filter(p -> p.getPokemon().equals("Zoroark") || p.getPokemon().equals("Zorua")).findFirst().ifPresent(p -> p.dead = true);
        } else {
            this.dead = true;
        }
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getHp() {
        return hp;
    }

    public String getPokemon() {
        return pokemon;
    }

    public void setPokemon(String pokemon) {
        this.pokemon = pokemon;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public Pokemon getStatusedBy() {
        return statusedBy;
    }

    public void setStatusedBy(Pokemon statusedBy) {
        this.statusedBy = statusedBy;
    }

    public Pokemon getLastDmgBy() {
        return lastDmgBy;
    }

    public void setLastDmgBy(Pokemon lastDmgBy) {
        this.lastDmgBy = lastDmgBy;
    }

    public void killsPlus1(int turn) {
        if(this.zoroTurns.contains(turn)) {
            player.getMons().stream().filter(p -> p.getPokemon().equals("Zoroark") || p.getPokemon().equals("Zorua")).findFirst().ifPresent(p -> p.kills++);
        } else {
            this.kills++;
        }
    }


    public Pokemon getPerishedBy() {
        return perishedBy;
    }


    public void setPerishedBy(Pokemon perishedBy) {
        this.perishedBy = perishedBy;
    }

    @Override
    public String toString() {
        return "Pokemon{" + "player=" + player +
                ", pokemon='" + pokemon + '\'' +
                ", kills=" + kills +
                ", dead=" + dead +
                '}';
    }
}