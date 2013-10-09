/*********************************************************************************
Organization 					: 				Georgia Institute of Technology
  
Institute 						:				Cognitive Computing Group(CCL)
 
Authors							: 				Santiago Ontanon, Kinshuk Mishra
 												Neha Sugandh
 												
Class							:				CBLILRModule
 
Function						: 				The class provides helper function 
												to parse the XML and create DOM nodes
*********************************************************************************/
package base;

import java.io.File;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.metadata.IIOMetadataNode;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

abstract public class CBLILRModule implements Runnable {
	protected Thread m_t;
	protected boolean m_stopSignal=false;
	
	protected boolean m_busy=false;
	protected CBLILRModule m_main;
	protected LinkedList<Node> m_messageQueue;
	
	public CBLILRModule() {
		m_t=new Thread(this);
		m_main=null;
		m_messageQueue=new LinkedList<Node>();
	}
	
	public void start() {
		m_t.start();
	}
	
	public void run() {
		while(!m_stopSignal); 
	}
	
	public void stop() {
		m_stopSignal=true;
	}
	
	public boolean stopped() {
		return m_stopSignal;
	}
	
	public void join() {
		try{
			m_t.join();
		} catch(Exception e) {
		}
	}
	
	public boolean busyP() {
		return m_busy;
	}
	
	public void enqueueMessage(Node msg) {
		m_messageQueue.add(msg);
	}
	
	/*
	 * XML services for the CBL modules
	 */
	
