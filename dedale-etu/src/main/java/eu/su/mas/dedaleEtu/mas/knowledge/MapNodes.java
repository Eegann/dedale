package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.tuple.Couple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MapNodes implements Serializable {
    /**
     * Array containing all the nodes of the map, with multiple information
     */
    private ArrayList<NodeInfo> nodes;
    /**
     * Each list contain a type of node. One node can be in multiple list (every node in diamond node are also closed nodes)
     */
    private ArrayList<NodeInfo> goldNodes;
    private ArrayList<NodeInfo> diamondNodes;
    private ArrayList<NodeInfo> closeNodes;
    private ArrayList<NodeInfo> openNodes;
    private ArrayList<NodeInfo> chestNodes;
    private ArrayList<NodeInfo> stenchNodes;
    private ArrayList<NodeInfo> wumpusNodes;

    public MapNodes(){
        this.nodes = new ArrayList<>();
        this.goldNodes = new ArrayList<>();
        this.diamondNodes = new ArrayList<>();
        this.closeNodes = new ArrayList<>();
        this.openNodes = new ArrayList<>();
        this.chestNodes = new ArrayList<>();
        this.stenchNodes = new ArrayList<>();
        this.wumpusNodes = new ArrayList<>();
    }

    /**
     * Update a specific node, or add it if it doesn't exist
     * @param nodeInfo
     */
    public void updateNode(NodeInfo nodeInfo){
        boolean exist = false;
        for(int i=0; i<this.nodes.size();i++){
            if(this.nodes.get(i).getId().equals(nodeInfo.getId())){
                if(!nodeInfo.getType().equals(NodeInfo.NodeType.open)) {
                    //Update the node if the timer is inferior. Never update an open node (useless). Update if our node is open and other node is something else, no matter the timer
                    if(nodeInfo.getTimer()>this.nodes.get(i).getTimer() || nodes.get(i).getType().equals(NodeInfo.NodeType.open) && !nodeInfo.getType().equals(NodeInfo.NodeType.open)) {
                        this.nodes.get(i).update(nodeInfo);
                        updateStench(nodeInfo.getId(), nodeInfo.isStench(), nodeInfo.getStenchTimer());
                    }
                }
                exist = true;
                break;
            }
        }
        if(!exist){
            nodes.add(nodeInfo);
            updateStench(nodeInfo.getId(), nodeInfo.isStench(), nodeInfo.getStenchTimer());
        }
    }

    /**
     * Merge own list with the list given as parameter
     * @param mapNodes
     */
    public void update(MapNodes mapNodes){

        for(NodeInfo ni:mapNodes.getNodes()){
            this.updateNode(ni);
        }
        updateLists();

    }

    /**
     * Update all the lists of specific nodes (diamond/gold/open/close)
     */
    public void updateLists(){
        this.diamondNodes.clear();
        this.goldNodes.clear();
        this.closeNodes.clear();
        this.openNodes.clear();
        this.chestNodes.clear();
        this.stenchNodes.clear();
        this.wumpusNodes.clear();
        //Update the lists considering the information we have in our list containing all the data
        for(NodeInfo ni:this.nodes){
            if(ni.getType().equals(NodeInfo.NodeType.gold)){
                this.goldNodes.add(ni);
                this.closeNodes.add(ni);
            }
            if(ni.getType().equals(NodeInfo.NodeType.diamond)){
                this.diamondNodes.add(ni);
                this.closeNodes.add(ni);
            }
            if(ni.getType().equals(NodeInfo.NodeType.close)){
                this.closeNodes.add(ni);
            }
            if(ni.getType().equals(NodeInfo.NodeType.open)){
                this.openNodes.add(ni);
            }
            if(ni.getType().equals(NodeInfo.NodeType.wumpus)){
                this.wumpusNodes.add(ni);
            }
            if(!ni.isOpen()){
                this.chestNodes.add(ni);
            }
            if(ni.isStench() && new Date().getTime() - ni.getStenchTimer()<1000){
                this.stenchNodes.add(ni);
            }
        }
    }

    public void updateStench(String id, boolean stench, long stenchTimer){
        for(NodeInfo ni: nodes){
            if(ni.getId().equals(id)){
                ni.setStench(stench);
                ni.setStenchTimer(stenchTimer);
            }
        }
    }

    public boolean isClose(String id){
        for(NodeInfo ni:closeNodes){
            if(ni.getId().equals(id)){
                return true;
            }
        }
        return false;
    }

    public boolean isOpen(String id){
        for(NodeInfo ni:openNodes){
            if(ni.getId().equals(id)){
                return true;
            }
        }
        return false;
    }

    public void setWumpusToOpen(){
        //reset old nodes and wumpus nodes.
        ArrayList<NodeInfo> nodes2 = new ArrayList<>();
        for(NodeInfo ni:nodes){
            if(ni.getType().equals(NodeInfo.NodeType.wumpus) || new Date().getTime() - ni.getTimer() > 5000){
                nodes2.add(new NodeInfo(ni.getId(), NodeInfo.NodeType.open, new Date().getTime()));
            }
            else{
                nodes2.add(new NodeInfo(ni.getId(), ni.getType(), ni.getTimer()));
            }
        }
        nodes = nodes2;
    }

    public ArrayList<NodeInfo> getNodes() {
        return nodes;
    }

    public ArrayList<NodeInfo> getGoldNodes() {
        return goldNodes;
    }

    public ArrayList<NodeInfo> getDiamondNodes() {
        return diamondNodes;
    }

    public ArrayList<NodeInfo> getCloseNodes() {
        return closeNodes;
    }

    public ArrayList<NodeInfo> getOpenNodes() {
        return openNodes;
    }

    public ArrayList<NodeInfo> getChestNodes() {
        return chestNodes;
    }

    public ArrayList<NodeInfo> getWumpusNodes() {
        return wumpusNodes;
    }

    public ArrayList<String> getWumpusString(){
        ArrayList<String> res = new ArrayList<>();
        for(NodeInfo ni: wumpusNodes){
            res.add(ni.getId());
        }
        return res;
    }

    public ArrayList<NodeInfo> getTreasureNodes() {
        ArrayList<NodeInfo> treasureNodes = new ArrayList<>();
        treasureNodes.addAll(this.goldNodes);
        treasureNodes.addAll(this.diamondNodes);
        return treasureNodes;
    }

    public ArrayList<String> getStenchNodes() {
        ArrayList<String> res = new ArrayList<>();
        for(NodeInfo ni: stenchNodes){
            res.add(ni.getId());
        }
        return res;
    }

    public ArrayList<String> getRecentWumpusString(){
        ArrayList<String> res = new ArrayList<>();
        for(NodeInfo ni: wumpusNodes){
            if(new Date().getTime() - ni.getTimer()<3000){
                res.add(ni.getId());
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return nodes.toString();
    }
}
