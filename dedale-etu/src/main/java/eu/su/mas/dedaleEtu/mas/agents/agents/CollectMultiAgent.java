package eu.su.mas.dedaleEtu.mas.agents.agents;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.*;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.NodeInfo;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;

import java.util.ArrayList;
import java.util.List;

public class CollectMultiAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = -6431752665590433727L;
    private MapRepresentation myMap;


    /**
     * This method is automatically called when "agent".start() is executed.
     * Consider that Agent is launched for the first time.
     * 			1) set the agent attributes
     *	 		2) add the behaviours
     *
     */
    protected void setup(){

        super.setup();
        try {
            this.doWait(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Get all the agentsExplo that are connected.
        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults((long)-1);
        ArrayList<String> agentNames = new ArrayList<>();
        try {
            AMSAgentDescription[] evalAgents = AMSService.search(this, new AMSAgentDescription(), sc);
            for (int i = 0; i < evalAgents.length; i++) {
                if ((evalAgents[i].getName().getLocalName().startsWith("Tanker") || evalAgents[i].getName().getLocalName().startsWith("Explo") || evalAgents[i].getName().getLocalName().startsWith("Collect")) && !evalAgents[i].getName().getLocalName().equals(this.getLocalName())) {
                    agentNames.add(evalAgents[i].getName().getLocalName());
                }
            }
        } catch (Exception e){
            System.err.print("CollectMultiAgent");
            e.printStackTrace();
        }



        List<Behaviour> lb=new ArrayList<Behaviour>();

        /************************************************
         *
         * ADD the behaviours of the Dummy Moving Agent
         *
         ************************************************/

        ReceiveMessageBehaviour rmb = new ReceiveMessageBehaviour(this, this.myMap, agentNames.size());
        SendMessageBehaviour smb = new SendMessageBehaviour(agentNames);
        SendPingBehaviour spb = new SendPingBehaviour(this, agentNames);
        ReceivePingBehaviour rpb = new ReceivePingBehaviour();
        CollectBehaviour cb;
        if(this.getMyTreasureType().getName().equals("Gold")){
            cb = new CollectBehaviour(this, this.myMap, agentNames, rmb, smb, NodeInfo.NodeType.gold, rpb, rmb);
        }
        else{
            cb = new CollectBehaviour(this, this.myMap, agentNames, rmb, smb, NodeInfo.NodeType.diamond, rpb, rmb);
        }
        OpenChestMultiBehaviour ocmb = new OpenChestMultiBehaviour(this, this.myMap, agentNames, rmb, rpb, smb);
        ExploMultiBehaviour emb = new ExploMultiBehaviour(this, this.myMap, agentNames, rmb, rpb, smb, null, cb, ocmb);
        StateCollectBehaviour sb = new StateCollectBehaviour(emb, spb, rpb, smb, rmb, cb, ocmb);

        lb.add(sb);


        /***
         * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
         */


        addBehaviour(new startMyBehaviours(this,lb));

        System.out.println(this.getLocalName()+" known agents "+this.getLocalName()+ " is started");
    }

}