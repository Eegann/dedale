package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
public class ReceivePingBehaviour extends SimpleBehaviour {
    private boolean finished = true;
    private boolean isPing = false;
    int endCode = 1;

    /**
     * This method will use the messages we just get.
     * We will update the map so they fit the data we just received.
     */
    public void action() {

        //1) receive the message
        final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        ACLMessage msg = this.myAgent.receive(msgTemplate);

        if(msg != null) {
            try {

                String text = msg.getContent();

                if(text.equals("ping")){
                    isPing = true;
                }
            } catch (Exception e) {
                System.err.print("ReceivePingBehaviour");
                e.printStackTrace();
            }
            while(msg != null){
                msg = this.myAgent.receive(msgTemplate);
            }
        }

        else{
            this.myAgent.blockingReceive(300);
        }
    }

    public boolean done() {
        return finished;
    }

    public int onEnd(){
        if(isPing){
            return 0;
        }
        return endCode;

    }

    public void setOnEnd(int x){
        endCode = x;
    }
}