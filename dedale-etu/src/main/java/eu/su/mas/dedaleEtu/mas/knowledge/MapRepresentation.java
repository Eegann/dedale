package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.*;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

/**
 * This simple topology representation only deals with the graph, not its content.</br>
 * The knowledge representation is not well written (at all), it is just given as a minimal example.</br>
 * The viewer methods are not independent of the data structure, and the dijkstra is recomputed every-time.
 * 
 * @author hc
 */
public class MapRepresentation implements Serializable {

	public enum MapAttribute {
		agent,open
	}

	private static final long serialVersionUID = -1333959882640838272L;

	private Graph g; //data structure
	private Viewer viewer; //ref to the display
	private Integer nbEdges;//used to generate the edges ids
	
	/*********************************
	 * Parameters for graph rendering
	 ********************************/
	
	private String defaultNodeStyle= "node {"+"fill-color: black;"+" size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = "node.agent {"+"fill-color: forestgreen;"+"}";
	private String nodeStyle_agent = "node.open {"+"fill-color: blue;"+"}";
	private String nodeStyle=defaultNodeStyle+nodeStyle_agent+nodeStyle_open;
	private HashMap<String, Boolean> agentsPriority;
	private MapNodes mapNodes;

	private HashMap<String, AgentInfo> agentsInfo;



