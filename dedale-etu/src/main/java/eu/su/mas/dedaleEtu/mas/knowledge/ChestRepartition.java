package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;

import java.lang.reflect.Array;
import java.util.*;

public class ChestRepartition {

    private HashMap<String, Set<Couple<Observation, Integer>>> agentsExpertise;
    private ArrayList<String> agentsName;

    public ChestRepartition(HashMap<String, Set<Couple<Observation, Integer>>> agentsExpertise, ArrayList<String> agentsName, String name){
        this.agentsExpertise=agentsExpertise;
        this.agentsName = new ArrayList<>();
        this.agentsName.add(name);
        for(String s: agentsName){
            this.agentsName.add(s);
        }
    }

    public ArrayList<Couple<String, ArrayList<String>>> computeCoallitions(MapNodes mapNodes, HashMap<String, Set<Couple<Observation, Integer>>> agentsExpertise){
        this.agentsExpertise = agentsExpertise;
        ArrayList<NodeInfo> chestNodes = mapNodes.getChestNodes();
        Comparator<NodeInfo> compareById = (NodeInfo ni1, NodeInfo ni2) -> ni1.getId().compareTo( ni2.getId() );
        Collections.sort(chestNodes, compareById);
        Collections.sort(this.agentsName);

        boolean allAssigned = false;
        int i = 0;
        int j = 0;
        int totalStrength = 0;
        int totalLockpicking = 0;
        ArrayList<Couple<String, ArrayList<String>>> assignedAgents = new ArrayList<>();
        assignedAgents.add(new Couple<> (chestNodes.get(i).getId(), new ArrayList<>()));
        while(!allAssigned){
            String name = agentsName.get(j);
            int strength=0;
            int lockpicking=0;
            Set<Couple<Observation, Integer>> expertise = agentsExpertise.get(name);
            for(Couple<Observation, Integer> c: expertise){
                if(c.getLeft().getName().equals("Strength")){
                    strength+=c.getRight();
                }
                if(c.getLeft().getName().equals("LockPicking")){
                    lockpicking+=c.getRight();
                }
            }
            totalStrength+=strength;
            totalLockpicking+=lockpicking;
            assignedAgents.get(assignedAgents.size()-1).getRight().add(name);
            if(totalStrength>=chestNodes.get(i).getStrength() && totalLockpicking>=chestNodes.get(i).getLockpicking()){
                i++;
                totalLockpicking=0;
                totalStrength=0;
                if(i<chestNodes.size()){
                    assignedAgents.add(new Couple<> (chestNodes.get(i).getId(),new ArrayList<>()));
                }
                else{
                    allAssigned=true;
                }
            }

            j=(j+1)%agentsName.size();
        }
        Comparator<Couple<String, ArrayList<String>>> compareBySize = (Couple<String, ArrayList<String>> c1, Couple<String, ArrayList<String>> c2) -> c2.getRight().size() - c1.getRight().size();
        Collections.sort(assignedAgents, compareBySize);
        return assignedAgents;

    }

    public ArrayList<String> computeCoallitions(MapNodes mapNodes){
        ArrayList<NodeInfo> chestNodes = mapNodes.getChestNodes();
        Comparator<NodeInfo> compareById = (NodeInfo ni1, NodeInfo ni2) -> ni1.getId().compareTo( ni2.getId() );
        Collections.sort(chestNodes, compareById);
        ArrayList<String> res = new ArrayList<>();
        for (NodeInfo ni: chestNodes){
            res.add(ni.getId());
        }
        return res;

    }

}
