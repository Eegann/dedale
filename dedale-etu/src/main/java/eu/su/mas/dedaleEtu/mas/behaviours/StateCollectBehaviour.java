package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.behaviours.FSMBehaviour;

public class StateCollectBehaviour extends FSMBehaviour {

    public StateCollectBehaviour(ExploMultiBehaviour emb, SendPingBehaviour spb, ReceivePingBehaviour rpb, SendMessageBehaviour smb, ReceiveMessageBehaviour rmb, CollectBehaviour cb, OpenChestMultiBehaviour opcm){
        this.registerFirstState(emb, "Exploration");
        this.registerState(spb, "Send ping");
        this.registerState(rpb, "Receive ping");
        this.registerState(smb, "Send data");
        this.registerState(rmb, "Receive message");
        this.registerState(cb, "Collect");
        this.registerState(opcm, "Chest unlocking");

        //When a behaviour returns 0 => next "normal" state
        this.registerTransition("Exploration", "Send ping", 0);
        this.registerTransition("Collect", "Send ping", 0);
        this.registerTransition("Chest unlocking", "Send ping", 0);

        this.registerTransition("Send ping", "Receive ping", 0);

        //0, we received a response, 1, no response and still have to explore, 2, no response and still have to collect.
        this.registerTransition("Receive ping", "Exploration", 1);
        this.registerTransition("Receive ping", "Send data", 0);
        this.registerTransition("Receive ping", "Chest unlocking", 2);
        this.registerTransition("Receive ping", "Collect", 3);

        this.registerTransition("Send data", "Receive message", 0);

        this.registerTransition("Receive message", "Receive message", 1);
        this.registerTransition("Receive message", "Exploration", 0);
        this.registerTransition("Receive message", "Chest unlocking", 2);
        this.registerTransition("Receive message", "Collect", 3);



        this.scheduleFirst();
    }
}