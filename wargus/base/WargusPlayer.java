/*********************************************************************************
Organization 					: 				Georgia Institute of Technology
  
Institute 						:				Cognitive Computing Group(CCL)
 
Authors							: 				Santiago Ontanon, Kinshuk Mishra
 												Neha Sugandh 
 												
Class							:				Player
 
Function						: 				Holding the Information corresponding to 
												the Player. Used in the Case base for CBR
****************************************************************************/
package base;

import java.awt.Point;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.jdom.Element;

public class WargusPlayer implements Serializable {
	private static final long serialVersionUID = 1L;
	private int m_ID;
	private int m_ResourceGold;
	private int m_ResourceWood;
	private int m_ResourceOil;
	private int m_food;
	private int m_demand;
	private boolean [] m_research;
	
	// This List contains the set of units that the solution of the case makes reference to,
	// but that are not in the game state. It's used by the ADAPTATION module
	// For example, here are units that are created by the solution of the case, but that were not
	// present in the game state.
	private Vector<WargusUnit> m_Units;
	
	
	public WargusPlayer(int ID, int a_ResourceGold, int a_ResourceWood, 
			int a_ResourceOil, Vector<WargusUnit> a_Units, boolean research[]) {
		m_ID = ID;
		m_ResourceGold = a_ResourceGold;
		m_ResourceWood = a_ResourceWood;
		m_ResourceOil = a_ResourceOil;
		m_Units = new Vector<WargusUnit>();
		for(int i=0;i<a_Units.size();i++)
			m_Units.addElement(a_Units.get(i));
		m_food = 0;
		m_demand = 0;
		m_research = new boolean[53];
		{
			if (research != null) {
				for(int i=0; i < m_research.length; i++) m_research[i] = research[i];
			} else {
				Arrays.fill(m_research, false);
			}			
		}			
	}
	
	public WargusPlayer(WargusPlayer a_Player){
		m_ID = a_Player.m_ID;
		this.m_ResourceGold = a_Player.m_ResourceGold;
		this.m_ResourceWood = a_Player.m_ResourceWood;
		this.m_ResourceOil = a_Player.m_ResourceOil;
		m_Units = new Vector<WargusUnit>();
		for(int i=0; i<a_Player.m_Units.size() ;i++)
		{
			this.m_Units.addElement(a_Player.m_Units.get(i));
		}
		
		m_research = new boolean[53];
		boolean[] otherPlayersResearch = a_Player.getResearch(); 
		for(int i = 0; i < m_research.length; ++i)	{
			m_research[i] = otherPlayersResearch[i];
		}
		m_food = a_Player.m_food;
		m_demand = a_Player.m_demand;
	}
	
	public int getGold() { return m_ResourceGold; }
	public int getWood() { return m_ResourceWood; }
	public int getOil() { return m_ResourceOil; }
	
	public Vector<WargusUnit> getUnits() {return m_Units; }
	
	public List<WargusUnit> getUnitsByType(String typeStr) {
		return getUnitsByType(WargusStateImporter.unitTypeToInteger(typeStr));
	}
	public List<WargusUnit> getUnitsByType(int type) {
		List<WargusUnit> units = new Vector<WargusUnit>();
		for (WargusUnit u : m_Units) {
			if (u.getType() == type) units.add(u);
		}
		return units;
	}
	public List<WargusUnit> getUnitsByGroup(WargusUnit.IsUnit isUnit) {
		List<WargusUnit.IsUnit> unitTesters = new Vector<WargusUnit.IsUnit>(1);
		unitTesters.add(isUnit);
		return getUnitsByGroup(unitTesters);
	}
	// XXX need to specify if using OR or AND... (currently this uses OR)
	public List<WargusUnit> getUnitsByGroup(Collection<WargusUnit.IsUnit> unitTesters) {
		Set<WargusUnit> units = new HashSet<WargusUnit>();
		int type;
		for (WargusUnit u : m_Units) {
			type = u.getType();
			for (WargusUnit.IsUnit isUnit : unitTesters) {
				if (isUnit.test(type)) { 
					units.add(u);
					break;
				}
			}
		}
		List<WargusUnit> lunits = new Vector<WargusUnit>();
		lunits.addAll(units);
		return lunits;
	}	
	public List<WargusUnit> getFlyingUnits() {
		return getUnitsByGroup(WargusUnit.isFlyingUnit());
	}
	public List<WargusUnit> getLandUnits() {
		return getUnitsByGroup(WargusUnit.isLandUnit());
	}
	public List<WargusUnit> getWaterUnits() {
		return getUnitsByGroup(WargusUnit.isWaterUnit());
	}
	public List<WargusUnit> getMobileUnits() {
		return getUnitsByGroup(WargusUnit.isMobileUnit());
	}
	public List<WargusUnit> getBuildings() {
		return getUnitsByGroup(WargusUnit.isBuildingUnit());
	}
	public List<WargusUnit> getBombardingUnits() {
		return getUnitsByGroup(WargusUnit.isBombardingUnit());
	}
	
	public boolean hasResearch(String research) {
		return m_research[WargusStateImporter.researchStringToType(research)];
	}
	public boolean hasResearch(int type) {
		return m_research[type];
	}
	public boolean[] getResearch() {
		return m_research;
	}
	public int getFood() { return m_food; }
	public int getDemand() { return m_demand; }
	
