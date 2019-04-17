package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.behaviours.FSMBehaviour;

public class StateExploBehaviour extends FSMBehaviour {

    public StateExploBehaviour(ExploMultiBehaviour emb, SendPingBehaviour spb, ReceivePingBehaviour rpb, SendMessageBehaviour smb, ReceiveMessageBehaviour rmb, ExploTreasureBehaviour etb, OpenChestMultiBehaviour opcm){
        this.registerFirstState(emb, "Exploration");
        this.registerState(spb, "Send ping");
        this.registerState(rpb, "Receive ping");
        this.registerState(smb, "Send data");
        this.registerState(rmb, "Receive message");
        this.registerState(etb, "Treasure checking");
        this.registerState(opcm, "Chest unlocking");

        //When a behaviour returns 0 => next "normal" state
        this.registerTransition("Exploration", "Send ping", 0);
        this.registerTransition("Treasure checking", "Send ping", 0);
        this.registerTransition("Chest unlocking", "Send ping", 0);

        this.registerTransition("Send ping", "Receive ping", 0);

        this.registerTransition("Receive ping", "Exploration", 1);
        this.registerTransition("Receive ping", "Send data", 0);
        this.registerTransition("Receive ping", "Chest unlocking", 2);
        this.registerTransition("Receive ping", "Treasure checking", 3);

        this.registerTransition("Send data", "Receive message", 0);

        this.registerTransition("Receive message", "Receive message", 1);
        this.registerTransition("Receive message", "Exploration", 0);
        this.registerTransition("Receive message", "Chest unlocking", 2);
        this.registerTransition("Receive message", "Treasure checking", 3);

        this.scheduleFirst();
    }
}
