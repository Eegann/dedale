package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.*;
import jade.core.behaviours.SimpleBehaviour;
import sun.plugin2.message.Message;

import java.util.*;

public class ExploTreasureBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 8567689731496787661L;

    private boolean finished = true;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;
    /**
     *
     */
    private ArrayList<String> nextPath;

    /**
     * Names of others exploAgents
     */
    private List<String> agentNames;
    /**
     * Update the data to send in the send behaviour
     */
    private SendMessageBehaviour smb;


    /**
     * actual node (in the gold list) to check
     */
    private int actualChecking = 0;
    /**
     * Boolean telling if we're giving the priority to a more important agent
     */
    private boolean givePrio = false;

    private MapNodes mapNodes;

    private boolean priority;
    private int block;
    private ReceiveMessageBehaviour rmb;
    private ReceivePingBehaviour rpb;


    public ExploTreasureBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames, SendMessageBehaviour smb, ReceiveMessageBehaviour rmb, ReceivePingBehaviour rpb) {
        super(myagent);
        this.myMap=myMap;
        this.agentNames = agentNames;
        this.smb = smb;
        this.nextPath = new ArrayList<>();
        this.mapNodes = new MapNodes();
        this.priority = false;
        this.rmb = rmb;
        this.rpb = rpb;
        block = 0;
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
                if(nextPath.isEmpty()){
                    this.myAgent.doWait(1000);
                }
                else{
                    this.myAgent.doWait(500);
                }
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
                        }
                        if(info.getLeft().getName().equals("Diamond")){
                            type = NodeInfo.NodeType.diamond;
                            qty = info.getRight();
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
            this.updateData();

            for(String node: nextPath){
                if(mapNodes.getRecentWumpusString().contains(node)){
                    this.nextPath.clear();
                    break;
                }
            }

            //In case the node doesn't have any treasure left (picked from other agent), we clear the path.
            if(this.nextPath.size()>0){
                boolean exist=false;
                for(NodeInfo ni:this.mapNodes.getTreasureNodes()) {
                    if (ni.getId().equals(this.nextPath.get(this.nextPath.size() - 1))) {
                        exist = true;
                    }
                }
                if(!exist){
                    this.nextPath.clear();
                }
            }

            //size of nextPath = 0 means either we are on a treasure node or we've been moved to give the priority to other agents.
            //No matter which case, we'll look for the next treasure node to check.
            if(this.nextPath.size() == 0){
                ArrayList<NodeInfo> treasureNode = this.mapNodes.getTreasureNodes();
                if(treasureNode.size()==0){
                    System.out.println(this.myAgent.getLocalName()+" no more treasure, i'm now useless now");
                    this.rmb.setOnEnd(0);
                    this.rpb.setOnEnd(1);
                    this.mapNodes.setWumpusToOpen();
                }
                else{
                    NodeInfo nextNode = treasureNode.get(0);
                    for(NodeInfo ni: treasureNode){
                        if(ni.getTimer()<nextNode.getTimer()){
                            nextNode = ni;
                        }
                    }
                    nextPath=(ArrayList<String>)this.myMap.getShortestPath(myPosition,nextNode.getId());
                }
            }
            //In this part, we check, everytime, if we're on the path of an agent which is more important.
            //We will then select a new path to let the other agent move freely to his destination.
            if(!this.priority) {
                for (String name : agentNames) {
                    if((new Date().getTime() - agentsTimer.get(name)) < 1000 && agentsPath.get(name).size() > 0) {
                        //Gives prio to priority agents/collect agents/lexicographicaly smaller agents
                        if (agentsPriority.get(name) || (name.startsWith("Collect") || this.myAgent.getLocalName().compareTo(name) > 0)) {

                            //Clear the naxt path if we are on an other agent's next position
                            if (agentsPath.get(name).get(0).equals(myPosition) ) {
                                this.nextPath.clear();
                                for (NodeInfo ni : this.mapNodes.getCloseNodes()) {
                                    if(agentsPriority.get(name)){
                                        if(!agentsPath.get(name).contains(ni.getId()) && !ni.getId().equals(myPosition)){
                                            List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                                            if( tempPath.size()>0 && !agentsPosition.get(name).equals(tempPath.get(0)) && !agentsPath.get(name).contains(tempPath.get(0))){
                                                if ((this.nextPath.size() == 0 || this.nextPath.size() > tempPath.size()) && nextPath.size()>0) {
                                                    this.nextPath = (ArrayList<String>) tempPath;
                                                }
                                            }
                                        }
                                    }
                                    else{
                                        //We select a path to a node that is not on the more important agent's path
                                        if (!agentsPath.get(name).contains(ni.getId()) && !ni.getId().equals(myPosition) ) {
                                            List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                                            if(tempPath.size()>0){
                                                //Select a path if there's no other agent on the way or if it's not on a priority agent's path
                                                boolean free = true;
                                                for(String n: agentNames){
                                                    if(n.startsWith("Collect") || (n.startsWith("Explo") && this.myAgent.getLocalName().compareTo(n) > 0)){
                                                        if(agentsPosition.get(n).contains(tempPath.get(0))){
                                                            free=false;
                                                        }
                                                    }
                                                }
                                                if (free) {
                                                    //Chose only a better path
                                                    if (this.nextPath.size() == 0 || this.nextPath.size() > tempPath.size()) {
                                                        this.nextPath = (ArrayList<String>) tempPath;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (agentsPriority.get(name)) {
                                    this.priority = true;
                                    break;
                                }
                                System.out.println(myAgent.getLocalName() + " on path of " + name + " " + agentsPath.get(name) + " my new path: " + nextPath);
                            }
                        }
                    }
                }
            }

            if(this.nextPath.size()==0){
                for (String n : this.myMap.getMultiNodes()) {
                    List<String> tempPath = this.myMap.getShortestPath(myPosition, n);
                    if ((tempPath.size() < this.nextPath.size() || this.nextPath.size() == 0)&& tempPath.size()>0) {
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

                    if(!this.nextPath.contains(s) && !myPosition.equals(s)){
                        this.nextPath.add(s);
                        break;
                    }
                }
                this.priority = true;
            }

            if(block == 5 && this.mapNodes.getStenchNodes().contains(myPosition)){
                block = 0;
                this.mapNodes.updateNode(new NodeInfo(nextPath.get(0), NodeInfo.NodeType.wumpus, new Date().getTime()));
                nextPath.clear();
                priority = false;
                givePrio = false;
            }
            if(block == 10){
                block = 0;
                nextPath.clear();
                priority = false;
                givePrio = false;
            }

            this.myMap.updateAgentsInfo(this.myAgent.getLocalName(), new AgentInfo(new ArrayList<>(nextPath), myPosition, ((AbstractDedaleAgent) this.myAgent).getMyExpertise(), new Date().getTime()));

            System.out.println(this.myAgent.getLocalName()+" "+block+" "+" "+priority+" "+this.nextPath);


            MessageInfo m = new MessageInfo(mapNodes, this.myMap.getEdges(), this.myMap.getAgentsInfo(), priority);
            this.smb.updateMessage(m);

            //Just in case the path is empty
            if(this.nextPath.size()>0) {
                //Moves to the next node.
                String nextNode = this.nextPath.get(0);
                //Don't remove the next position if there is a collision, otherwise we won't move BUT we'll still remove it
                // => Cheater exception
                boolean couldMove = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNode);
                if (couldMove) {
                    block = 0;
                    this.nextPath.remove(0);
                }
                else this.block++;

            }
            if(this.nextPath.size()==0){
                if(!this.givePrio && !this.priority) this.actualChecking++;
                this.givePrio=false;
                this.priority = false;
            }
        }
    }

    private void updateData() {
        this.mapNodes.update(this.myMap.getMapNodes());
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