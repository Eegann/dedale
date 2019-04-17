package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

public class AgentInfo implements Serializable {
    ArrayList<String> path;
    String pos;
    Set<Couple<Observation, Integer>> expertise;
    long timer;

    public AgentInfo(ArrayList<String> path, String pos, Set<Couple<Observation, Integer>> expertise, long timer){
        this.path = path;
        this.pos = pos;
        this.expertise = expertise;
        this.timer = timer;
    }

    public Set<Couple<Observation, Integer>> getExpertise() {
        return expertise;
    }

    public long getTimer() {
        return timer;
    }

    public ArrayList<String> getPath() {
        return path;
    }

    public String getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return this.path+" "+this.pos+" "+this.timer+" "+this.expertise;
    }
}