	public static Node ReadXMLfromFile(String filename) {
		try{
			File file = new File(filename);

			// SAX Builder is used to load the XML and parse it
			SAXBuilder sb = new SAXBuilder(true);
			// Validation Schema for the XML
			sb.setFeature("http://apache.org/xml/features/validation/schema",true);
			org.jdom.Document jdoc = sb.build(file);

			List list = jdoc.getRootElement().getChildren();

			// for each message show some debug messages
			for(Iterator iter = list.iterator(); iter.hasNext(); ){
				Element currmsg = (Element)iter.next();
				try{
					System.out.println("debug: " + currmsg.getChild("label").getText());
				}catch(Exception e){}
			}

			DOMOutputter outputter = new DOMOutputter();
			org.w3c.dom.Document w3cdoc= outputter.output(jdoc);
			return cloneXML(w3cdoc.getChildNodes().item(0));
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static Node ReadXMLfromReader(Reader io) {
		try{
	
			// real stuff begins
			SAXBuilder sb = new SAXBuilder(false);
		//	sb.setFeature("http://apache.org/xml/features/validation/schema",true);
			org.jdom.Document jdoc = sb.build(io);

			List list = jdoc.getRootElement().getChildren();

			// for each message show some debug messages
			for(Iterator iter = list.iterator(); iter.hasNext(); ){
				Element currmsg = (Element)iter.next();
				try{
					System.out.println("debug: " + currmsg.getChild("label").getText());
				}catch(Exception e){}
			}

			DOMOutputter outputter = new DOMOutputter();
			org.w3c.dom.Document w3cdoc= outputter.output(jdoc);
			return cloneXML(w3cdoc.getChildNodes().item(0));
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	/*  
	 * Helper Function to create DOM node from an XML string
	 */
	public static Node ReadXMLfromString(String XML) {
		try{
			// real stuff begins
			SAXBuilder sb = new SAXBuilder(true);
			sb.setFeature("http://apache.org/xml/features/validation/schema",true);
			org.jdom.Document jdoc = sb.build(XML);

			List list = jdoc.getRootElement().getChildren();

			// for each message show some debug messages
			for(Iterator iter = list.iterator(); iter.hasNext(); ){
				Element currmsg = (Element)iter.next();
				try{
					System.out.println("debug: " + currmsg.getChild("label").getText());
				}catch(Exception e){}
			}

			DOMOutputter outputter = new DOMOutputter();
			org.w3c.dom.Document w3cdoc= outputter.output(jdoc);
			return cloneXML(w3cdoc.getChildNodes().item(0));
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * Helper function to cleanup the XML node and get rid 
	 * of the spaces, tab space and new line characters
	 */
	public static Node cleanup(Node d)
	{
		int i,n;
		Node att;
		NodeList l;
		NamedNodeMap attl;
		IIOMetadataNode result=null;
		
		boolean found = true;
		if (d.getNodeName().equals("#text")) {
			char [] nm = d.getNodeValue().toCharArray();
			char []st = new char[nm.length];
			int j =0;
			// remove spaces, new line characters and tab spaces
			for(int k =0; k < nm.length; k++)			
			{
				if((nm[k] != '\n') && (nm[k] != '\r') && (nm[k] != '\t') && (nm[k] != ' '))
				{
						found = false;
						st[j] = nm[k];
						j++;
				}		
			}	
			
			if(!found)
			{	
				String str = new String(st, 0, j);
				d.setNodeValue(str);
				result=new IIOMetadataNode("#text");
				result.setNodeValue(d.getNodeValue());
				return result;
			}
			else
				return null;	
		} else {
			result=new IIOMetadataNode(d.getNodeName());

			// Attributes:
			attl=d.getAttributes();
			n=attl.getLength();
			for(i=0;i<n;i++) { 
				att=attl.item(i);
				result.setAttribute(att.getNodeName(),att.getNodeValue());
			}

			l=d.getChildNodes();
			n=l.getLength();
			for(i=0;i<n;i++) {
				Node tmp=cleanup(l.item(i));
				if(tmp !=null)
					result.appendChild(tmp);
			}
		}		
		return result;
	}
	
	// TODO use XMLWriter
	/*
	 * Generate an XML string from a DOM node
	 */
	public static String GenerateXML(Node d) {
		int i,n;
		Node att;
		NodeList l;
		NamedNodeMap attl;
		String result="";
		
		if (d.getNodeName().equals("#text")) {
			result = d.getNodeValue() + "\n";
		} else {
			result=result + "<" + d.getNodeName();
			// Attributes:
			attl=d.getAttributes();
			n=attl.getLength();
			if (n>0) result=result + " ";
			for(i=0;i<n;i++) { 
				att=attl.item(i);
				result = result + att.getNodeName() + "=\"" + att.getNodeValue() + "\" ";
			}
			result=result + ">" + "\n";
	
			l=d.getChildNodes();
			n=l.getLength();
			for(i=0;i<n;i++) {
				String tmp=GenerateXML(l.item(i),1);
				result = result + tmp;
			}
			
			result=result + "</" + d.getNodeName() + ">" + "\n";
		}
		
		return result;
	}
	/*
	 * Generate a string from XML node with appropriate tab spaces 
	 */
	public static String GenerateXML(Node d,int tabs) {
		int i,n;
		Node att;
		NodeList l;
		NamedNodeMap attl;
		String result="";

		for(i=0;i<tabs;i++) result=result+"\t";
		
		if (d.getNodeName().equals("#text")) {
			result = result + d.getNodeValue() + "\n";
		} else {
			result=result + "<" + d.getNodeName() + " ";
			// Attributes:
			attl=d.getAttributes();
			n=attl.getLength();
			for(i=0;i<n;i++) { 
				att=attl.item(i);
				result = result + att.getNodeName() + "=\"" + att.getNodeValue() + "\" ";
			}
			result=result + ">" + "\n";
	
			l=d.getChildNodes();
			n=l.getLength();
			for(i=0;i<n;i++) {
				String tmp=GenerateXML(l.item(i),tabs+1);
				result = result + tmp;
			}
			
			for(i=0;i<tabs;i++) result=result+"\t";
			result=result + "</" + d.getNodeName() + ">" + "\n";
		}
		
		return result;
	}
	
	/*
	 * Helper function to clone the node 
	 */
	public static Node cloneXML(Node d) {
		int i,n;
		Node att;
		NodeList l;
		NamedNodeMap attl;
		IIOMetadataNode result=null;
		
		if (d.getNodeName().equals("#text")) {
			result=new IIOMetadataNode("#text");
			result.setNodeValue(d.getNodeValue());
			return result;
		} else {
			result=new IIOMetadataNode(d.getNodeName());

			// Attributes:
			attl=d.getAttributes();
			n=attl.getLength();
			for(i=0;i<n;i++) { 
				att=attl.item(i);
				result.setAttribute(att.getNodeName(),att.getNodeValue());
			}

			l=d.getChildNodes();
			n=l.getLength();
			for(i=0;i<n;i++) {
				Node tmp=cloneXML(l.item(i));
				result.appendChild(tmp);
			}
		}		
		return result;
	}
	/*
	 *  Extract the value for a particular feature from a give Node
	 */
	public static Node XMLfeatureValue(Node n,String feature) {
		NodeList l=n.getChildNodes();
		int i,length;
		
		length=l.getLength();
		for(i=0;i<length;i++) {
			String tmp = l.item(i).getNodeName();
			if (tmp.equals(feature)) return l.item(i);
		}
		
		return null;
	}

	
}
