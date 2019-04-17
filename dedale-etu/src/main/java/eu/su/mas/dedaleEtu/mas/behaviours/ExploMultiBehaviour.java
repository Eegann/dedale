package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.*;
import jade.core.behaviours.SimpleBehaviour;
import java.util.*;

public class ExploMultiBehaviour extends SimpleBehaviour {

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

    private ReceiveMessageBehaviour rmb;

    private ReceivePingBehaviour rpb;
    /**
     * Update the data to send in the send behaviour
     */
    private SendMessageBehaviour smb;

    private ExploTreasureBehaviour etb;

    private CollectBehaviour cb;

    private OpenChestMultiBehaviour ocmb;

    private MapNodes mapNodes;


    public ExploMultiBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames, ReceiveMessageBehaviour rmb, ReceivePingBehaviour rpb, SendMessageBehaviour smb, ExploTreasureBehaviour etb, CollectBehaviour cb, OpenChestMultiBehaviour ocmb) {
        super(myagent);
        this.myMap=myMap;
        this.agentNames = agentNames;
        this.rmb = rmb;
        this.rpb = rpb;
        this.smb = smb;
        this.etb = etb;
        this.cb = cb;
        this.ocmb = ocmb;
        this.nextPath = new ArrayList<>();
        this.mapNodes = new MapNodes();
    }

    @Override
    public void action() {

        if (this.myMap == null) {
            this.myMap = new MapRepresentation(agentNames);

            rmb.setMyMap(this.myMap);
            if(ocmb != null) ocmb.setMyMap(this.myMap);
            if(etb != null) etb.setMyMap(this.myMap);
            if(cb != null) cb.setMyMap(this.myMap);
        }
        //0) Retrieve the current position
        String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (myPosition!=null){

            //List of observable from the agent's current position
            List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
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
            /**
             * Just added here to let you see what the agent is doing, otherwise he will be too quick
             */
            try {
                this.myAgent.doWait(300);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.updateData(myPosition);

            //2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
            Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
            HashMap<String,String> agentsPosition = this.myMap.getAgentsPosition();
            HashMap<String,ArrayList<String>> agentsPath = this.myMap.getAgentsPath();
            ArrayList<String> stenchNodes = this.mapNodes.getStenchNodes();

            for(String stench:stenchNodes){
                //TODO change condition to add wumpus node
                if(stenchNodes.containsAll(myMap.getNeighbor(stench))){
                    System.out.println("Wumpus: "+stench);
                    this.mapNodes.updateNode(new NodeInfo(stench, NodeInfo.NodeType.wumpus, new Date().getTime()));
                }
            }
            this.updateData(myPosition);
            System.out.println(mapNodes.getWumpusString()+" "+mapNodes.getRecentWumpusString());

            for(String node: nextPath){
                if(mapNodes.getRecentWumpusString().contains(node)){
                    this.nextPath.clear();
                    break;
                }
            }

            if(this.nextPath.size()>0) {
                if ((this.mapNodes.isClose(this.nextPath.get(this.nextPath.size() - 1)) || agentsPosition.values().contains(this.nextPath.get(0)))) {
                    this.nextPath.clear();
                }
                else{
                    for(String name: agentNames){
                        ArrayList<String> path = agentsPath.get(name);
                        //Check if there's an other agent who wanna go to the same destination
                        if(path != null && path.size()>0 && this.nextPath.size()>0) {
                            if (path.get(path.size() - 1).equals(this.nextPath.get(this.nextPath.size() - 1))) {
                                //Will change the path if the other agent's path is shorter
                                if (path.size() < this.nextPath.size()) {
                                    this.nextPath.clear();
                                } else if (path.size() == this.nextPath.size()) {
                                    if (name.compareTo(this.myAgent.getLocalName()) < 0) {
                                        this.nextPath.clear();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(this.nextPath.size() ==0) {
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
                        //If there isn't already a chosen path, and there's no agent on the node, it will be our next destination.
                        if (nextPath.size() == 0 && !agentsPosition.values().contains(nodeId) &&!mapNodes.getWumpusString().contains(nodeId)) nextPath.add(nodeId);
                    }
                }
                //We added nodes, so we have to update the lists
                this.mapNodes.updateLists();
            }


            //If there's no more node to explore, going to the next behaviour
            if (this.mapNodes.getOpenNodes().isEmpty()){
                //Explo finished
                finished=true;
                //Update on end code for receive message and receive ping, so the FSM goes to the correct behaviour
                this.rmb.setOnEnd(2);
                this.rpb.setOnEnd(2);
                System.out.println(this.myAgent.getLocalName()+" Exploration successufully done.");
            }
            else {
                if (this.nextPath.size() == 0) {

                    //no directly accessible openNode
                    for (int i = 0; i < this.mapNodes.getOpenNodes().size(); i++) {
                        List<String> tempPath = this.myMap.getShortestPath(myPosition, this.mapNodes.getOpenNodes().get(i).getId());
                        if (tempPath.size() > 0 ) {
                            if (this.nextPath.size() == 0 || tempPath.size() < this.nextPath.size()) {
                                this.nextPath = (ArrayList<String>) tempPath;
                            }
                        }
                    }
                }

                this.myMap.updateAgentsInfo(this.myAgent.getLocalName(), new AgentInfo(new ArrayList<>(nextPath), myPosition, ((AbstractDedaleAgent) this.myAgent).getMyExpertise(), new Date().getTime()));

                MessageInfo m = new MessageInfo(mapNodes, this.myMap.getEdges(), this.myMap.getAgentsInfo(), false);

                this.smb.updateMessage(m);


                //Just in case disjktra returned empty list...
                if (nextPath.size() > 0) {
                    //Moves to the next node.
                    String nextNode = nextPath.get(0);
                    //Don't remove the next position if there is a collision, otherwise we won't move BUT we'll still remove it
                    // => Cheater exception
                    boolean couldMove = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNode);
                    if (couldMove) {
                        nextPath.remove(0);
                    } else {
                        nextPath.clear();
                    }
                }
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
