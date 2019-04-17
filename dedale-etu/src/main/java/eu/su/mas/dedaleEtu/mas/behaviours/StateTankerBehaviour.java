package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.agents.ExploreMultiAgent;
import jade.core.behaviours.FSMBehaviour;

public class StateTankerBehaviour extends FSMBehaviour {

    public StateTankerBehaviour(ExploMultiBehaviour emb, SendPingBehaviour spb, ReceivePingBehaviour rpb, SendMessageBehaviour smb, ReceiveMessageBehaviour rmb, TankerBehaviour tb){
        this.registerFirstState(emb, "Explo");
        this.registerState(tb, "Tanker");
        this.registerState(spb, "Send ping");
        this.registerState(rpb, "Receive ping");
        this.registerState(smb, "Send data");
        this.registerState(rmb, "Receive message");

        this.registerTransition("Tanker", "Send ping", 0);
        this.registerTransition("Explo", "Send ping", 0);

        this.registerTransition("Send ping", "Receive ping", 0);

        this.registerTransition("Receive ping", "Send data", 0);
        this.registerTransition("Receive ping", "Explo", 1);
        this.registerTransition("Receive ping", "Tanker", 2);

        this.registerTransition("Send data", "Receive message", 0);

        this.registerTransition("Receive message", "Receive message", 1);
        this.registerTransition("Receive message", "Explo", 0);
        this.registerTransition("Receive message", "Tanker", 2);

        this.scheduleFirst();
    }
}
