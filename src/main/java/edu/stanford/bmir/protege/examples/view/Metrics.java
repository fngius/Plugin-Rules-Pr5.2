package edu.stanford.bmir.protege.examples.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.protege.editor.core.ui.action.NewAction;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLPredicate;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SWRLVariable;

class Pair
{
	// Return a map entry (key-value pair) from the specified values
	public static <T, U> Map.Entry<T, U> of(T first, U second)
	{
		return new AbstractMap.SimpleEntry<>(first, second);
	}
}

public class Metrics extends JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JButton refreshButton = new JButton("Refresh");
	//In a container that uses a BorderLayout:
	private JTextArea textArea = new JTextArea(5, 30);
	
	private JScrollPane scrollPane = new JScrollPane(textArea);
	
	private JTextArea textAreaGraph = new JTextArea(5, 30);

	private JScrollPane scrollPaneGraph = new JScrollPane(textAreaGraph);
	
	private JLabel textComponent = new JLabel();
    
    private OWLModelManager modelManager;
    
    private ActionListener refreshAction = e -> recalculate();

    private OWLModelManagerListener modelListener = event -> {
        if (event.getType() == EventType.ACTIVE_ONTOLOGY_CHANGED) {
            recalculate();
        }
    };
    
	/*public Set<SWRLRule> getRules(SWRLRule rule, Set<org.jgrapht.alg.util.Pair<SWRLRule,Set<SWRLRule>>> graph) {
		for (Iterator<?> it = graph.iterator(); it.hasNext();) {
			org.jgrapht.alg.util.Pair<SWRLRule,Set<SWRLRule>> aPair = (org.jgrapht.alg.util.Pair<SWRLRule, Set<SWRLRule>>) it.next();
			if (rule == aPair.getFirst())
				return aPair.getSecond();
		}
		return null;
	}
    */
    public Metrics(OWLModelManager modelManager) {
    	this.modelManager = modelManager;
        recalculate();
        
        modelManager.addListener(modelListener);
        refreshButton.addActionListener(refreshAction);
        setLayout(new BorderLayout());
        
        textComponent.setForeground(Color.RED);
        Font font = new Font("SansSerif", Font.BOLD, 14);
        textComponent.setFont(font);
        add(textComponent, BorderLayout.PAGE_START);
        
        textArea.setEditable(false);
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(Color.WHITE);
        Font myFont = new Font("SansSerif", Font.BOLD, 14);
        textArea.setFont(myFont);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Rule Paraphrasing"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrollPane.setPreferredSize(new Dimension(600, 500));
        add(scrollPane, BorderLayout.LINE_START);
        
        textAreaGraph.setEditable(false);
        scrollPaneGraph.setPreferredSize(new Dimension(600, 500));
        textAreaGraph.setBackground(Color.BLACK);
        textAreaGraph.setForeground(Color.WHITE);
        Font myFont1 = new Font("SansSerif", Font.BOLD, 14);
        textAreaGraph.setFont(myFont1);
        scrollPaneGraph.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Rule Graph Visualization"),
                BorderFactory.createEmptyBorder(5,5,5,5)));        
        add(scrollPaneGraph, BorderLayout.LINE_END);
        
        add(refreshButton,BorderLayout.PAGE_END);
        /*
        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
            Object v1 = graph.insertVertex(parent, null, "Hello", 20, 20, 80,30);
            Object v2 = graph.insertVertex(parent, null, "World!", 240, 150,80, 30);
            graph.insertEdge(parent, null, "Edge", v1, v2);
        } finally {
            graph.getModel().endUpdate();
        }
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        //GraphFrame frame = new GraphFrame();
        add(graphComponent,BorderLayout.CENTER);
        setVisible(true);
        */
    }

    public void dispose() {
        modelManager.removeListener(modelListener);
        refreshButton.removeActionListener(refreshAction);
    }

	private void recalculate() {
        int count = modelManager.getActiveOntology().getClassesInSignature().size();
        if (count == 0) {
            count = 1;  // owl:Thing is always there.
        }
        
        //Set<org.jgrapht.alg.util.Pair<SWRLRule,Set<SWRLRule>>> tree;
        //Set<org.jgrapht.alg.util.Pair<Integer,Set<Integer>>> tree = null;
        Pair p1 = new Pair();
        // Structure to store rules.
        Set<SWRLRule> SetOfRules = modelManager.getActiveOntology().getAxioms(AxiomType.SWRL_RULE);
        
        // Check if the ontology has any SWRL Rule
        if (SetOfRules.isEmpty()) {
        	scrollPane.setVisible(false);
        	scrollPaneGraph.setVisible(false);
        	refreshButton.setVisible(false);
        	textComponent.setText("The load ontology has no SWRL Rules");
        	return;
        }
        
        ArrayList<ArrayList<SWRLAtom>> ruleBodies = new ArrayList<ArrayList<SWRLAtom>>();
        ArrayList<ArrayList<SWRLAtom>> ruleHeads = new ArrayList<ArrayList<SWRLAtom>>();
        String atomsInBody = null;
        String atomsInHead = null;
        
        ArrayList<ArrayList<Integer>> tree = new ArrayList<ArrayList<Integer>>();

        int i = 0;
        String allTheRules = "";
        
        // Loop for all the SWRL rules in the Ontology
        for (Iterator<?> it = SetOfRules.iterator(); it.hasNext(); i++) {            
        	SWRLRule rule = (SWRLRule) it.next();

        	ArrayList<SWRLAtom> bodyOfRule = new ArrayList<SWRLAtom>();
        	ArrayList<SWRLAtom> headOfRule = new ArrayList<SWRLAtom>();
        	
            atomsInBody = "";
            //System.out.println(bodyOfRule.toString()+"\n");
            // Loop for the body of a rule
            for (Iterator<?> it1 = rule.getBody().iterator(); it1.hasNext();) {                
            	SWRLAtom atom = (SWRLAtom) it1.next();
                SWRLArgument atomArgument = null;
                String var = "";
                String completeAtom = "";                
                // Loop for the Arguments of an Atom           
                for (Iterator<?> it2 = atom.getAllArguments().iterator(); it2.hasNext();) {
                	// Check if the atom is an object property or an unary predicate
                	if (atom.getAllArguments().size()<2) {                        
                		atomArgument = (SWRLArgument) it2.next();
                		// Here it is necessary to check the instance type of atomArgument (We will see this after)
                		//if (!(atomArgument instanceof SWRLIndividualArgument))
                		var = ((SWRLVariable) atomArgument).getIRI().getShortForm();
                		completeAtom = "'"+var+"'" +" is a " + atom.getPredicate().toString();
                	} else {
                		atomArgument = (SWRLArgument) it2.next();
                		// Here it is necessary to check the instance type of atomArgument (We will see this after)
                		//if (!(atomArgument instanceof SWRLIndividualArgument))
                		var = ((SWRLVariable) atomArgument).getIRI().getShortForm();
                		if (!completeAtom.isEmpty())
                			completeAtom += " " + atom.getPredicate().toString() + " " + "'" + var + "'";
                		else
                			completeAtom += "'" + var + "'";
                	}
                }
                // Check if it is the last atom of the body
                // to see if it is necessary to add "AND" or not
                if (it1.hasNext())
                	atomsInBody += completeAtom + "\n    AND ";
                else
                	atomsInBody += completeAtom;
                
                bodyOfRule.add(atom);
            }
            ruleBodies.add(bodyOfRule);
            atomsInHead = "";
            // Loop for the head of a rule
            for (Iterator<?> it1 = rule.getHead().iterator(); it1.hasNext();) {
            	SWRLAtom atom = (SWRLAtom) it1.next();
                SWRLArgument atomArgument = null;
                String var = "";
                String completeAtom = "";
                // Loop for the Arguments of an Atom
                for (Iterator<?> it3 = atom.getAllArguments().iterator(); it3.hasNext(); ) {
                	// Check if the atom is an object property or an unary predicate
                	if (atom.getAllArguments().size()<2) {                        
                		atomArgument = (SWRLArgument) it3.next();
                		// Here it is necessary to check the instance type of atomArgument (We will see this after)
                		//if (!(atomArgument instanceof SWRLIndividualArgument))
                		var = ((SWRLVariable) atomArgument).getIRI().getShortForm();
                		completeAtom = "'" + var + "'" +" is a " + atom.getPredicate().toString();
                	}
                	else {
                		atomArgument = (SWRLArgument) it3.next();
                		// Here it is necessary to check the instance type of atomArgument (We will see this after)
                		//if (!(atomArgument instanceof SWRLIndividualArgument))
                		var = ((SWRLVariable) atomArgument).getIRI().getShortForm();
                		if (!completeAtom.isEmpty())
                			completeAtom += " " + atom.getPredicate().toString() + " " + "'" + var + "'";
                		else
                			completeAtom += "'" + var + "'";
                	}
                }
                // Check if it is the last atom of the head
                // to see if it is necessary to add "AND" or not
                if (it1.hasNext())
                	atomsInHead += completeAtom + "\n    AND ";
                else
                	atomsInHead += completeAtom;
                
                headOfRule.add(atom);
            }
            ruleHeads.add(headOfRule);

            allTheRules += "RULE_" + i + ":\nIF\n" + "    " + atomsInBody + "\nTHEN\n" + "    " + atomsInHead + "\n\n**************************************************\n\n";
        }

        textArea.setText(allTheRules);
        
        // Graph of RULES
        NaryTreeNode root = new NaryTreeNode(1);
        
        String graph = "";
        for (int h = 0; h<ruleHeads.size();h++) {
        	ArrayList<Integer> linkNodes = new ArrayList<Integer>();
        	graph += "RULE_" + h + "\n";
        	for (int b = 0; b<ruleBodies.size();b++) { 
            	ArrayList<SWRLPredicate> list = new ArrayList<SWRLPredicate>();
            	list = (ArrayList<SWRLPredicate>) ruleBodies.get(b).stream().map(at -> at.getPredicate()).collect(Collectors.toList());
            	if (list.contains(ruleHeads.get(h).get(0).getPredicate())) {
        			//System.out.println(h +"--->"+ b);
            		//graph += "RULE_" + h + " --> " + "RULE_" +  b + "\n";
            		linkNodes.add(b);
            		graph += "             --> " + "RULE_" +  b + "\n";
        		}
        	}
        	tree.add(linkNodes);
        	graph += "\n";
        }
        textAreaGraph.setText(graph);
        
        // Graph of RULES        
        for (Iterator<?> it = tree.iterator(); it.hasNext();) {
        	System.out.println(it.next().toString() + "   ");
        }
    }

	
	
	public String name(ArrayList<Integer> list) {
		String output = "";
		if (list.isEmpty())
			return "";
		else
			for(int i = 0; i<list.size(); i++) {
				output += "--->" + list.get(i) + name(list);	
			}
		return output;
	}


	public class NaryTreeNode {
	    //final String LABEL;
	    final Integer LABEL;
	    //final int N;
	    private final ArrayList<NaryTreeNode> children;

	    public NaryTreeNode(Integer LABEL/*, int n*/) {
	        this.LABEL = LABEL;
	        //this.N = n;
	        children = new ArrayList<>();//ArrayList<>(n);
	    }

	    private boolean addChild(NaryTreeNode node) {
	        //if (children.size() < N) {
	            return children.add(node);
	        //}

	        //return false;
	    }

	    public boolean addChild(Integer label) {
	        //return addChild(new NaryTreeNode(label, N));
	        return addChild(new NaryTreeNode(label));
	    }

	    public ArrayList<NaryTreeNode> getChildren() {
	        return new ArrayList<>(children);
	    }

	    public NaryTreeNode getChild(int index) {
	        if (index < children.size()) {
	            return children.get(index);
	        }

	        return null;
	    }

	    public void print(NaryTreeNode root) {
	        printUtil(root, 0);
	    }

	    private void printUtil(NaryTreeNode node, int depth) {
	        for (int i = 0; i < depth; ++i) {
	            System.out.print("   ");
	        }

	        System.out.println(node.LABEL);

	        for (NaryTreeNode child : node.getChildren()) {
	            printUtil(child, depth + 1);
	        }
	    }
	}

	/*
	class TestNaryTree {
	    public static void main(String[] args) {
	        int n = 3;
	        //NaryTreeNode root = new NaryTreeNode("Matter", n);
	        NaryTreeNode root = new NaryTreeNode("Matter");

	        root.addChild("Pure");
	            root.getChild(0).addChild("Elements");
	                root.getChild(0).getChild(0).addChild("Metals");
	                root.getChild(0).getChild(0).addChild("Metalloids");
	                root.getChild(0).getChild(0).addChild("Non-metals");
	            root.getChild(0).addChild("Compounds");
	                root.getChild(0).getChild(1).addChild("Water");
	                root.getChild(0).getChild(1).addChild("Carbon dioxide");
	                root.getChild(0).getChild(1).addChild("Salt");
	                root.getChild(0).getChild(1).addChild("Camphor");  // won't add
	        root.addChild("Mixture");
	            root.getChild(1).addChild("Homogeneous");
	                root.getChild(1).getChild(0).addChild("Air");
	                root.getChild(1).getChild(0).addChild("Vinegar");
	            root.getChild(1).addChild("Heterogeneous");
	                root.getChild(1).getChild(1).addChild("Colloids");
	                root.getChild(1).getChild(1).addChild("Suspensions");

	        NaryTreeNode.print(root);
	    }
	}
	*/
	
}