package eu.su.mas.dedaleEtu.mas.knowledge;


import java.io.Serializable;

public class NodeInfo implements Serializable {
    public enum NodeType {
        open, wumpus,  close, gold, diamond
    }
    private String id;
    private NodeType type;
    private long timer;
    private int quantity;
    private boolean open;
    private int lockpicking;
    private int strength;
    private boolean stench;
    private long stenchTimer;

    public NodeInfo(String id, NodeType type, long timer){
        this.id = id;
        this.type = type;
        this.timer = timer;
        this.quantity = 0;
        this.open = true;
        this.lockpicking = 0;
        this.strength = 0;
        this.stench = false;
        this.stenchTimer = 0;
    }

    public NodeInfo(String id, NodeType type, long timer, int qty, boolean open, int lockpicking, int strength){
        this.id = id;
        this.type = type;
        this.timer = timer;
        this.quantity = qty;
        this.open = open;
        this.lockpicking = lockpicking;
        this.strength = strength;
        this.stench = false;
        this.stenchTimer = 0;

    }

    public void update(NodeInfo nodeInfo){

        this.timer = nodeInfo.getTimer();
        this.quantity = nodeInfo.getQuantity();
        if(quantity >0){
            this.type = nodeInfo.getType();
        }
        else{
            if(nodeInfo.getType().equals(NodeType.wumpus)){
                this.type = NodeType.wumpus;
            }
            else {
                this.type = NodeType.close;
            }
        }
        this.open = nodeInfo.isOpen();
        this.strength = nodeInfo.getStrength();
        this.lockpicking = nodeInfo.getLockpicking();
    }

    public void setStench(boolean stench) {
        this.stench = stench;
    }

    public void setStenchTimer(long stenchTimer){
        this.stenchTimer = stenchTimer;
    }

    public long getTimer() {
        return timer;
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isOpen() {
        return open;
    }

    public int getStrength() {
        return strength;
    }

    public int getLockpicking() {
        return lockpicking;
    }

    public long getStenchTimer() {
        return stenchTimer;
    }

    public boolean isStench() {
        return stench;
    }

    @Override
    public String toString() {
        if(type == NodeType.open || type == NodeType.close) {
            return this.id + " " + this.type.toString() + " " + this.timer;
        }
        else{
            return this.id + " " + this.type.toString() + " " + this.quantity + " " + this.timer + " " + this.open + " "+ this.strength + " " + this.lockpicking;
        }
    }
}
