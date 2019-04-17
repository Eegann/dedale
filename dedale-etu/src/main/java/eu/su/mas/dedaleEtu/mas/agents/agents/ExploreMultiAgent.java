package eu.su.mas.dedaleEtu.mas.agents.agents;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.*;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ExploreMultiAgent extends AbstractDedaleAgent {

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
            this.doWait(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Get all the agentsExplo that are connected.
        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults((long)-1);
        ArrayList<String> agentNames = new ArrayList();
        HashMap<String, Set<Couple<Observation, Integer>>> agentsExpertise = new HashMap<>();

        try {
            AMSAgentDescription[] evalAgents = AMSService.search(this, new AMSAgentDescription(), sc);
            for (int i = 0; i < evalAgents.length; i++) {
                if ((evalAgents[i].getName().getLocalName().startsWith("Tanker") || evalAgents[i].getName().getLocalName().startsWith("Explo") || evalAgents[i].getName().getLocalName().startsWith("Collect")) && !evalAgents[i].getName().getLocalName().equals(this.getLocalName())) {
                    agentNames.add(evalAgents[i].getName().getLocalName());
                    //agentsExpertise.put(evalAgents[i].getName().getLocalName()
                }
            }
        } catch (Exception e){
            System.err.print("ExploreMultiAgent");
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
        ExploTreasureBehaviour egb = new ExploTreasureBehaviour(this, this.myMap, agentNames, smb, rmb, rpb);
        OpenChestMultiBehaviour ocmb = new OpenChestMultiBehaviour(this, this.myMap, agentNames, rmb, rpb, smb);
        ExploMultiBehaviour emb = new ExploMultiBehaviour(this, this.myMap, agentNames, rmb, rpb, smb, egb, null, ocmb);
        StateExploBehaviour sb = new StateExploBehaviour(emb, spb, rpb, smb, rmb, egb, ocmb);

        lb.add(sb);


        /***
         * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
         */


        addBehaviour(new startMyBehaviours(this,lb));

        System.out.println(this.getLocalName()+" known agents "+this.getLocalName()+ " is started");
    }

}
