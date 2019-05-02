package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.*;
import jade.core.behaviours.SimpleBehaviour;

import java.util.*;

public class TankerBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 8567689731496787661L;

    private boolean finished = true;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;

    private ReceiveMessageBehaviour rmb;
    /**
     * Update the data to send in the send behaviour
     */
    private SendMessageBehaviour smb;

    private MapNodes mapNodes;

    private ArrayList<String> agentNames;

    private ArrayList<String> nextPath;

    private boolean priority;

    private boolean onPath;

    private int block;

    public TankerBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, ReceiveMessageBehaviour rmb, SendMessageBehaviour smb, ArrayList<String> agentNames) {
        super(myagent);
        this.myMap=myMap;
        this.rmb = rmb;
        this.smb = smb;
        this.mapNodes = new MapNodes();
        this.agentNames = agentNames;
        nextPath = new ArrayList<>();
        priority = false;
        onPath = false;
        block = 0;
    }

    @Override
    public void action() {

        if (this.myMap == null) {
            this.myMap = new MapRepresentation(agentNames);

            rmb.setMyMap(this.myMap);
        }
        //0) Retrieve the current position
        String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (myPosition!=null){
            //List of observable from the agent's current position
            List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

            if(block == 5 && this.mapNodes.getStenchNodes().contains(myPosition)){
                block = 0;
                this.mapNodes.updateNode(new NodeInfo(nextPath.get(0), NodeInfo.NodeType.wumpus, new Date().getTime()));
                nextPath.clear();
                priority = false;
            }

            for(int i=0;i<lobs.size();i++){
                if(lobs.get(i).getRight().size()>0) {
                    String node = lobs.get(i).getLeft();
                    List<Couple<Observation,Integer>> informations = lobs.get(i).getRight();
                    boolean open=false;
                    int strength=0;
                    int lockpicking=0;
                    int qty=0;
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
                    }
                    this.mapNodes.updateNode(new NodeInfo(node, type, new Date().getTime(), qty, open, lockpicking, strength));
                }
                else{
                    this.mapNodes.updateNode(new NodeInfo(myPosition, i==0?NodeInfo.NodeType.close: NodeInfo.NodeType.open, new Date().getTime()));
                }
            }

            Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
            this.myMap.addNode(myPosition);
            while (iter.hasNext()) {
                String nodeId = iter.next().getLeft();

                if (!this.mapNodes.isClose(nodeId)) {
                    if (!this.mapNodes.isOpen(nodeId)) {
                        this.mapNodes.updateNode(new NodeInfo(nodeId, NodeInfo.NodeType.open, new Date().getTime()));
                        this.myMap.addNode(nodeId, MapRepresentation.MapAttribute.open);
                        this.myMap.addEdge(myPosition, nodeId);
                    } else {
                        //the node exist, but not necessarily the edge.
                        this.myMap.addNode(nodeId);
                        this.myMap.addEdge(myPosition, nodeId);
                    }
                }
            }
            /**
             * Just added here to let you see what the agent is doing, otherwise he will be too quick
             */
            try {
                this.myAgent.doWait(500);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.updateData(myPosition);

            this.myMap.updateAgentsInfo(this.myAgent.getLocalName(), new AgentInfo(new ArrayList<>(), myPosition, ((AbstractDedaleAgent) this.myAgent).getMyExpertise(), new Date().getTime()));

            HashMap<String, Boolean> agentsPriority = myMap.getAgentsPriority();
            HashMap<String, Long> agentsTimer = myMap.getAgentsTimer();
            HashMap<String, ArrayList<String>> agentsPath = myMap.getAgentsPath();
            HashMap<String, String> agentsPosition = myMap.getAgentsPosition();

            if(this.nextPath.isEmpty()){
                boolean gold = false;
                boolean diamond = false;
                boolean goldOpen = false;
                boolean diamondOpen = false;
                for (NodeInfo ni : this.mapNodes.getGoldNodes()) {
                    if (ni.getId().equals(myPosition)) {
                        gold = true;
                        goldOpen = ni.isOpen();
                    }
                }
                for (NodeInfo ni : this.mapNodes.getDiamondNodes()) {
                    if (ni.getId().equals(myPosition)) {
                        diamond = true;
                        diamondOpen = ni.isOpen();
                    }
                }
                //Keep trying to open the lock while waiting other agents
                if (gold &&!goldOpen) {
                    ((AbstractDedaleAgent) this.myAgent).openLock(Observation.GOLD);
                }
                if (diamond &&!diamondOpen) {
                    ((AbstractDedaleAgent) this.myAgent).openLock(Observation.DIAMOND);
                }
            }

            if(!this.priority) {
                //In this part, we check, everytime, if we're on the path of an agent which is more important.
                //We will then select a new path to let the other agent move freely to his destination.
                for (String name : agentNames) {
                    if(new Date().getTime() - agentsTimer.get(name)<1000 && agentsPath.get(name).size() > 0) {

                        //Clear the naxt path if we are on an other agent's next position
                        if (agentsPath.get(name).get(0).equals(myPosition)) {
                            onPath = true;
                            this.nextPath.clear();
                            for (NodeInfo ni : this.mapNodes.getCloseNodes()) {
                                if(agentsPriority.get(name)){
                                    if(!agentsPath.get(name).contains(ni.getId()) && !ni.getId().equals(myPosition)){
                                        List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                                        if( !agentsPosition.get(name).equals(tempPath.get(0)) && !agentsPath.get(name).contains(tempPath.get(0))){
                                            if (this.nextPath.size() == 0 || this.nextPath.size() > tempPath.size()) {
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
                                                if(agentsPosition.get(n).contains(tempPath.get(0))){
                                                    free=false;
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
                                //We select a path to a node that is not on the more important agent's path
                                if (!agentsPath.get(name).contains(ni.getId()) && !ni.getId().equals(myPosition)) {
                                    List<String> tempPath = this.myMap.getShortestPath(myPosition, ni.getId());
                                    //Select a path if there's no other agent on the way or if it's not on a priority agent's path
                                    if (tempPath.size()>0 && !agentsPosition.values().contains(tempPath.get(0))) {
                                        //Chose only a better path
                                        if ((this.nextPath.size() == 0 || this.nextPath.size() > tempPath.size()) ) {
                                            this.nextPath = (ArrayList<String>) tempPath;
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
            if(onPath && nextPath.isEmpty()){
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

                    if(!this.nextPath.contains(s) && !myPosition.equals(s)){
                        this.nextPath.add(s);
                        break;
                    }
                }
                this.priority = true;


            }



            MessageInfo m = new MessageInfo(mapNodes, this.myMap.getEdges(), this.myMap.getAgentsInfo(), false);
            System.out.println(this.myAgent.getLocalName()+" "+" "+priority+" "+this.nextPath);

            this.smb.updateMessage(m);

            if(this.nextPath.size()>0) {
                //Moves to the next node.
                String nextNode = this.nextPath.get(0);
                //Don't remove the next position if there is a collision, otherwise we won't move BUT we'll still remove it
                // => Cheater exception
                boolean couldMove = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNode);
                if (couldMove){
                    this.nextPath.remove(0);
                }
                else{
                    this.block++;
                }
            }
            if(this.nextPath.size()==0){
                this.priority = false;
                this.onPath = false;
            }
        }
    }

    private void updateData(String myPosition){
        this.myMap.addNode(myPosition);
        //Update data from the map.
        this.mapNodes.update(this.myMap.getMapNodes());
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
