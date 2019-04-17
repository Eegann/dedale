package eu.su.mas.dedaleEtu.mas.behaviours;

import com.sun.xml.internal.bind.v2.TODO;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MessageInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.NodeInfo;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class ReceiveMessageBehaviour extends SimpleBehaviour {
    private boolean finished = true;

    private int repeat;
    private int actuRepeat;
    private int endCode;

    MapRepresentation myMap;
    /**
     * This behaviour handle all the messages the agent may receive during the exploration.
     *
     * @param myagent
     * @param myMap
     */
    public ReceiveMessageBehaviour(final Agent myagent, MapRepresentation myMap, int repeat) {
        super(myagent);
        this.myMap = myMap;
        this.repeat = repeat;
        this.actuRepeat = repeat;
        this.endCode = 0;
    }

    /**
     * This method will use the messages we just get.
     * We will update the map so they fit the data we just received.
     */
    public void action() {

        //1) receive the message
        final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

        final ACLMessage msg = this.myAgent.receive(msgTemplate);

        if(msg != null) {
            try {
                MessageInfo messageInfo = (MessageInfo) msg.getContentObject();
                //System.out.println(this.myAgent.getLocalName() + " Message received from "+msg.getSender().getLocalName());
                ArrayList<String> edges = messageInfo.getEdges();
                HashMap<String, AgentInfo> agentsInfo = messageInfo.getAgentsInfo();
                this.myMap.updateMapNodes(messageInfo.getMapNodes());
                String string = "";
                for(NodeInfo ni: messageInfo.getMapNodes().getNodes()){
                    string = string+" "+ni.getId();
                }
                boolean priority = messageInfo.isPriority();
                for (String s : edges) {
                    String split[] = s.split(" ");
                    myMap.addEdge(split[0],split[1]);
                }
                //Update the position of the agent sending the message
                this.myMap.updateAgentsPriority(msg.getSender().getLocalName(), priority);
                for(String name: agentsInfo.keySet()){
                    this.myMap.updateAgentsInfo(name, agentsInfo.get(name));
                }

            } catch (Exception e) {
                System.err.print("ReceiveMessageBehaviour");
                e.printStackTrace();
            }
        }
    }

    public void setMyMap(MapRepresentation myMap){
        this.myMap = myMap;
    }

    public boolean done() {
        return finished;
    }

    public int onEnd(){
        if(actuRepeat ==0){
            actuRepeat = repeat;
            return endCode;
        }
        else{
            actuRepeat --;
            return 1;
        }

    }

    public void setOnEnd(int x){
        endCode = x;
    }
}