	public void setFood(int food) { m_food = food; }
	public void setDemand(int demand) { m_demand = demand; }

	public void addCompletedReseach(String a_research) {
		m_research[WargusStateImporter.researchStringToType(a_research)] = true;
	}
	public void addCompletedResearch(int type) {
		m_research[type] = true;
	}

	
	/**
	 * Calculate the players score based on the value of all players units, 
	 * resources, upgrades, research, and the number of sectors they have 
	 * influence over.  Military units are valued slightly more than others.
	 * 
	 * May want to devalue research & upgrades b/c they might not always be
	 * in use or useful...
	 * 
	 * @return the score
	 */
	public int calculateScore() {
		int score = 0;
		
		int resources = m_ResourceGold + m_ResourceWood + m_ResourceOil;
		// modify to value unspent resources more than civilian but less than military?
		
		int military = 0, civilian = 0, sectors = 0;
		{
			int type, lx, ly;
			float cost;
			String typeStr;
			Collection<Point> points = new HashSet<Point>();
			for (WargusUnit u : m_Units) {
				type = u.getType();
				if (type == 92 || type == 93) continue; // don't count oil or gold ...
				
				typeStr = WargusStateImporter.unitTypeToString(type);
				
				cost = ((float) u.getHitPoints() / (float) u.getMaxHP()) *
					(WargusStateImporter.unitCostGold(typeStr) +
					 WargusStateImporter.unitCostOil(typeStr) +
					 WargusStateImporter.unitCostWood(typeStr));
				if (Arrays.binarySearch(WargusUnit.CivilianUnits, type) >= 0) {
					civilian += cost;
				} else {
					military += cost;
				}
				
				if (type == 2 || type == 3) continue; // peasants & peons dont affect sectors
				lx = u.getLocX();
				ly = u.getLocY();
				// each unit influences points in a square with width = 7
				for (int i = lx - 3; i <= lx + 3; ++i) {
					for (int j = ly - 3; j <= ly + 3; ++j) {
						points.add(new Point(i,j));
					}
				}
				//System.out.println("size so far: " + points.size());
			}
			military = (int) (((float) military) * 1.5);
			sectors = points.size() * 100; // .. guessing, this will need to be adjusted
		}
		
		int research = 0;
		String tech;
		for (int i = 0; i < m_research.length; ++i) {
			if (m_research[i]) {
				tech = WargusStateImporter.researchTypeToString(i);
				research += (WargusStateImporter.unitCostGold(tech) +
						WargusStateImporter.unitCostOil(tech) +
						WargusStateImporter.unitCostWood(tech));
			}
		}
		score = resources + military + civilian + sectors + research;
		
		try {
			PrintWriter outer = new PrintWriter(new FileWriter("scores.out", true));
			outer.printf("Scores(resources=%d, military=%d, civilian=%d," +
					" sectors=%d, research=%d) Total=%d\n",
					resources, military, civilian, sectors, research, score);
			outer.close();
		} catch (Exception e) {}

		return score;
	}
	
	public static WargusPlayer loadFromXML(Element e) {
		assert e.getName().equals("player") : 
			"WargusPlayer.loadFromXML: Invalid XML Element " + e.getName();

		int id = Integer.parseInt(e.getAttributeValue("id"));
		int gold = Integer.parseInt(e.getChildText("resource-gold"));
		int wood = Integer.parseInt(e.getChildText("resource-wood"));
		int oil = Integer.parseInt(e.getChildText("resource-oil"));
		int food = Integer.parseInt(e.getChildText("food"));
		int demand = Integer.parseInt(e.getChildText("demand"));
		
		boolean[] researched = new boolean[53];
		Arrays.fill(researched, false);		
		String researchList = e.getChildText("research").trim();
		if (!researchList.equals("")) {
			String[] research = researchList.split(",");
			for (String tech : research) {
				researched[WargusStateImporter.researchStringToType(tech)] = true;
			}
		}
		
		Vector<WargusUnit> units = new Vector<WargusUnit>();
		for (Object o : e.getChild("units").getChildren("unit")) {
			Element unitElement = (Element) o;
			units.add(WargusUnit.loadFromXML(unitElement));
		}

		WargusPlayer p = new WargusPlayer(id, gold, wood, oil, units, researched);
		p.setFood(food);
		p.setDemand(demand);
		return p;
	}
	
/*
	public void writeXML(XMLWriter w) {
		w.tag("player id=\"" + m_ID + "\"");		
		{
			w.tag("resource-gold", m_ResourceGold);
			w.tag("resource-wood", m_ResourceWood);
			w.tag("resource-oil", m_ResourceOil);
			w.tag("food", m_food);
			w.tag("demand", m_demand);
			
			w.tag("research");
			{
				StringBuffer sb = new StringBuffer("");
				for (int i = 0; i < m_research.length; ++i) {
					if (m_research[i]) {
						sb.append(WargusStateImporter.researchTypeToString(i) + ",");
					}
				}
				// remove the last ','
				if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1); 
				w.rawXML(sb.toString());
			}
			w.tag("/research");
			
			w.tag("units");			
			{
				for(WargusUnit u:m_Units) u.writeXML(w);
			}
			w.tag("/units");
		}
		w.tag("/player");
	}	
	
	public int getID() {
		return m_ID;
	}
*/
};
