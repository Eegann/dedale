package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MessageInfo;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.HashMap;

public class SendMessageBehaviour extends SimpleBehaviour {
    private boolean finished = true;

    ArrayList<String> agentNames;
    HashMap<String, Integer> lastCall;

    MessageInfo data;

    public SendMessageBehaviour(ArrayList<String> agentNames){
        this.agentNames = agentNames;
        //Init the last call hashmap
        this.lastCall = new HashMap<>();
        for(String name: agentNames){
            this.lastCall.put(name,6);
        }
    }
    @Override
    public void action() {

        for( String name: agentNames){
            lastCall.replace(name, lastCall.get(name)+1);
        }
        for(String name: agentNames){
            //Send te message only if the last message to this agent is more than 5 "steps" ago
            if(lastCall.get(name)>=0) {
                lastCall.replace(name, 0);
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.setSender(this.myAgent.getAID());
                try {
                    message.setContentObject(this.data);
                } catch (Exception e) {
                    System.err.print(this.myAgent.getLocalName() + " " + e);
                    e.printStackTrace();
                }
                message.addReceiver(new AID(name, AID.ISLOCALNAME));
                ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
            }
        }

    }

    public void updateMessage(MessageInfo data){
        this.data = data;
    }

    @Override
    public boolean done() {
        return finished;
    }

    @Override
    public int onEnd() {
        return 0;
    }
}
