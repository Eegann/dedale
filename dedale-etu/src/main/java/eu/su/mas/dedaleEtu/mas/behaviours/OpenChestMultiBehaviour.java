package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.*;
import jade.core.behaviours.SimpleBehaviour;
import java.util.*;

public class OpenChestMultiBehaviour extends SimpleBehaviour {

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


    private MapNodes mapNodes;

    private ChestRepartition chestRepartition;

    private ArrayList<String> assignedCoallition;

    boolean priority;

    int block;

    public OpenChestMultiBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames, ReceiveMessageBehaviour rmb, ReceivePingBehaviour rpb, SendMessageBehaviour smb) {
        super(myagent);
        this.myMap = myMap;
        this.agentNames = agentNames;
        this.rmb = rmb;
        this.rpb = rpb;
        this.smb = smb;
        this.nextPath = new ArrayList<>();
        this.mapNodes = new MapNodes();
        this.priority = false;
        this.block = 0;
    }

    @Override
    public void action() {

        //0) Retrieve the current position
        String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (myPosition != null) {
            //List of observable from the agent's current position
            List<Couple<String, List<Couple<Observation, Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();//myPosition

            if(block == 5 && this.mapNodes.getStenchNodes().contains(myPosition)){
                block = 0;
                if(nextPath.size()>0)this.mapNodes.updateNode(new NodeInfo(nextPath.get(0), NodeInfo.NodeType.wumpus, new Date().getTime()));
                nextPath.clear();
                priority = false;
            }
            if(block == 10){
                block = 0;
                nextPath.clear();
                priority = false;
            }

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
                this.myAgent.doWait(500);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.updateData(myPosition);

            //2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
            HashMap<String, String> agentsPosition = this.myMap.getAgentsPosition();
            HashMap<String, Set<Couple<Observation, Integer>>> agentsExpertise = this.myMap.getAgentsExpertise();
            HashMap<String, Long> agentsTimer = this.myMap.getAgentsTimer();

            for(String node: nextPath){
                if(mapNodes.getRecentWumpusString().contains(node)){
                    this.nextPath.clear();
                    break;
                }
            }

            this.chestRepartition= new ChestRepartition(agentsExpertise, (ArrayList<String>)this.agentNames, this.myAgent.getLocalName());
            if(assignedCoallition == null){
                assignedCoallition = this.chestRepartition.computeCoallitions(this.mapNodes);
            }

            ArrayList<NodeInfo> chestNodes = this.mapNodes.getChestNodes();
            System.out.println(chestNodes);

            if( chestNodes.size() == 0){
                //Explo finished
                finished = true;
                //Update on end for receive message and receive ping, so the FSM goes to the right behaviour
                if(this.myAgent.getLocalName().contains("Explo")){
                    this.rmb.setOnEnd(3);
                    this.rpb.setOnEnd(3);
                }
                else{
                    this.rmb.setOnEnd(3);
                    this.rpb.setOnEnd(3);

                }
                System.out.println("OpenChest "+this.myAgent.getLocalName() + " Chest openning successufully done.");
            }
            ArrayList<String> chestString = new ArrayList<>();
            for(NodeInfo cn: chestNodes){
                chestString.add(cn.getId());
            }
            if(this.nextPath.size()>0 && chestString.contains(this.nextPath.get(this.nextPath.size()-1))){
                nextPath.clear();
            }

            if(this.nextPath.size()==0) {
                //Define if the node contains gold or diamond
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
                boolean stillOneChest = false;
                //Search for the next node we're supposed to unlock
                for (String chest : assignedCoallition) {
                    boolean isChest = false;
                    for (NodeInfo ni : this.mapNodes.getChestNodes()) {
                        if (chest.equals(ni.getId())) {
                            isChest = true;
                            break;
                        }
                    }
                    if (isChest ) {
                        stillOneChest = true;
                        this.nextPath = (ArrayList<String>) this.myMap.getShortestPath(myPosition, chest);
                        break;
                    }
                }
                if(!stillOneChest){
                    String pos = agentsPosition.get("Tanker1");
                    for(String neighbor: this.myMap.getNeighbor(pos)){
                        List<String> tempPath = this.myMap.getShortestPath(myPosition, neighbor);
                        if(nextPath.size() == 0 || nextPath.size()>tempPath.size()){
                            nextPath = (ArrayList<String>)tempPath;
                        }
                    }
                }
            }

            this.myMap.updateAgentsInfo(this.myAgent.getLocalName(), new AgentInfo(new ArrayList<>(nextPath), myPosition, ((AbstractDedaleAgent) this.myAgent).getMyExpertise(), new Date().getTime()));

            MessageInfo m = new MessageInfo(mapNodes, this.myMap.getEdges(), this.myMap.getAgentsInfo(), priority);

            this.smb.updateMessage(m);

            if( nextPath.size() > 0 && !myMap.getNeighbor(myPosition).contains(nextPath.get(0))){
                this.nextPath.clear();
            }

            if (nextPath.size() > 0) {
                //Moves to the next node.
                String nextNode = nextPath.get(0);
                //Don't remove the next position if there is a collision, otherwise we won't move BUT we'll still remove it
                // => Cheater exception
                boolean couldMove = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNode);
                if (couldMove) {
                    nextPath.remove(0);
                    block = 0;
                } else {
                    nextPath.clear();
                    block ++;
                }
            }
            if(nextPath.size() == 0){
                priority = false;
            }
        }

    }

    private void updateData(String myPosition) {
        this.myMap.addNode(myPosition);
        //Update data from the map.
        this.mapNodes.update(this.myMap.getMapNodes());
    }

    public void setMyMap(MapRepresentation myMap){
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