	public MapRepresentation(List<String> agentNames) {
		System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);
		this.viewer = this.g.display();
		this.nbEdges=0;
		this.mapNodes = new MapNodes();
		this.agentsPriority = new HashMap<>();
		this.agentsInfo = new HashMap<>();
		for(String name: agentNames){
			agentsPriority.put(name, false);
			agentsInfo.put(name, new AgentInfo(new ArrayList<>(), "-1", null, 0));
		}
	}

	/**
	 * Associate to a node an attribute in order to identify them by type. 
	 * @param id
	 * @param mapAttribute
	 */
	//TODO
	public void addNode(String id,MapAttribute mapAttribute){
		Node n;
		if (this.g.getNode(id)==null){
			n=this.g.addNode(id);
		}else{
			n=this.g.getNode(id);
		}
		n.clearAttributes();
		n.addAttribute("ui.class", mapAttribute.toString());
		n.addAttribute("ui.label",id);
	}

	/**
	 * Add the node id if not already existing
	 * @param id
	 */
	public void addNode(String id){
		Node n=this.g.getNode(id);
		if(n==null){
			n=this.g.addNode(id);
		}else{
			n.clearAttributes();
		}
		n.addAttribute("ui.label",id);
	}

   /**
    * Add the edge if not already existing.
    * @param idNode1
    * @param idNode2
    */
	public void addEdge(String idNode1,String idNode2) {
		if(!idNode1.equals(idNode2)) {
			try {
				this.nbEdges++;
				this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
			} catch (EdgeRejectedException e) {
				//Do not add an already existing one
				this.nbEdges--;
			}
		}
	}

	public ArrayList<String> getEdges(){
	    ArrayList<String> edges = new ArrayList<>();
		for(int i=0;i<this.g.getEdgeCount();i++){
		    edges.add(this.g.getEdge(i).getNode0()+" "+this.g.getEdge(i).getNode1());
		}
		return edges;
	}


	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow
	 */
	public List<String> getShortestPath(String idFrom,String idTo){
		List<String> shortestPath=new ArrayList<String>();
		Graph g2= new SingleGraph("My world vision");
		Integer i=0;
		ArrayList<String> wumpusNodes = mapNodes.getRecentWumpusString();
		for(Node n: g.getNodeSet()){
			boolean add = true;
			for(String wumpusNode: wumpusNodes){
				if(wumpusNode.equals(n.toString()) && !wumpusNode.equals(idFrom)) {
					add = false;
				}
			}
			if(add){
				g2.addNode(n.getId());
			}

		}
		for(Edge e: g.getEdgeSet()){
			if(g2.getNode(e.getNode0().toString())!= null && g2.getNode(e.getNode1().toString()) != null){
				g2.addEdge(i.toString(), e.getNode0().toString(), e.getNode1().toString());
				i++;
			}
		}

		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g2);
		dijkstra.setSource(g2.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		//TODO render NULL
		List<Node> path=dijkstra.getPath(g2.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo

		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		if(shortestPath.size()>0) shortestPath.remove(0);//remove the current position
		return shortestPath;
	}

	public ArrayList<String> getMultiNodes(){
		ArrayList<String> multiNode = new ArrayList<>();
		for(Node n: g.getNodeSet()){
			if(n.getDegree()>2){
				multiNode.add(n.toString());
			}
		}
		return multiNode;
	}

	public ArrayList<String> getNeighbor(String node){
		Node n = g.getNode(node);
		ArrayList<String>neighbor = new ArrayList<>();
		if(n!=null){
		    Iterator<Node> ite = n.getNeighborNodeIterator();
            while(ite.hasNext()){
                neighbor.add(ite.next().toString());
            }
        }
		return neighbor;
	}

	public void updateAgentsInfo(String name, AgentInfo info){
		if(this.agentsInfo.containsKey(name)){
			if(this.agentsInfo.get(name).getTimer()<info.getTimer()){
				this.agentsInfo.replace(name, info);
			}
		}
		else{
			this.agentsInfo.put(name, info);
		}
	}

	public HashMap<String, AgentInfo> getAgentsInfo() {
		return agentsInfo;
	}

	public HashMap<String, String> getAgentsPosition(){
		HashMap<String, String> agentsPosition = new HashMap<>();
		for(String name: agentsInfo.keySet()){
			agentsPosition.put(name, agentsInfo.get(name).getPos());
		}
		return agentsPosition;
	}

	public HashMap<String, ArrayList<String>> getAgentsPath() {
		HashMap<String, ArrayList<String>> agentsPath = new HashMap<>();
		for(String name: agentsInfo.keySet()){
			agentsPath.put(name, agentsInfo.get(name).getPath());
		}
		return agentsPath;
	}

	public HashMap<String, Boolean> getAgentsPriority() {
		return agentsPriority;
	}

	public HashMap<String, Set<Couple<Observation, Integer>>> getAgentsExpertise() {
		HashMap<String, Set<Couple<Observation, Integer>>> agentsExpertise = new HashMap<>();
		for(String name: agentsInfo.keySet()){
			agentsExpertise.put(name, agentsInfo.get(name).getExpertise());
		}
		return agentsExpertise;
	}

	public HashMap<String, Long> getAgentsTimer(){
		HashMap<String,Long> agentsTimer = new HashMap<>();
		for(String name: agentsInfo.keySet()){
			agentsTimer.put(name, agentsInfo.get(name).getTimer());
		}
		return agentsTimer;
	}

	public void updateMapNodes(MapNodes mapNodes){
		this.mapNodes.update(mapNodes);
		for(NodeInfo ni:mapNodes.getOpenNodes()){
			this.addNode(ni.getId(), MapAttribute.open);
		}
		for(NodeInfo ni:mapNodes.getCloseNodes()){
			this.addNode(ni.getId());
		}
		for(NodeInfo ni:mapNodes.getGoldNodes()){
			this.addNode(ni.getId());
		}
		for(NodeInfo ni:mapNodes.getDiamondNodes()){
			this.addNode(ni.getId());
		}
		for(NodeInfo ni:mapNodes.getWumpusNodes()){
			this.addNode(ni.getId());
		}
	}

	public MapNodes getMapNodes() {
		return mapNodes;
	}

	public void updateAgentsPriority(String name, boolean priority){
		if (this.agentsPriority.containsKey(name)){
			this.agentsPriority.replace(name, priority);
		}
		else{
			this.agentsPriority.put(name, priority);
		}
	}
}
