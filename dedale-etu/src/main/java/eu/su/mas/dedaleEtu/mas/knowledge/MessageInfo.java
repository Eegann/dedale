package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import jade.util.HashCache;

import java.io.Serializable;
import java.util.*;

public class MessageInfo implements Serializable {

    private MapNodes mapNodes;

    private ArrayList<String> edges;

    private boolean priority;

    private HashMap<String, AgentInfo> agentsInfo;

    /**
     * MessageInfo is the class containing all the data we are going to send through messages.
     * Agents will send their current position, the nodes they know as open or closed and the edges they know.
     * @param edges
     */
    public MessageInfo(MapNodes mapNodes, ArrayList<String> edges, HashMap<String, AgentInfo> agentsInfo, boolean priority){
        this.edges = edges;
        this.mapNodes = mapNodes;
        this.priority = priority;
        this.agentsInfo = agentsInfo;
    }

    public ArrayList<String> getEdges() {
        return edges;
    }

    public MapNodes getMapNodes() {
        return mapNodes;
    }

    public boolean isPriority() {
        return priority;
    }

    public HashMap<String, AgentInfo> getAgentsInfo() {
        return agentsInfo;
    }
}

