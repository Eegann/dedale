package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.*;
import jade.core.behaviours.SimpleBehaviour;
import org.apache.commons.math3.analysis.function.Abs;

import java.util.*;

public class CollectBehaviour extends SimpleBehaviour {

    private boolean finished = true;

    /**
     * Current knowledge of the agent regarding the environment
     */
    protected MapRepresentation myMap;
    /**
     *
     */
    protected ArrayList<String> nextPath;

    /**
     * Names of others exploAgents
     */
    protected List<String> agentNames;
    /**
     * Update the map in the message behaviour
     */
    protected ReceiveMessageBehaviour rb;
    /**
     * Update the data to send in the send behaviour
     */
    protected SendMessageBehaviour smb;


    protected MapNodes mapNodes;
    private NodeInfo.NodeType type;
    boolean priority;
    boolean backpackFull;
    int bagCapacity;
    ReceivePingBehaviour rpb;
    ReceiveMessageBehaviour rmb;

    public CollectBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames, ReceiveMessageBehaviour rb, SendMessageBehaviour smb, NodeInfo.NodeType type, ReceivePingBehaviour rpb, ReceiveMessageBehaviour rmb) {
        super(myagent);
        this.myMap=myMap;
        this.agentNames = agentNames;
        this.rb = rb;
        this.smb = smb;
        this.nextPath = new ArrayList<>();
        this.mapNodes = new MapNodes();
        this.type = type;
        this.priority = false;
        this.backpackFull = false;
        this.bagCapacity = ((AbstractDedaleAgent)myAgent).getBackPackFreeSpace();
        this.rmb = rmb;
        this.rpb = rpb;
    }

    @Override
    public void action() {

        //0) Retrieve the current position
        String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (myPosition!=null){
            //List of observable from the agent's current position
            List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

            /**
             * Just added here to let you see what the agent is doing, otherwise he will be too quick
             */
            try {
                this.myAgent.doWait(200);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.updateData();
            HashMap<String,String> agentsPosition = this.myMap.getAgentsPosition();
            HashMap<String,ArrayList<String>> agentsPath = this.myMap.getAgentsPath();
            HashMap<String, Boolean> agentsPriority = this.myMap.getAgentsPriority();
            HashMap<String, Long> agentsTimer = this.myMap.getAgentsTimer();

            //Update the node we are on
            for(int i=0;i<lobs.size();i++){
                String node = lobs.get(i).getLeft();

                if(lobs.get(i).getRight().size()>0) {
                    List<Couple<Observation,Integer>> informations = lobs.get(i).getRight();
                    boolean open=false;
                    int strength=0;
                    int lockpicking=0;
                    int qty=0;
                    boolean stench = false;
                    NodeInfo.NodeType type= NodeInfo.NodeType.open;
                    for(Couple<Observation, Integer> info: informations) {
                        if(info.getLeft().getName().equals("Gold")){
                            type = NodeInfo.NodeType.gold;
                            qty = info.getRight();
                            if(this.type.equals(NodeInfo.NodeType.gold)){
                                int x = ((AbstractDedaleAgent) this.myAgent).pick();
                                qty = qty-x;
                            }
                        }
                        if(info.getLeft().getName().equals("Diamond")){
                            type = NodeInfo.NodeType.diamond;
                            qty = info.getRight();
                            if(this.type.equals(NodeInfo.NodeType.diamond)){
                                int x = ((AbstractDedaleAgent) this.myAgent).pick();
                                qty = qty-x;
                            }
                        }
                        if(info.getLeft().getName().equals("LockPicking")){
                            lockpicking = info.getRight();
                        }
                        if(info.getLeft().getName().equals("Strength")){
                            strength = info.getRight();
                        }
                        if(info.getLeft().getName().equals("LockIsOpen")){
                            open = info.getRight()==1;
                        }
                        if(info.getLeft().getName().equals("Stench")){
                            stench = true;
                        }
                    }
                    this.mapNodes.updateNode(new NodeInfo(node, type, new Date().getTime(), qty, open, lockpicking, strength));
                    this.mapNodes.updateStench(node, stench, new Date().getTime());
                } else{
                    this.mapNodes.updateNode(new NodeInfo(myPosition, i==0?NodeInfo.NodeType.close: NodeInfo.NodeType.open, new Date().getTime()));
                    this.mapNodes.updateStench(node, false, new Date().getTime());

                }
            }

            ArrayList<String> stenchNodes = mapNodes.getStenchNodes();

            for(String stench:stenchNodes){
                if(stenchNodes.containsAll(myMap.getNeighbor(stench))){
                    System.out.println("Wumpus: "+stench);
                    this.mapNodes.updateNode(new NodeInfo(stench, NodeInfo.NodeType.wumpus, new Date().getTime()));
                }
            }
            this.updateData();

            for(String node: nextPath){
                if(mapNodes.getRecentWumpusString().contains(node)){
                    this.nextPath.clear();
                    break;
                }
            }
            //size of nextPath = 0 means either we are on a treasure node or we've been moved to give the priority to other agents.
            //No matter which case, we'll look for the next treasure node to check.

            if(this.nextPath.size() == 0 ){
                if(this.myMap.getNeighbor(agentsPosition.get("Tanker1")).contains(myPosition) && ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace() < this.bagCapacity){
                    ((AbstractDedaleAgent)this.myAgent).emptyMyBackPack("Tanker1");
                }
                this.searchNextPath(myPosition);
            }
            if(!this.priority) {
                //In this part, we check, everytime, if we're on the path of an agent which is more important.
                //We will then select a new path to let the other agent move freely to his destination.
                for (String name : agentNames) {
                    if(new Date().getTime() - agentsTimer.get(name)<1000) {
                        //Gives priority to priority agents/lexicographicaly smaller agents
                        if (agentsPriority.get(name) || (agentsPath.get(name).size() > 0 && (name.startsWith("Collect") && this.myAgent.getLocalName().compareTo(name) > 0))) {
                            //Clear the naxt path if we are on an other agent's next position
                            if (agentsPath.get(name).get(0).equals(myPosition)) {
                                this.nextPath.clear();
                                for (NodeInfo ni : this.mapNodes.getCloseNodes()) {
                                    //We select a path to a node that is not on the more important agent's path
                                    if (!agentsPath.get(name).contains(ni.getId()) && !ni.getId().equals(myPosition) && !agentsPosition.values().contains(ni.getId()) ) {
                                        List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                                        //Select a path if there's no other agent on the way or if it's not on a priority agent's path
                                        if (!agentsPosition.get(name).equals(tempPath.get(0)) && !(agentsPriority.get(name) && agentsPath.get(name).contains(tempPath.get(0)))) {
                                            //Chose only a better path
                                            if (this.nextPath.size() == 0 || this.nextPath.size() > tempPath.size()) {
                                                this.nextPath = (ArrayList<String>) tempPath;
                                            }
                                        }
                                    }
                                }
                                System.out.println(this.myAgent.getLocalName()+" on path of "+name+" new path: "+nextPath+" "+agentsPriority);
                                if (agentsPriority.get(name)) {
                                    this.priority = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if(this.nextPath.size()==0){
                boolean inPath = false;
                for(String name:agentNames){
                    if(agentsPath.get(name).contains(myPosition) && new Date().getTime() - agentsTimer.get(name) < 1000){
                        inPath = true;
                    }
                }
                if(inPath) {
                    for (String n : this.myMap.getMultiNodes()) {
                        List<String> tempPath = this.myMap.getShortestPath(myPosition, n);
                        if (tempPath.size() < this.nextPath.size() || this.nextPath.size() == 0) {
                            this.nextPath = (ArrayList<String>) tempPath;
                        }
                    }
                    ArrayList<String> neighbor;
                    if(this.nextPath.size()==0){
                        neighbor = this.myMap.getNeighbor(myPosition);
                    }
                    else{
                        neighbor = this.myMap.getNeighbor(this.nextPath.get(this.nextPath.size()-1));
                    }
                    for(String s:neighbor){

                        if(!agentsPosition.values().contains(s) && !this.nextPath.contains(s) && !myPosition.equals(s)){
                            this.nextPath.add(s);
                            break;
                        }
                    }
                    this.priority = true;
                }
            }

            this.myMap.updateAgentsInfo(this.myAgent.getLocalName(), new AgentInfo(new ArrayList<>(nextPath), myPosition, ((AbstractDedaleAgent) this.myAgent).getMyExpertise(), new Date().getTime()));

            MessageInfo m = new MessageInfo(mapNodes, this.myMap.getEdges(), this.myMap.getAgentsInfo(), this.priority);
            this.smb.updateMessage(m);

            System.out.println(this.myAgent.getLocalName()+" "+this.nextPath);

            if( nextPath.size() > 0 && !myMap.getNeighbor(myPosition).contains(nextPath.get(0))){
                this.nextPath.clear();
            }

            //Just in case the path is empty
            if(this.nextPath.size()>0) {
                //Moves to the next node.
                String nextNode = this.nextPath.get(0);
                //Don't remove the next position if there is a collision, otherwise we won't move BUT we'll still remove it
                // => Cheater exception
                boolean couldMove = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNode);
                if (couldMove){
                    this.nextPath.remove(0);
                    if(this.nextPath.size()==0){
                        this.priority = false;
                    }
                }
            }
        }
    }

    private void updateData() {
        this.mapNodes.update(this.myMap.getMapNodes());
    }

    private void searchNextPath(String myPosition){
        if(((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace() == 0){
            this.backpackFull = true;
            ArrayList<String> neighbor = this.myMap.getNeighbor(this.myMap.getAgentsPosition().get("Tanker1"));
            for(String node: neighbor){
                List<String> tempPath = this.myMap.getShortestPath(myPosition, node);
                if(nextPath.size() == 0 || nextPath.size()>tempPath.size()){
                    this.nextPath = (ArrayList<String>)tempPath;
                }
            }
        }
        else{
            if(this.type.equals(NodeInfo.NodeType.gold)){
                ArrayList<NodeInfo> goldNodes = this.mapNodes.getGoldNodes();
                //If bag is empty
                if(((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace() == bagCapacity){
                    if(goldNodes.isEmpty() && !mapNodes.getWumpusNodes().isEmpty()){
                        System.out.println("CollectBehaviour "+this.myAgent.getLocalName()+" no more gold, i'm useless now.");
                        this.rmb.setOnEnd(0);
                        this.rpb.setOnEnd(1);
                        this.mapNodes.setWumpusToOpen();
                    }
                    else {
                        for (NodeInfo ni : goldNodes) {
                            List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                            if (this.nextPath.size() == 0 || tempPath.size() < this.nextPath.size()) {
                                this.nextPath = (ArrayList<String>) tempPath;
                            }
                        }
                    }
                }
                else{
                    if(goldNodes.isEmpty()){
                        ArrayList<String> neighbor = this.myMap.getNeighbor(this.myMap.getAgentsPosition().get("Tanker1"));
                        for(String node: neighbor){
                            List<String> tempPath = this.myMap.getShortestPath(myPosition, node);
                            if(nextPath.size() == 0 || nextPath.size()>tempPath.size()){
                                this.nextPath = (ArrayList<String>)tempPath;
                            }
                        }
                    }
                    else {
                        for (NodeInfo ni : goldNodes) {
                            List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                            if (this.nextPath.size() == 0 || tempPath.size() < this.nextPath.size()) {
                                this.nextPath = (ArrayList<String>) tempPath;
                            }
                        }
                    }
                }
            }
            if(this.type.equals(NodeInfo.NodeType.diamond)){
                ArrayList<NodeInfo> diamondNodes = this.mapNodes.getDiamondNodes();
                //If bag is empty
                if(((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace() == bagCapacity){
                    if(diamondNodes.isEmpty() &&!mapNodes.getWumpusNodes().isEmpty()){
                        System.out.println("CollectBehaviour "+this.myAgent.getLocalName()+" no more diamond, i'm useless now.");
                        this.rmb.setOnEnd(0);
                        this.rpb.setOnEnd(1);
                        this.mapNodes.setWumpusToOpen();
                    }
                    else {
                        for (NodeInfo ni : diamondNodes) {
                            List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                            if (this.nextPath.size() == 0 || tempPath.size() < this.nextPath.size()) {
                                this.nextPath = (ArrayList<String>) tempPath;
                            }
                        }
                    }
                }
                else{
                    if(diamondNodes.isEmpty()){
                        ArrayList<String> neighbor = this.myMap.getNeighbor(this.myMap.getAgentsPosition().get("Tanker1"));
                        for(String node: neighbor){
                            List<String> tempPath = this.myMap.getShortestPath(myPosition, node);
                            if(nextPath.size() == 0 || nextPath.size()>tempPath.size()){
                                this.nextPath = (ArrayList<String>)tempPath;
                            }
                        }
                    }
                    else {
                        for (NodeInfo ni : diamondNodes) {
                            List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                            if (this.nextPath.size() == 0 || tempPath.size() < this.nextPath.size()) {
                                this.nextPath = (ArrayList<String>) tempPath;
                            }
                        }
                    }
                }
            }
        }

    }

    public void setMyMap (MapRepresentation myMap){
        this.myMap = myMap;
    }


    @Override
    public boolean done() {
        return finished;
    }

    @Override
    public int onEnd(){
        return 0;
    }

}