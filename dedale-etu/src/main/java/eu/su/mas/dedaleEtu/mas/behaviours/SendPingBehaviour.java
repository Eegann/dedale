package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;

public class SendPingBehaviour extends SimpleBehaviour {
    private boolean finished = true;

    ArrayList<String> agentNames;

    public SendPingBehaviour(final Agent myAgent, ArrayList<String> agentNames){
        super(myAgent);
        this.agentNames = agentNames;
    }
    @Override
    public void action() {

        for(String name: agentNames){
            ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
            message.setSender(this.myAgent.getAID());
            message.addReceiver(new AID(name, AID.ISLOCALNAME));
            message.setContent("ping");
            ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
        }

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
