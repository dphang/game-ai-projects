/*********************************************************************************
Organization 					: 				Georgia Institute of Technology
  
Institute 						:				Cognitive Computing Group(CCL)
 
Authors							: 				Santiago Ontanon, Kinshuk Mishra
 												Neha Sugandh,
 												
Class							:				GameStateImporter
 
Function						: 				The class provides helper function 
												to parse the Game State XML nodes
												and get specific values for different 
												attributes
*********************************************************************************/
package base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/*
import planninglanguage.plans.AttackBOPlan;
import planninglanguage.plans.AttackGroundBOPlan;
import planninglanguage.plans.BasicOperatorPlan;
import planninglanguage.plans.BuildBOPlan;
import planninglanguage.plans.MoveBOPlan;
import planninglanguage.plans.RepairBOPlan;
import planninglanguage.plans.ResearchBOPlan;
import planninglanguage.plans.ResourceBOPlan;
import planninglanguage.plans.ResourceLocationBOPlan;
import planninglanguage.plans.StandGroundBOPlan;
import planninglanguage.plans.StopBOPlan;
import planninglanguage.plans.TrainBOPlan;
import planninglanguage.plans.UpgradeBOPlan;
*/

public class WargusStateImporter {
	
	public final static char TERRAIN_NONE = ' ';
	public final static char TERRAIN_GRASS = '.';
	public final static char TERRAIN_GRASS_NOBUILD = ',';
	public final static char TERRAIN_WALL = 'X';
	public final static char TERRAIN_TREE = 't';
	public final static char TERRAIN_WATER = 'w';
	public final static char TERRAIN_GOLD = 'G';
	public final static char TERRAIN_OIL = 'O';
	public final static char TERRAIN_BUILDING = 'B';
	public final static char TERRAIN_BUILDINGOPP = 'b';
	public final static char TERRAIN_UNIT = 'U';
	public final static char TERRAIN_UNITOPP = 'u';
	public final static char TERRAIN_EDGE = 'e';
	
	
	/*
	 * Get the game state from the proxy bot and convert it into a DOM node
	 */
	/*
	static public WargusGameState getGameState(ProxyBot pb) {
		WargusGameState description = null;
		WargusMap map = getGameStateMap(pb);
		List<WargusPlayer> opponents = getGameStateOpponents(pb);
		WargusPlayer player = getGameStatePlayer(pb);
		
		if (map==null || opponents==null || player==null) return null;
		
		description = new WargusGameState(map,opponents,player);
		
//		System.out.println("After we get it from base:\n" + description.getMap().toStringTerrain());
		
		return description;
	}
	*/
		
	/*
	 * Get the game state map from the proxy bot 
	 */
	static public WargusMap getGameStateMap(ProxyBot pb)  {
        WargusMap map = null;
        int i,j;
        ArrayList<WargusUnit> l;

        if (!pb.connected()) return null;

        l=pb.getFriendOrNeutralUnits();
        map = new WargusMap(pb.GetMapWidth(),pb.GetMapLength());

        {
                for(i=0;i<pb.GetMapLength();i++) {
                        for(j=0;j<pb.GetMapWidth();j++) {
                                char c=pb.GetMapCell(i,j);
                                char d=TERRAIN_GRASS;

//                                System.out.print(c);

                                if (c=='+') d=TERRAIN_GRASS_NOBUILD;
                                if (c=='W') d='X';
                                if (c=='#') d='X';
                                if (c=='T') d='t';
                                if (c=='B') d='B';
                                map.set_map(j, i, d);
                        }
//                        System.out.println("");
                }
        }

        for(WargusUnit u:l) {
                if (u.getType()==92) {
                        map.add_goldMine(u);
                }
                if (u.getType()==93) {
                        map.add_oilPatch(u);
                }
        }

        map.insertMines();

        return map;
	} 
	
	/*
	 * Get the Node for the players from the proxy bot
	 */
	static public List<WargusPlayer> getGameStateOpponents(ProxyBot pb) {
		
		List<WargusPlayer> players = new LinkedList<WargusPlayer>();
		ArrayList<WargusUnit> l;
		
		if (!pb.connected()) return null;		

		l=pb.getEnemyUnits();
		
		// Filter dead-bodies:
		{
			LinkedList<WargusUnit> todelete = new LinkedList<WargusUnit>();
			
			for(WargusUnit u:l) {
				if (u.getType()== 105) todelete.add(u);
				if (u.getType()== 106) todelete.add(u);
				if (u.getType()== 107) todelete.add(u);
				if (u.getType()== 108) todelete.add(u);
				if (u.getType()== 109) todelete.add(u);
			}
			
			while(todelete.size()>0) {
				l.remove(todelete.removeFirst());
			}
		}		
		
		if (l.size()>0) {
			Vector<WargusUnit> units = new Vector<WargusUnit>();
			
			for(WargusUnit u:l) {
				if (u!=null) units.add(u);
			}			
			WargusPlayer player= new WargusPlayer(1, 0, 0, 0, units, null);
			players.add(player);
		}				
		return players;
	}	
	
	
	/*
	 * Get the Node for the players from the proxy bot
	 */
	static public WargusPlayer getGameStatePlayer(ProxyBot pb) {
		
		WargusPlayer player = null;
		ArrayList<WargusUnit> l;
		
		if (!pb.connected()) return null;
						
		l=pb.getMyUnits();
		
		// Filter dead-bodies:
		{
			LinkedList<WargusUnit> todelete = new LinkedList<WargusUnit>();
			
			for(WargusUnit u:l) {
				if (u.getType()== 105) todelete.add(u);
				if (u.getType()== 106) todelete.add(u);
				if (u.getType()== 107) todelete.add(u);
				if (u.getType()== 108) todelete.add(u);
				if (u.getType()== 109) todelete.add(u);
			}
			
			while(todelete.size()>0) {
				l.remove(todelete.removeFirst());
			}
		}
		
		{
			Vector<WargusUnit> units = new Vector<WargusUnit>();
			int food = 0;
			int demand = 0;

			for(WargusUnit u:l) {
				if (u!=null) units.add(u);
				if (u.getType()==74 && !statusToString(u.getStatus()[0]).equals("being-built")) food++;
				if (u.getType()==75 && !statusToString(u.getStatus()[0]).equals("being-built")) food++;
				if (u.getType()==88 && !statusToString(u.getStatus()[0]).equals("being-built")) food++;
				if (u.getType()==89 && !statusToString(u.getStatus()[0]).equals("being-built")) food++;
				if (u.getType()==90 && !statusToString(u.getStatus()[0]).equals("being-built")) food++;
				if (u.getType()==91 && !statusToString(u.getStatus()[0]).equals("being-built")) food++;
				if (u.getType()==58 && !statusToString(u.getStatus()[0]).equals("being-built")) food+=4;
				if (u.getType()==59 && !statusToString(u.getStatus()[0]).equals("being-built")) food+=4;
				
				if (unitSize(unitTypeToString(u.getType()))==1) demand++;				
			}			
			player = new WargusPlayer(0,(int)pb.getGold(),(int)pb.getWood(),(int)pb.getOil(),units,pb.getAdvances());
			player.setFood(food);
			player.setDemand(demand);
		}	

		return player;
	}	
	
	// TODO check that all water & advanced units are correctly named and have correct settings
	//      b/c i found a couple incorrect  --Andrew
	// 	 i fixed submarines & transports & destroyers
	// TODO specifically eye-of-kilrogg is not supported... do you build or summon it?
	static HashMap<String,Integer> m_maxHitPoints = null;
	static HashMap<String,Integer> m_unitSize = null;
	static HashMap<String,Integer> m_costGold= null;
	static HashMap<String,Integer> m_costWood= null;
	static HashMap<String,Integer> m_costOil= null;
	
	// TODO refactor to HashMap
	static public int statusToInteger(String status) {
		if (status.equals("idle")) return 0;
		else if (status.equals("move")) return 2;
		else if (status.equals("working")) return 3;
		else if (status.equals("attack")) return 4;
		else if (status.equals("repair")) return 5;
		else if (status.equals("resource")) return 6;
		else if (status.equals("returning-goods")) return 7;
		else if (status.equals("patrol")) return 9;
		else if (status.equals("stand-ground")) return 10;
		else if (status.equals("follow")) return 11;
		else if (status.equals("boarding")) return 12;
		else if (status.equals("unloading")) return 13;
		else if (status.equals("upgrading")) return 14;
		else if (status.equals("research")) return 15;
		else if (status.equals("spell-casting")) return 16;
		else if (status.equals("being-built")) return 17;
		
		throw new Error("WargusStateImporter.statusToInteger: unknown status"
				+ status);
	}

	static public String statusToString(int status) {
		switch(status) {
		case 0: return "idle";
		case 2: return "move";
		case 3: return "working";
		case 4:
		case 8: return "attack";
		case 5: return "repair";
		case 6: return "resource";
		case 7: return "returning-goods";
		case 9: return "patrol";
		case 10:return "stand-ground";
		case 11:return "follow";
		case 12:return "boarding";
		case 13:return "unloading";
		case 14:return "upgrading";
		case 15:return "research";
		case 16:return "spell-casting";
		case 17:return "being-built";
		default:
			throw new Error(
					"WargusStateImporter.statusToString: Unrecognized status! "
					+ status);
		}
		
	}
	
	static public int unitMaxHitPoints(int unitType) {
		return unitMaxHitPoints(unitTypeToString(unitType));
	}
	
	/*
	 * Helper function to get Maximum Hit Points for a given Unit Name
	 */
	static public int unitMaxHitPoints(String unitName) {
		Integer retVal;
		if (m_maxHitPoints == null) {
			m_maxHitPoints=new HashMap<String,Integer>();
			
			m_maxHitPoints.put("town-hall",1200);
			m_maxHitPoints.put("great-hall",1200);
			m_maxHitPoints.put("keep",1400);
			m_maxHitPoints.put("stronghold",1400);
			m_maxHitPoints.put("castle",1400);
			m_maxHitPoints.put("fortress",1400);
			m_maxHitPoints.put("human-barracks",800);
			m_maxHitPoints.put("orc-barracks",800);
			m_maxHitPoints.put("human-shipyard",1100);
			m_maxHitPoints.put("orc-shipyard",1100);
			m_maxHitPoints.put("gryphon-aviary",500);
			m_maxHitPoints.put("mage-tower",500);
			m_maxHitPoints.put("gnomish-inventor",500);
			m_maxHitPoints.put("inventor",500);
			m_maxHitPoints.put("goblin-alchemist",500);
			m_maxHitPoints.put("human-scout-tower",100);
			m_maxHitPoints.put("human-guard-tower",130);
			m_maxHitPoints.put("human-cannon-tower",160);
			m_maxHitPoints.put("orc-scout-tower",100);
			m_maxHitPoints.put("orc-guard-tower",130);
			m_maxHitPoints.put("orc-cannon-tower",160);
			m_maxHitPoints.put("elven-lumber-mill",600);
			m_maxHitPoints.put("troll-lumber-mill",600);
			m_maxHitPoints.put("human-blacksmith",775);
			m_maxHitPoints.put("orc-blacksmith",775);
			m_maxHitPoints.put("stables",500);
			m_maxHitPoints.put("ogre-mound",500);
			m_maxHitPoints.put("church",700);
			m_maxHitPoints.put("dragon-roost",500);
			m_maxHitPoints.put("temple-of-the-damned",500);
			m_maxHitPoints.put("altar-of-storms",700);
			m_maxHitPoints.put("farm",400);
			m_maxHitPoints.put("pig-farm",400);
			m_maxHitPoints.put("human-oil-rig",650);
			m_maxHitPoints.put("orc-oil-rig",650);
			m_maxHitPoints.put("human-foundry",750);
			m_maxHitPoints.put("orc-foundry",750);
			m_maxHitPoints.put("human-oil-refinery",600);
			m_maxHitPoints.put("orc-oil-refinery",600);

			m_maxHitPoints.put("sheep",5);
			m_maxHitPoints.put("peasant",30);
			m_maxHitPoints.put("peon",30);
			m_maxHitPoints.put("footman",60);
			m_maxHitPoints.put("grunt",60);
			m_maxHitPoints.put("skeleton",40);
			m_maxHitPoints.put("archer",40);
			m_maxHitPoints.put("axethrower",40);
			m_maxHitPoints.put("ballista",110);
			m_maxHitPoints.put("catapult",110);
			m_maxHitPoints.put("dwarven-demolition-squad",40);
			m_maxHitPoints.put("goblin-sappers",40);
			m_maxHitPoints.put("knight",90);
			m_maxHitPoints.put("ogre",90);
			m_maxHitPoints.put("paladin",90);
			m_maxHitPoints.put("mage",60);
			m_maxHitPoints.put("gnomish-flying-machine",150);
			m_maxHitPoints.put("goblin-zeppelin",150);
			m_maxHitPoints.put("gryphon-rider",100);
			m_maxHitPoints.put("dragon",100);
			m_maxHitPoints.put("human-oil-tanker",90);
			m_maxHitPoints.put("orc-oil-tanker",90);
			m_maxHitPoints.put("elven-destroyer",100);
			m_maxHitPoints.put("troll-destroyer",100);
			m_maxHitPoints.put("human-transport",150);
			m_maxHitPoints.put("orc-transport",150);
			m_maxHitPoints.put("gnomish-submarine",60);
			m_maxHitPoints.put("giant-turtle",60);
			m_maxHitPoints.put("battleship",150);
			m_maxHitPoints.put("ogre-juggernaught",150);
		}
		
		retVal=m_maxHitPoints.get(unitName);
		if (retVal!=null) return retVal;
		return 0;
	}
	
	/*
	 * Helper Function to get the size of a Unit given its name
	 */
	static public int unitSize(int unitType) {
		return unitSize(unitTypeToString(unitType));
	}
	
	static public int unitSize(String unitName) {
		Integer retVal;
		if (m_unitSize == null) {
			m_unitSize=new HashMap<String,Integer>();
			
			m_unitSize.put("town-hall",4);
			m_unitSize.put("great-hall",4);
			m_unitSize.put("keep",4);
			m_unitSize.put("stronghold",4);
			m_unitSize.put("castle",4);
			m_unitSize.put("fortress",4);
			m_unitSize.put("human-barracks",3);
			m_unitSize.put("orc-barracks",3);
			m_unitSize.put("human-shipyard",3);
			m_unitSize.put("orc-shipyard",3);
			m_unitSize.put("gryphon-aviary",3);
			m_unitSize.put("mage-tower",3);
			m_unitSize.put("gnomish-inventor",3);
			m_unitSize.put("inventor",3);
			m_unitSize.put("goblin-alchemist",3);
			m_unitSize.put("human-scout-tower",2);
			m_unitSize.put("human-guard-tower",2);
			m_unitSize.put("human-cannon-tower",2);
			m_unitSize.put("orc-scout-tower",2);
			m_unitSize.put("orc-guard-tower",2);
			m_unitSize.put("orc-cannon-tower",2);
			m_unitSize.put("elven-lumber-mill",3);
			m_unitSize.put("troll-lumber-mill",3);
			m_unitSize.put("human-blacksmith",3);
			m_unitSize.put("orc-blacksmith",3);
			m_unitSize.put("stables",3);
			m_unitSize.put("ogre-mound",3);
			m_unitSize.put("church",3);
			m_unitSize.put("dragon-roost",3);
			m_unitSize.put("temple-of-the-damned",3);
			m_unitSize.put("altar-of-storms",3);
			m_unitSize.put("farm",2);
			m_unitSize.put("pig-farm",2);
			m_unitSize.put("human-oil-rig",3);
			m_unitSize.put("orc-oil-rig",3);
			m_unitSize.put("human-foundry",3);
			m_unitSize.put("orc-foundry",3);
			m_unitSize.put("human-oil-refinery",3);
			m_unitSize.put("orc-oil-refinery",3);

			m_unitSize.put("sheep",1);
			m_unitSize.put("peasant",1);
			m_unitSize.put("peon",1);
			m_unitSize.put("footman",1);
			m_unitSize.put("grunt",1);
			m_unitSize.put("skeleton",1);
			m_unitSize.put("archer",1);
			m_unitSize.put("axethrower",1);
			m_unitSize.put("ballista",1);
			m_unitSize.put("catapult",1);
			m_unitSize.put("dwarven-demolition-squad",1);
			m_unitSize.put("goblin-sappers",1);
			m_unitSize.put("knight",1);
			m_unitSize.put("ogre",1);
			m_unitSize.put("paladin",1);
			m_unitSize.put("mage",1);
			m_unitSize.put("gnomish-flying-machine",1);
			m_unitSize.put("goblin-zeppelin",1);
			m_unitSize.put("gryphon-rider",1);
			m_unitSize.put("dragon",1);
			m_unitSize.put("human-oil-tanker",1);
			m_unitSize.put("orc-oil-tanker",1);
			m_unitSize.put("elven-destroyer",1);
			m_unitSize.put("troll-destroyer",1);
			m_unitSize.put("human-transport",1);
			m_unitSize.put("orc-transport",1);
			m_unitSize.put("gnomish-submarine",1);
			m_unitSize.put("giant-turtle",1);
			m_unitSize.put("battleship",1);
			m_unitSize.put("ogre-juggernaught",1);
		}
		
		retVal=m_unitSize.get(unitName);
		if (retVal!=null) return retVal;
		return 0; // XXX does this make sense or should we throw an error?
	}
		
	/*
	 * Helper Function to get the cost of Gold to build a particular 
	 * Unit 
	 */
	static public int unitCostGold(String unitName) {
		Integer retVal;
		if (m_costGold == null) {
			m_costGold = new HashMap<String,Integer>();

			m_costGold.put("town-hall",1200);
			m_costGold.put("great-hall",1200);
			
			m_costGold.put("keep",2000);
			m_costGold.put("stronghold",2000);
			m_costGold.put("castle",2500);
			m_costGold.put("fortress",2500);
			m_costGold.put("farm",500);
			m_costGold.put("pig-farm",500);
			m_costGold.put("human-barracks",700);
			m_costGold.put("orc-barracks",700);
			m_costGold.put("elven-lumber-mill",600);
			m_costGold.put("troll-lumber-mill",600);
			m_costGold.put("human-blacksmith",800);
			m_costGold.put("orc-blacksmith",800);
			m_costGold.put("human-scout-tower",550);
			m_costGold.put("orc-scout-tower",550);
			m_costGold.put("human-guard-tower",550);
			m_costGold.put("orc-guard-tower",550);
			m_costGold.put("human-cannon-tower",550);
			m_costGold.put("orc-cannon-tower",550);
			m_costGold.put("human-shipyard",800);
			m_costGold.put("orc-shipyard",800);
			m_costGold.put("human-oil-rig",700);
			m_costGold.put("orc-oil-rig",700);
			m_costGold.put("human-foundry",700);
			m_costGold.put("orc-foundry",700);
			m_costGold.put("human-oil-refinery",800);
			m_costGold.put("orc-refinery",800);
			m_costGold.put("orc-oil-refinery",800);
			m_costGold.put("stables",1000);
			m_costGold.put("ogre-mound",1000);
			m_costGold.put("gnomish-inventor",1000);
			m_costGold.put("inventor",1000);
			m_costGold.put("goblin-alchemist",1000);
			m_costGold.put("gryphon-aviary",1000);
			m_costGold.put("dragon-roost",1000);
			m_costGold.put("mage-tower",1000);
			m_costGold.put("temple-of-the-damned",1000);
			m_costGold.put("church",900);
			m_costGold.put("altar-of-storms",900);

			m_costGold.put("sheep",0);
			m_costGold.put("peasant",400);
			m_costGold.put("peon",400);
			m_costGold.put("footman",600);
			m_costGold.put("grunt",600);
			m_costGold.put("skeleton",0);
			m_costGold.put("archer",500);
			m_costGold.put("axethrower",500);
			m_costGold.put("ballista",900);
			m_costGold.put("catapult",900);
			m_costGold.put("dwarven-demolition-squad",750);
			m_costGold.put("goblin-sappers",750);
			m_costGold.put("knight",800);
			m_costGold.put("ogre",800);
			m_costGold.put("paladin",800);
			m_costGold.put("ogre-mage",800);
			m_costGold.put("mage",1200);
			m_costGold.put("death-knight",1200);
			m_costGold.put("gnomish-flying-machine",500);
			m_costGold.put("goblin-zeppelin",500);
			m_costGold.put("gryphon-rider",2500);
			m_costGold.put("dragon",2500);
			m_costGold.put("human-oil-tanker",400);
			m_costGold.put("orc-oil-tanker",400);
			m_costGold.put("troll-destroyer",700);
			m_costGold.put("elven-destroyer",700);
			m_costGold.put("human-transport",600);
			m_costGold.put("orc-transport",600);
			m_costGold.put("gnomish-submarine",800);
			m_costGold.put("giant-turtle",800);
			m_costGold.put("battleship",1000);	
			m_costGold.put("ogre-juggernaught",1000);
			
			// Research:	
			m_costGold.put("upgrade-sword1", 800);	// attack footmen
			m_costGold.put("upgrade-sword2", 2400); // attack footmen
			m_costGold.put("upgrade-battle-axe1", 500);
			m_costGold.put("upgrade-battle-axe2", 1500);
			m_costGold.put("upgrade-arrow1", 300); // attack archer
			m_costGold.put("upgrade-arrow2", 900); // attack archer
			m_costGold.put("upgrade-throwing-axe1", 300);
			m_costGold.put("upgrade-throwing-axe2", 900);
			m_costGold.put("upgrade-human-shield1", 300); // defense *
			m_costGold.put("upgrade-human-shield2", 900); // defense *
			m_costGold.put("upgrade-orc-shield1", 300);
			m_costGold.put("upgrade-orc-shield2", 900);
			m_costGold.put("upgrade-human-ship-cannon1", 700);  // attack ships
			m_costGold.put("upgrade-human-ship-cannon2", 2000); // attack ships
			m_costGold.put("upgrade-orc-ship-cannon1", 700);
			m_costGold.put("upgrade-orc-ship-cannon2", 2000);
			m_costGold.put("upgrade-human-ship-armor1", 500);
			m_costGold.put("upgrade-human-ship-armor2", 1500);
			m_costGold.put("upgrade-orc-ship-armor1", 500);
			m_costGold.put("upgrade-orc-ship-armor2", 1500);
			m_costGold.put("upgrade-catapult1", 1500);
			m_costGold.put("upgrade-catapult2", 4000);
			m_costGold.put("upgrade-ballista1", 1500);
			m_costGold.put("upgrade-ballista2", 4000);
			m_costGold.put("upgrade-ranger", 1500);
			m_costGold.put("upgrade-berserker", 1500);
			m_costGold.put("upgrade-longbow", 2000);
			m_costGold.put("upgrade-light-axes", 2000);
			m_costGold.put("upgrade-ranger-scouting", 1500);
			m_costGold.put("upgrade-berserker-scouting", 1500);
			m_costGold.put("upgrade-ranger-marksmanship", 2500);
			m_costGold.put("upgrade-berserker-regeneration", 3000);
			m_costGold.put("upgrade-paladin", 1000);
			m_costGold.put("upgrade-ogre-mage", 1000);
			m_costGold.put("upgrade-holy-vision", 0);
			m_costGold.put("upgrade-healing", 1000);
			m_costGold.put("upgrade-exorcism", 2000);
			m_costGold.put("upgrade-flame-shield", 1000);
			m_costGold.put("upgrade-fireball", 0);
			m_costGold.put("upgrade-slow", 500);
			m_costGold.put("upgrade-invisibility", 2500);
			m_costGold.put("upgrade-polymorph", 2000);
			m_costGold.put("upgrade-blizzard", 2000);
			m_costGold.put("upgrade-eye-of-kilrogg", 0);
			m_costGold.put("upgrade-bloodlust", 1000);
			m_costGold.put("upgrade-raise-dead", 1500);
			m_costGold.put("upgrade-death-coil", 0);
			m_costGold.put("upgrade-whirlwind", 1500);
			m_costGold.put("upgrade-haste", 500);
			m_costGold.put("upgrade-unholy-armor", 2500);
			m_costGold.put("upgrade-runes", 1000);
			m_costGold.put("upgrade-death-and-decay", 2000);
			m_costGold.put("upgrade-area-healing", 2000);
		}
		
		retVal=m_costGold.get(unitName);
		if (retVal == null) {
			throw new Error("unitCostGold: Unit type not recognized: '"
					+ unitName + "'!");
		}
		return retVal;
	}	
	
	
	/*
	 * Helper Function to get the cost of Wood to build a particular 
	 * Unit 
	 */
	static public int unitCostWood(String unitName) {
		Integer retVal;
		if (m_costWood == null) {
			m_costWood=new HashMap<String,Integer>();

			m_costWood.put("town-hall",800);
			m_costWood.put("great-hall",800);
			m_costWood.put("keep",1000);
			m_costWood.put("stronghold",1000);
			m_costWood.put("castle",1200);
			m_costWood.put("fortress",1200);
			m_costWood.put("farm",250);
			m_costWood.put("pig-farm",250);
			m_costWood.put("human-barracks",450);
			m_costWood.put("orc-barracks",450);
			m_costWood.put("elven-lumber-mill",450);
			m_costWood.put("troll-lumber-mill",450);
			m_costWood.put("human-blacksmith",450);
			m_costWood.put("orc-blacksmith",450);
			m_costWood.put("human-scout-tower",150);
			m_costWood.put("orc-scout-tower",150);
			m_costWood.put("human-guard-tower",150);
			m_costWood.put("orc-guard-tower",150);
			m_costWood.put("human-cannon-tower",150);
			m_costWood.put("orc-cannon-tower",150);
			m_costWood.put("human-shipyard",450);
			m_costWood.put("orc-shipyard",450);
			m_costWood.put("human-oil-rig",450);
			m_costWood.put("orc-oil-rig",450);
			m_costWood.put("human-foundry",400);
			m_costWood.put("orc-foundry",400);
			m_costWood.put("human-oil-refinery",350);
			m_costWood.put("orc-oil-refinery",350);
			m_costWood.put("stables",300);
			m_costWood.put("ogre-mound",300);
			m_costWood.put("gnomish-inventor",400);
			m_costWood.put("inventor",400);
			m_costWood.put("goblin-alchemist",400);
			m_costWood.put("gryphon-aviary",400);
			m_costWood.put("dragon-roost",400);
			m_costWood.put("mage-tower",200);
			m_costWood.put("temple-of-the-damned",200);
			m_costWood.put("church",500);
			m_costWood.put("altar-of-storms",500);
		
			m_costWood.put("sheep",0);
			m_costWood.put("peasant",0);
			m_costWood.put("peon",0);
			m_costWood.put("footman",0);
			m_costWood.put("grunt",0);
			m_costWood.put("skeleton",0);
			m_costWood.put("archer",50);
			m_costWood.put("axethrower",50);
			m_costWood.put("ballista",300);
			m_costWood.put("catapult",300);
			m_costWood.put("dwarven-demolition-squad",250);
			m_costWood.put("goblin-sappers",250);
			m_costWood.put("knight",100);
			m_costWood.put("ogre",100);
			m_costWood.put("paladin",100);
			m_costWood.put("ogre-mage",100);
			m_costWood.put("mage",0);
			m_costWood.put("death-knight",0);
			m_costWood.put("gnomish-flying-machine",100);
			m_costWood.put("goblin-zeppelin",100);
			m_costWood.put("gryphon-rider",0);
			m_costWood.put("dragon",0);
			m_costWood.put("human-oil-tanker",250);
			m_costWood.put("orc-oil-tanker",250);
			m_costWood.put("troll-destroyer",350);
			m_costWood.put("elven-destroyer",350);
			m_costWood.put("human-transport",200);
			m_costWood.put("orc-transport",200);
			m_costWood.put("gnomish-submarine",150);
			m_costWood.put("giant-turtle",150);
			m_costWood.put("battleship",500);	
			m_costWood.put("ogre-juggernaught",500);	
			
			
			// Research
			m_costWood.put("upgrade-sword1", 0);
			m_costWood.put("upgrade-sword2", 0);
			m_costWood.put("upgrade-battle-axe1", 100);
			m_costWood.put("upgrade-battle-axe2", 300);
			m_costWood.put("upgrade-arrow1", 300);
			m_costWood.put("upgrade-arrow2", 500);
			m_costWood.put("upgrade-throwing-axe1", 300);
			m_costWood.put("upgrade-throwing-axe2", 500);
			m_costWood.put("upgrade-human-shield1", 300);
			m_costWood.put("upgrade-human-shield2", 500);
			m_costWood.put("upgrade-orc-shield1", 300);
			m_costWood.put("upgrade-orc-shield2", 500);
			m_costWood.put("upgrade-human-ship-cannon1", 100);
			m_costWood.put("upgrade-human-ship-cannon2", 200);
			m_costWood.put("upgrade-orc-ship-cannon1", 100);
			m_costWood.put("upgrade-orc-ship-cannon2", 200);
			m_costWood.put("upgrade-human-ship-armor1", 500);
			m_costWood.put("upgrade-human-ship-armor2", 900);
			m_costWood.put("upgrade-orc-ship-armor1", 500);
			m_costWood.put("upgrade-orc-ship-armor2", 900);
			m_costWood.put("upgrade-catapult1", 0);
			m_costWood.put("upgrade-catapult2", 0);
			m_costWood.put("upgrade-ballista1", 0);
			m_costWood.put("upgrade-ballista2", 0);
			m_costWood.put("upgrade-ranger", 0);
			m_costWood.put("upgrade-berserker", 0);
			m_costWood.put("upgrade-longbow", 0);
			m_costWood.put("upgrade-light-axes", 0);
			m_costWood.put("upgrade-ranger-scouting", 0);
			m_costWood.put("upgrade-berserker-scouting", 0);
			m_costWood.put("upgrade-ranger-marksmanship", 0);
			m_costWood.put("upgrade-berserker-regeneration", 0);
			m_costWood.put("upgrade-paladin", 0);
			m_costWood.put("upgrade-ogre-mage", 0);
			m_costWood.put("upgrade-holy-vision", 0);
			m_costWood.put("upgrade-healing", 0);
			m_costWood.put("upgrade-exorcism", 0);
			m_costWood.put("upgrade-flame-shield", 0);
			m_costWood.put("upgrade-fireball", 0);
			m_costWood.put("upgrade-slow", 0);
			m_costWood.put("upgrade-invisibility", 0);
			m_costWood.put("upgrade-polymorph", 0);
			m_costWood.put("upgrade-blizzard", 0);
			m_costWood.put("upgrade-eye-of-kilrogg", 0);
			m_costWood.put("upgrade-bloodlust", 0);
			m_costWood.put("upgrade-raise-dead", 0);
			m_costWood.put("upgrade-death-coil", 0);
			m_costWood.put("upgrade-whirlwind", 0);
			m_costWood.put("upgrade-haste", 0);
			m_costWood.put("upgrade-unholy-armor", 0);
			m_costWood.put("upgrade-runes", 0);
			m_costWood.put("upgrade-death-and-decay", 0);
			m_costWood.put("upgrade-area-healing", 0);
		}
		
		retVal=m_costWood.get(unitName);
		if (retVal!=null) return retVal;
		throw new Error("unitCostWood: Unit type not recognized: '" + unitName + "'!");
		//return 0; // XXX does this make sense or should we throw an error?
	}	
	
	/*
	 * Helper Function to get the cost of Oil to build a particular 
	 * Unit 
	 */
	static public int unitCostOil(String unitName) {
		Integer retVal;
		if (m_costOil == null) {
			m_costOil=new HashMap<String,Integer>();


			m_costOil.put("town-hall",0);
			m_costOil.put("great-hall",0);
			m_costOil.put("keep",200);
			m_costOil.put("stronghold",200);
			m_costOil.put("castle",500);
			m_costOil.put("fortress",500);
			m_costOil.put("farm",0);
			m_costOil.put("pig-farm",0);
			m_costOil.put("human-barracks",0);
			m_costOil.put("orc-barracks",0);
			m_costOil.put("elven-lumber-mill",0);
			m_costOil.put("troll-lumber-mill",0);
			m_costOil.put("human-blacksmith",100);
			m_costOil.put("orc-blacksmith",100);
			m_costOil.put("human-scout-tower",0);
			m_costOil.put("orc-scout-tower",0);
			m_costOil.put("human-guard-tower",0);
			m_costOil.put("orc-guard-tower",0);
			m_costOil.put("human-cannon-tower",0);
			m_costOil.put("orc-cannon-tower",0);
			m_costOil.put("human-shipyard",0);
			m_costOil.put("orc-shipyard",0);
			m_costOil.put("human-oil-rig",0);
			m_costOil.put("orc-oil-rig",0);
			m_costOil.put("human-foundry",400);
			m_costOil.put("orc-foundry",400);
			m_costOil.put("human-oil-refinery",20);
			m_costOil.put("orc-oil-refinery",20);
			m_costOil.put("stables",0);
			m_costOil.put("ogre-mound",0);
			m_costOil.put("gnomish-inventor",0);
			m_costOil.put("inventor",0);
			m_costOil.put("goblin-alchemist",0);
			m_costOil.put("gryphon-aviary",0);
			m_costOil.put("dragon-roost",0);
			m_costOil.put("mage-tower",0);
			m_costOil.put("temple-of-the-damned",0);
			m_costOil.put("church",0);
			m_costOil.put("altar-of-storms",0);

			m_costOil.put("sheep",0);
			m_costOil.put("peasant",0);
			m_costOil.put("peon",0);
			m_costOil.put("footman",0);
			m_costOil.put("grunt",0);
			m_costOil.put("skeleton",0);
			m_costOil.put("archer",0);
			m_costOil.put("axethrower",0);
			m_costOil.put("ballista",0);
			m_costOil.put("catapult",0);
			m_costOil.put("dwarven-demolition-squad",0);
			m_costOil.put("goblin-sappers",0);
			m_costOil.put("knight",0);
			m_costOil.put("ogre",0);
			m_costOil.put("paladin",0);
			m_costOil.put("ogre-mage",0);
			m_costOil.put("mage",0);
			m_costOil.put("death-knight",0);
			m_costOil.put("gnomish-flying-machine",0);
			m_costOil.put("goblin-zeppelin",0);
			m_costOil.put("gryphon-rider",0);
			m_costOil.put("dragon",0);
			m_costOil.put("human-oil-tanker",0);
			m_costOil.put("orc-oil-tanker",0);
			m_costOil.put("troll-destroyer",700);
			m_costOil.put("elven-destroyer",700);
			m_costOil.put("human-transport",500);
			m_costOil.put("orc-transport",500);
			m_costOil.put("gnomish-submarine",800);
			m_costOil.put("giant-turtle",800);
			m_costOil.put("battleship",1000);	
			m_costOil.put("ogre-juggernaught",1000);	
			
			// Research
			m_costOil.put("upgrade-sword1", 0);
			m_costOil.put("upgrade-sword2", 0);
			m_costOil.put("upgrade-battle-axe1", 0);
			m_costOil.put("upgrade-battle-axe2", 0);
			m_costOil.put("upgrade-arrow1", 0);
			m_costOil.put("upgrade-arrow2", 0);
			m_costOil.put("upgrade-throwing-axe1", 0);
			m_costOil.put("upgrade-throwing-axe2", 0);
			m_costOil.put("upgrade-human-shield1", 0);
			m_costOil.put("upgrade-human-shield2", 0);
			m_costOil.put("upgrade-orc-shield1", 0);
			m_costOil.put("upgrade-orc-shield2", 0);
			m_costOil.put("upgrade-human-ship-cannon1", 1000);
			m_costOil.put("upgrade-human-ship-cannon2", 3000);
			m_costOil.put("upgrade-orc-ship-cannon1", 1000);
			m_costOil.put("upgrade-orc-ship-cannon2", 3000);
			m_costOil.put("upgrade-human-ship-armor1", 0);
			m_costOil.put("upgrade-human-ship-armor2", 0);
			m_costOil.put("upgrade-orc-ship-armor1", 0);
			m_costOil.put("upgrade-orc-ship-armor2", 0);
			m_costOil.put("upgrade-catapult1", 0);
			m_costOil.put("upgrade-catapult2", 0);
			m_costOil.put("upgrade-ballista1", 0);
			m_costOil.put("upgrade-ballista2", 0);
			m_costOil.put("upgrade-ranger", 0);
			m_costOil.put("upgrade-berserker", 0);
			m_costOil.put("upgrade-longbow", 0);
			m_costOil.put("upgrade-light-axes", 0);
			m_costOil.put("upgrade-ranger-scouting", 0);
			m_costOil.put("upgrade-berserker-scouting", 0);
			m_costOil.put("upgrade-ranger-marksmanship", 0);
			m_costOil.put("upgrade-berserker-regeneration", 0);
			m_costOil.put("upgrade-paladin", 0);
			m_costOil.put("upgrade-ogre-mage", 0);
			m_costOil.put("upgrade-holy-vision", 0);
			m_costOil.put("upgrade-healing", 0);
			m_costOil.put("upgrade-exorcism", 0);
			m_costOil.put("upgrade-flame-shield", 0);
			m_costOil.put("upgrade-fireball", 0);
			m_costOil.put("upgrade-slow", 0);
			m_costOil.put("upgrade-invisibility", 0);
			m_costOil.put("upgrade-polymorph", 0);
			m_costOil.put("upgrade-blizzard", 0);
			m_costOil.put("upgrade-eye-of-kilrogg", 0);
			m_costOil.put("upgrade-bloodlust", 0);
			m_costOil.put("upgrade-raise-dead", 0);
			m_costOil.put("upgrade-death-coil", 0);
			m_costOil.put("upgrade-whirlwind", 0);
			m_costOil.put("upgrade-haste", 0);
			m_costOil.put("upgrade-unholy-armor", 0);
			m_costOil.put("upgrade-runes", 0);
			m_costOil.put("upgrade-death-and-decay", 0);
			m_costOil.put("upgrade-area-healing", 0);
		}
		
		retVal=m_costOil.get(unitName);
		if (retVal!=null) return retVal;
		throw new Error("unitCostOil: Unit type not recognized: '" + unitName + "'!");
		//return 0; // XXX does this make sense or should we throw an error?
	}	
		
	/*
	 * Helper function to get the type in terms of int to a String
	 */
	public static String unitTypeToString(int a_type) {
		switch(a_type) {
		case 0: return "footman";
		case 1: return "grunt";
		case 2: return "peasant";
		case 3: return "peon";
		case 4: return "ballista";
		case 5: return "catapult";
		case 6: return "knight";
		case 7: return "ogre";
		case 8: return "archer";
		case 9: return "axethrower";
		case 10: return "mage";
		case 11: return "death-knight";
		case 12: return "paladin"; 
		case 13: return "ogre-mage"; 
		case 14: return "dwarven-demolition-squad"; 
		case 15: return "goblin-sappers"; 
		case 16: return "peasant";
		case 17: return "peon";
		case 18: return "ranger";
		case 19: return "berserker";		
		case 26: return "human-oil-tanker"; 
		case 27: return "orc-oil-tanker"; 
		case 28: return "human-transport"; 
		case 29: return "orc-transport"; 
		case 30: return "elven-destroyer"; 
		case 31: return "troll-destroyer"; 
		case 32: return "battleship"; 
		case 33: return "ogre-juggernaught"; 
		case 38: return "gnomish-submarine"; 
		case 39: return "giant-turtle"; 
		case 40: return "gnomish-flying-machine"; 
		case 41: return "goblin-zeppelin"; 
		case 42: return "gryphon-rider"; 
		case 43: return "dragon"; 
		case 44: return "eye-of-kilrogg"; 
		case 45: return "skeleton"; 
		case 57: return "sheep"; 
		case 58: return "farm"; 
		case 59: return "pig-farm"; 
		case 60: return "human-barracks"; 
		case 61: return "orc-barracks"; 
		case 62: return "church"; 
		case 63: return "altar-of-storms"; 
		case 64: return "human-scout-tower"; 
		case 65: return "orc-scout-tower"; 
		case 66: return "stables"; 
		case 67: return "ogre-mound"; 
		case 68: return "gnomish-inventor"; 
		case 69: return "goblin-alchemist"; 
		case 70: return "gryphon-aviary"; 
		case 71: return "dragon-roost"; 
		case 72: return "human-shipyard"; 
		case 73: return "orc-shipyard"; 
		case 74: return "town-hall"; 
		case 75: return "great-hall"; 
		case 76: return "elven-lumber-mill"; 
		case 77: return "troll-lumber-mill"; 
		case 78: return "human-foundry"; 
		case 79: return "orc-foundry"; 
		case 80: return "mage-tower"; 
		case 81: return "temple-of-the-damned"; 
		case 82: return "human-blacksmith"; 
		case 83: return "orc-blacksmith"; 
		case 84: return "human-refinery"; 
		case 85: return "orc-refinery"; 
		case 88: return "keep"; 
		case 89: return "stronghold"; 
		case 90: return "castle"; 
		case 91: return "fortress"; 
		case 92: return "goldmine"; 
		case 93: return "oil-patch"; 
		case 96: return "human-guard-tower"; 
		case 97: return "orc-guard-tower"; 
		case 98: return "human-cannon-tower"; 
		case 99: return "orc-cannon-tower"; 
		case 103: return "human-wall"; 
		case 104: return "orc-wall"; 
		case 105: return "dead-body"; 
		case 106: return "destroyed-1x1-place"; 
		case 107: return "destroyed-2x2-place"; 
		case 108: return "destroyed-3x3-place"; 
		case 109: return "destroyed-4x4-place"; 
		case 110: return "peasant"; 
		case 111: return "peon"; 
		case 112: return "peasant"; 
		case 113: return "peon"; 
		case 114: return "human-oil-tanker"; 
		case 115: return "orc-oil-tanker"; 	
		default: 
				  return null;	
		//	throw new Error(
			//		"WargusStateImporter.unitTypeToString: Unrecognized type! "
				//	+ a_type);
		}
	}
	/*
	 * Help Function to convert from int "type" to corresponding 
	 * String 
	 */
	public static String researchTypeToString(int a_type) {	
		switch(a_type) {
		case 0: return "upgrade-sword1";
		case 2: return "upgrade-battle-axe1";
		case 1: return "upgrade-sword2";
		case 3: return "upgrade-battle-axe2";
		case 4: return "upgrade-arrow1";
		case 5: return "upgrade-arrow2";
		case 6: return "upgrade-throwing-axe1";
		case 7: return "upgrade-throwing-axe2";
		case 8: return "upgrade-human-shield1";
		case 9: return "upgrade-human-shield2";
		case 10: return "upgrade-orc-shield1";
		case 11: return "upgrade-orc-shield2";
		case 12: return "upgrade-human-ship-cannon1";
		case 13: return "upgrade-human-ship-cannon2";
		case 14: return "upgrade-orc-ship-cannon1";
		case 15: return "upgrade-orc-ship-cannon2";
		case 16: return "upgrade-human-ship-armor1";
		case 17: return "upgrade-human-ship-armor2";
		case 18: return "upgrade-orc-ship-armor1";
		case 19: return "upgrade-orc-ship-armor2";
		case 20: return "upgrade-catapult1";
		case 21: return "upgrade-catapult2";
		case 22: return "upgrade-ballista1";
		case 23: return "upgrade-ballista2";
		case 24: return "upgrade-ranger";
		case 25: return "upgrade-longbow";
		case 26: return "upgrade-ranger-scouting";
		case 27: return "upgrade-ranger-marksmanship";
		case 28: return "upgrade-berserker";
		case 29: return "upgrade-light-axes";
		case 30: return "upgrade-berserker-scouting";
		case 31: return "upgrade-berserker-regeneration";
		case 32: return "upgrade-paladin";
		case 33: return "upgrade-ogre-mage";
		case 34: return "upgrade-holy-vision";
		case 35: return "upgrade-healing";
		case 36: return "upgrade-exorcism";
		case 37: return "upgrade-flame-shield";
		case 38: return "upgrade-fireball";
		case 39: return "upgrade-slow";
		case 40: return "upgrade-invisibility";
		case 41: return "upgrade-polymorph";
		case 42: return "upgrade-blizzard";
		case 43: return "upgrade-eye-of-kilrogg";
		case 44: return "upgrade-bloodlust";
		case 45: return "upgrade-raise-dead";
		case 46: return "upgrade-death-coil";
		case 47: return "upgrade-whirlwind";
		case 48: return "upgrade-haste";
		case 49: return "upgrade-unholy-armor";
		case 50: return "upgrade-runes";
		case 51: return "upgrade-death-and-decay";
		case 52: return "upgrade-area-healing";
		default: 
			throw new Error(
					"WargusStateImporter.researchTypeToString: Unit type not recognized: "
					+ a_type + "!");
		}
	}	
	/*
	 * Helper Function to convert a research type from String to int value
	 */	
	// TODO refactor to a HashMap
	public static int researchStringToType(String a_type) {	
		if (a_type.equals("upgrade-sword1")) return 0;
		else if (a_type.equals("upgrade-sword2")) return 1;
		else if (a_type.equals("upgrade-battle-axe1")) return 2;
		else if (a_type.equals("upgrade-battle-axe2")) return 3;
		else if (a_type.equals("upgrade-arrow1")) return 4;
		else if (a_type.equals("upgrade-arrow2")) return 5;
		else if (a_type.equals("upgrade-throwing-axe1")) return 6;
		else if (a_type.equals("upgrade-throwing-axe2")) return 7;
		else if (a_type.equals("upgrade-human-shield1")) return 8;
		else if (a_type.equals("upgrade-human-shield2")) return 9;
		else if (a_type.equals("upgrade-orc-shield1")) return 10;
		else if (a_type.equals("upgrade-orc-shield2")) return 11;
		else if (a_type.equals("upgrade-human-ship-cannon1")) return 12;
		else if (a_type.equals("upgrade-human-ship-cannon2")) return 13;
		else if (a_type.equals("upgrade-orc-ship-cannon1")) return 14;
		else if (a_type.equals("upgrade-orc-ship-cannon2")) return 15;
		else if (a_type.equals("upgrade-human-ship-armor1")) return 16;
		else if (a_type.equals("upgrade-human-ship-armor2")) return 17;
		else if (a_type.equals("upgrade-orc-ship-armor1")) return 18;
		else if (a_type.equals("upgrade-orc-ship-armor2")) return 19;
		else if (a_type.equals("upgrade-catapult1")) return 20;
		else if (a_type.equals("upgrade-catapult2")) return 21;
		else if (a_type.equals("upgrade-ballista1")) return 22;
		else if (a_type.equals("upgrade-ballista2")) return 23;
		else if (a_type.equals("upgrade-ranger")) return 24;
		else if (a_type.equals("upgrade-longbow")) return 25;
		else if (a_type.equals("upgrade-ranger-scouting")) return 26;
		else if (a_type.equals("upgrade-ranger-marksmanship")) return 27;
		else if (a_type.equals("upgrade-berserker")) return 28;
		else if (a_type.equals("upgrade-light-axes")) return 29;
		else if (a_type.equals("upgrade-berserker-scouting")) return 30;
		else if (a_type.equals("upgrade-berserker-regeneration")) return 31;
		else if (a_type.equals("upgrade-paladin")) return 32;
		else if (a_type.equals("upgrade-ogre-mage")) return 33;
		else if (a_type.equals("upgrade-holy-vision")) return 34;
		else if (a_type.equals("upgrade-healing")) return 35;
		else if (a_type.equals("upgrade-exorcism")) return 36;
		else if (a_type.equals("upgrade-flame-shield")) return 37;
		else if (a_type.equals("upgrade-fireball")) return 38;
		else if (a_type.equals("upgrade-slow")) return 39;
		else if (a_type.equals("upgrade-invisibility")) return 40;
		else if (a_type.equals("upgrade-polymorph")) return 41;
		else if (a_type.equals("upgrade-blizzard")) return 42;
		else if (a_type.equals("upgrade-eye-of-kilrogg")) return 43;
		else if (a_type.equals("upgrade-bloodlust")) return 44;
		else if (a_type.equals("upgrade-raise-dead")) return 45;
		else if (a_type.equals("upgrade-death-coil")) return 46;
		else if (a_type.equals("upgrade-whirlwind")) return 47;
		else if (a_type.equals("upgrade-haste")) return 48;
		else if (a_type.equals("upgrade-unholy-armor")) return 49;
		else if (a_type.equals("upgrade-runes")) return 50;
		else if (a_type.equals("upgrade-death-and-decay")) return 51;
		// non-standard upgrade, i don't think it is implemented on Wargus side
		// except that they send it over the network so we have to handle it
		else if (a_type.equals("upgrade-area-healing")) return 52; 
		
		throw new Error("WargusStateImporter.researchStringToType: " +
				"Research type not recognized: " + a_type + "!");
	}	
	
	/*
	 * Helper Function to convert a type from String to int value 
	 */
	// TODO refactor to a HashMap 
	public static int unitTypeToInteger(String a_type) {
		
		if(a_type.equals("footman"))return 0;
		else if(a_type.equals("grunt")) return 1;
		else if(a_type.equals("peasant")) return 2;
		else if(a_type.equals("peon")) return 3;
		else if(a_type.equals("ballista")) return 4;
		else if(a_type.equals("catapult")) return 5;
		else if(a_type.equals("knight")) return 6;
		else if(a_type.equals("ogre")) return 7;
		else if(a_type.equals("archer")) return 8;
		else if(a_type.equals("axethrower")) return 9;
		else if(a_type.equals("mage")) return 10;
		else if(a_type.equals("death-knight")) return 11;
		else if(a_type.equals("paladin")) return 12;
		else if(a_type.equals("ogre-mage")) return 13;
		else if(a_type.equals("dwarven-demolition-squad")) return 14;
		else if(a_type.equals("goblin-sappers")) return 15;
		else if(a_type.equals("ranger")) return 18;
		else if(a_type.equals("berserker")) return 19;
		else if(a_type.equals("human-oil-tanker")) return 26;
		else if(a_type.equals("orc-oil-tanker")) return 27;
		else if(a_type.equals("human-transport")) return 28;
		else if(a_type.equals("orc-transport")) return 29;
		else if(a_type.equals("elven-destroyer")) return 30;
		else if(a_type.equals("troll-destroyer")) return 31;
		else if(a_type.equals("battleship")) return 32;
		else if(a_type.equals("ogre-juggernaught")) return 33;
		else if(a_type.equals("gnomish-submarine")) return 38;
		else if(a_type.equals("giant-turtle")) return 39;
		else if(a_type.equals("gnomish-flying-machine")) return 40;
		else if(a_type.equals("goblin-zeppelin")) return 41;
		else if(a_type.equals("gryphon-rider")) return 42;
		else if(a_type.equals("dragon")) return 43;
		else if(a_type.equals("eye-of-kilrogg")) return 44;
		else if(a_type.equals("skeleton")) return 45;
		else if(a_type.equals("sheep")) return 57;
		else if(a_type.equals("farm")) return 58;
		else if(a_type.equals("pig-farm")) return 59;
		else if(a_type.equals("human-barracks")) return 60;
		else if(a_type.equals("orc-barracks")) return 61;
		else if(a_type.equals("church")) return 62;
		else if(a_type.equals("altar-of-storms")) return 63;
		else if(a_type.equals("human-scout-tower")) return 64;
		else if(a_type.equals("human-watch-tower")) return 64;
		else if(a_type.equals("orc-scout-tower")) return 65;
		else if(a_type.equals("stables")) return 66;
		else if(a_type.equals("ogre-mound")) return 67;
		else if(a_type.equals("gnomish-inventor")) return 68;
		else if(a_type.equals("inventor")) return 68;
		else if(a_type.equals("goblin-alchemist")) return 69;
		else if(a_type.equals("gryphon-aviary")) return 70;
		else if(a_type.equals("dragon-roost")) return 71;
		else if(a_type.equals("human-shipyard")) return 72;
		else if(a_type.equals("orc-shipyard")) return 73;
		else if(a_type.equals("town-hall")) return 74;
		else if(a_type.equals("great-hall")) return 75;
		else if(a_type.equals("elven-lumber-mill")) return 76;
		else if(a_type.equals("troll-lumber-mill")) return 77;
		else if(a_type.equals("human-foundry")) return 78;
		else if(a_type.equals("orc-foundry")) return 79;
		else if(a_type.equals("mage-tower")) return 80;
		else if(a_type.equals("temple-of-the-damned")) return 81;
		else if(a_type.equals("human-blacksmith")) return 82;
		else if(a_type.equals("orc-blacksmith")) return 83;
		else if(a_type.equals("human-refinery")) return 84;
		else if(a_type.equals("orc-refinery")) return 85;
		else if(a_type.equals("keep")) return 88;
		else if(a_type.equals("stronghold")) return 89;
		else if(a_type.equals("castle")) return 90;
		else if(a_type.equals("fortress")) return 91;
		else if(a_type.equals("goldmine")) return 92;
		else if(a_type.equals("gold-mine")) return 92;
		else if(a_type.equals("oil-patch")) return 93;
		else if(a_type.equals("human-guard-tower")) return 96;
		else if(a_type.equals("orc-guard-tower"))return 97;
		else if(a_type.equals("human-cannon-tower"))return 98;
		else if(a_type.equals("orc-cannon-tower"))return 99;
		else if(a_type.equals("human-wall"))return 103;
		else if(a_type.equals("orc-wall"))return 104;
		else if(a_type.equals("dead-body"))return 105;
		else if(a_type.equals("destroyed-1x1-place"))return 106;
		else if(a_type.equals("destroyed-2x2-place"))return 107;
		else if(a_type.equals("destroyed-3x3-place"))return 108;
		else if(a_type.equals("destroyed-4x4-place"))return 109;
		
		throw new Error("WargusStateImporter.unitTypeToInteger: " +
				"Unit type not recognized: " + a_type + "!");
	}
	
	// TODO refactor below 2 to array searches
	public static boolean canMove(String a_type) {
		if(a_type.equals("footman"))return true;
		else if(a_type.equals("grunt")) return true;
		else if(a_type.equals("peasant")) return true;
		else if(a_type.equals("peon")) return true;
		else if(a_type.equals("ballista")) return true;
		else if(a_type.equals("catapult")) return true;
		else if(a_type.equals("knight")) return true;
		else if(a_type.equals("ogre")) return true;
		else if(a_type.equals("archer")) return true;
		else if(a_type.equals("axethrower")) return true;
		else if(a_type.equals("mage")) return true;
		else if(a_type.equals("death-knight")) return true;
		else if(a_type.equals("paladin")) return true;
		else if(a_type.equals("ogre-mage")) return true;
		else if(a_type.equals("dwarven-demolition-squad")) return true;
		else if(a_type.equals("goblin-sappers")) return true;
		else if(a_type.equals("human-oil-tanker")) return true;
		else if(a_type.equals("orc-oil-tanker")) return true;
		else if(a_type.equals("human-transport")) return true;
		else if(a_type.equals("orc-transport")) return true;
		else if(a_type.equals("elven-destroyer")) return true;
		else if(a_type.equals("troll-destroyer")) return true;
		else if(a_type.equals("battleship")) return true;
		else if(a_type.equals("ogre-juggernaught")) return true;
		else if(a_type.equals("gnomish-submarine")) return true;
		else if(a_type.equals("giant-turtle")) return true;
		else if(a_type.equals("gnomish-flying-machine")) return true;
		else if(a_type.equals("goblin-zeppelin")) return true;
		else if(a_type.equals("gryphon-rider")) return true;
		else if(a_type.equals("dragon")) return true;
		else if(a_type.equals("eye-of-kilrogg")) return true;
		else if(a_type.equals("skeleton")) return true;
		else if(a_type.equals("sheep")) return true;
		return false;
	}	

	public static boolean canAttack(String a_type) {
		if(a_type.equals("footman"))return true;
		else if(a_type.equals("grunt")) return true;
		else if(a_type.equals("peasant")) return true;
		else if(a_type.equals("peon")) return true;
		else if(a_type.equals("ballista")) return true;
		else if(a_type.equals("catapult")) return true;
		else if(a_type.equals("knight")) return true;
		else if(a_type.equals("ogre")) return true;
		else if(a_type.equals("archer")) return true;
		else if(a_type.equals("axethrower")) return true;
		else if(a_type.equals("mage")) return true;
		else if(a_type.equals("death-knight")) return true;
		else if(a_type.equals("paladin")) return true;
		else if(a_type.equals("ogre-mage")) return true;
		else if(a_type.equals("dwarven-demolition-squad")) return true;
		else if(a_type.equals("goblin-sappers")) return true;
		else if(a_type.equals("elven-destroyer")) return true;
		else if(a_type.equals("troll-destroyer")) return true;
		else if(a_type.equals("battleship")) return true;
		else if(a_type.equals("ogre-juggernaught")) return true;
		else if(a_type.equals("gnomish-submarine")) return true;
		else if(a_type.equals("giant-turtle")) return true;
		else if(a_type.equals("gnomish-flying-machine")) return true;
		else if(a_type.equals("goblin-zeppelin")) return true;
		else if(a_type.equals("gryphon-rider")) return true;
		else if(a_type.equals("dragon")) return true;
		else if(a_type.equals("eye-of-kilrogg")) return true;
		else if(a_type.equals("skeleton")) return true;
		else if(a_type.equals("sheep")) return true;
		return false;
	}	
	
	public static boolean canBuild(String a_type) {
		if(a_type.equals("peasant") || a_type.equals("peon"))
			return true;
		else
			return false;
	}		
	
	// TODO refactor to array search
	public static boolean canTrain(String a_type) {
		if(a_type.equals("farm")) return true;
		else if(a_type.equals("pig-farm")) return true;
		else if(a_type.equals("human-barracks")) return true;
		else if(a_type.equals("orc-barracks")) return true;
		else if(a_type.equals("gnomish-inventor")) return true;
		else if(a_type.equals("inventor")) return true;
		else if(a_type.equals("goblin-alchemist")) return true;
		else if(a_type.equals("gryphon-aviary")) return true;
		else if(a_type.equals("dragon-roost")) return true;
		else if(a_type.equals("human-shipyard")) return true;
		else if(a_type.equals("orc-shipyard")) return true;
		else if(a_type.equals("town-hall")) return true;
		else if(a_type.equals("great-hall")) return true;
		else if(a_type.equals("mage-tower")) return true;
		else if(a_type.equals("temple-of-the-damned")) return true;
		else if(a_type.equals("keep")) return true;
		else if(a_type.equals("stronghold")) return true;
		else if(a_type.equals("castle")) return true;
		else if(a_type.equals("fortress")) return true;

		return false;
	}		

	// TODO: Make it depend on the research statu
	public static boolean canTrain(String a_type,String trainee) {
		if(a_type.equals("farm")) {
			if (trainee.equals("sheep")) return true;
			return false;
		} else if(a_type.equals("pig-farm")) {
			if (trainee.equals("sheep")) return true;
			return false;
		} else if (a_type.equals("human-barracks")) {
			if (trainee.equals("footman"))return true;
			else if(trainee.equals("ballista")) return true;
			else if(trainee.equals("knight")) return true;
			else if(trainee.equals("archer")) return true;
			else if(trainee.equals("paladin")) return true;
			return false;
		} else if(a_type.equals("orc-barracks")) {
			if(trainee.equals("grunt")) return true;
			else if(trainee.equals("catapult")) return true;
			else if(trainee.equals("ogre")) return true;
			else if(trainee.equals("axethrower")) return true;
			else if(trainee.equals("ogre-mage")) return true;			
			return false;
		} else if(a_type.equals("gnomish-inventor") ||
				  a_type.equals("inventor")) {
			if(trainee.equals("dwarven-demolition-squad")) return true;
			else if(trainee.equals("gnomish-flying-machine")) return true;
			return false;
		} else if(a_type.equals("goblin-alchemist")) {
			if(trainee.equals("goblin-sappers")) return true;
			else if(trainee.equals("goblin-zeppelin")) return true;
			return false;
		} else if(a_type.equals("gryphon-aviary")) {
			if(trainee.equals("gryphon-rider")) return true;
			return false;
		} else if(a_type.equals("dragon-roost")) {
			if(trainee.equals("dragon")) return true;
			return false;
		} else if(a_type.equals("human-shipyard")) {
			if(trainee.equals("human-oil-tanker")) return true;
			else if(trainee.equals("human-transport")) return true;
			else if(trainee.equals("elven-destroyer")) return true;
			else if(trainee.equals("battleship")) return true;
			else if(trainee.equals("gnomish-submarine")) return true;
			return false;
		} else if(a_type.equals("orc-shipyard")) {
			if(trainee.equals("orc-oil-tanker")) return true;
			else if(trainee.equals("orc-transport")) return true;
			else if(trainee.equals("troll-destroyer")) return true;
			else if(trainee.equals("giant-turtle")) return true;
			else if(trainee.equals("ogre-juggernaught")) return true;
			return false;
		} else if(a_type.equals("town-hall")) {
			if(trainee.equals("peasant")) return true;
			return false;
		} else if(a_type.equals("great-hall")) {
			if(trainee.equals("peon")) return true;
			return false;
		} else if(a_type.equals("mage-tower")) {
			if(trainee.equals("mage")) return true;
			return false;
		} else if(a_type.equals("temple-of-the-damned")) {
			if(trainee.equals("death-knight")) return true;			
			return false;
		} else if(a_type.equals("keep")) {
			if(trainee.equals("peasant")) return true;
			return false;
		} else if(a_type.equals("stronghold")) {
			if(trainee.equals("peon")) return true;
			return false;
		} else if(a_type.equals("castle")) {
			if(trainee.equals("peasant")) return true;
			return false;
		} else if(a_type.equals("fortress")) {
			if(trainee.equals("peon")) return true;
			return false;
		}

		return false;
	}		
	
	/**
	 * Determines if the given plan is valid in the given context.
	 * 
	 * Doesn't check if the player has enough resources which is good.
	 * 
	 * Otherwise is somewhat permissive in saying plans are valid...
	 */
	/*
	public static boolean validAction(WargusUnit unit, BasicOperatorPlan plan,
			WargusGameState state, boolean research_status[]) {
		
		if (unit==null) {
			System.out.println("validAction: ERROR!!!!! Null unit!!");
		}
		
		if (plan instanceof AttackBOPlan) {
			// need to check whether opposing unit is flying or water or land & if unit can
			// attack such a unit..
			return canAttack(WargusStateImporter.unitTypeToString(unit.getType()));
		} if (plan instanceof AttackGroundBOPlan) {
			// need to check whether unit can bombard..
			return canAttack(WargusStateImporter.unitTypeToString(unit.getType()));			
		} if (plan instanceof BuildBOPlan) {
			// Peasants:
			
//			System.out.println("validAction(BuildBOPlan): type: " + unit.getType() + " building: " + WargusStateImporter.unitTypeToString(((BuildBOPlan)plan).getBuildType()));
			
			if (unit.getType()==2 || unit.getType()==3) {
				BuildBOPlan p = (BuildBOPlan)plan;
				String building = WargusStateImporter.unitTypeToString(p.getBuildType());
				
				if (building.equals("town-hall")) {
					return true;
				} else if (building.equals("farm")) {
					return true;
				} else if (building.equals("human-barracks")) {
					return true;
				} else if (building.equals("elven-lumber-mill")) {
					return true;
				} else if (building.equals("human-blacksmith")) {
					return true;
				} else if (building.equals("human-scout-tower")) {
					return true;					
				} else if (building.equals("human-shipyard")) {
					int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
					WargusPlayer player = state.getPlayer(playerID);
					for(WargusUnit u:player.getUnits()) {
						if (WargusStateImporter.unitTypeToString(u.getType()).equals("human-lumber-mill")) return true;
					}
					return false;
				} else if (building.equals("human-foundry")) {
					return true;
				} else if (building.equals("human-oil-refinery")) {
					return true;
				} else if (building.equals("stables")) {
					int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
					WargusPlayer player = state.getPlayer(playerID);
					for(WargusUnit u:player.getUnits()) {
						if (WargusStateImporter.unitTypeToString(u.getType()).equals("keep")) return true;
					}
					return false;
				} else if (building.equals("gnomish-inventor")) {
					int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
					WargusPlayer player = state.getPlayer(playerID);
					for(WargusUnit u:player.getUnits()) {
						if (WargusStateImporter.unitTypeToString(u.getType()).equals("keep")) return true;
					}
					return false;
				} else if (building.equals("gryphon-aviary")) {
					int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
					WargusPlayer player = state.getPlayer(playerID);
					for(WargusUnit u:player.getUnits()) {
						if (WargusStateImporter.unitTypeToString(u.getType()).equals("castle")) return true;
					}
					return false;
				} else if (building.equals("mage-tower")) {
					int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
					WargusPlayer player = state.getPlayer(playerID);
					for(WargusUnit u:player.getUnits()) {
						if (WargusStateImporter.unitTypeToString(u.getType()).equals("castle")) return true;
					}
					return false;
				} else if (building.equals("gryphon-aviary")) {
					int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
					WargusPlayer player = state.getPlayer(playerID);
					for(WargusUnit u:player.getUnits()) {
						if (WargusStateImporter.unitTypeToString(u.getType()).equals("caslte")) return true;
					}
					return false;
				}
				
			// Human tanker:
			} else if (unit.getType()==26) {
				BuildBOPlan p = (BuildBOPlan)plan;
				String building = WargusStateImporter.unitTypeToString(p.getBuildType());
				
				if (building.equals("human-oil-rig")) {
					return true;
				} 
				
				return false;	
				
			} else if (unit.getType()==3) {
					BuildBOPlan p = (BuildBOPlan)plan;
					String building = WargusStateImporter.unitTypeToString(p.getBuildType());
					
					if (building.equals("great-hall")) {
						return true;
					} else if (building.equals("pig-farm")) {
						return true;
					} else if (building.equals("orc-barracks")) {
						return true;
					} else if (building.equals("troll-lumber-mill")) {
						return true;
					} else if (building.equals("orc-blacksmith")) {
						return true;
					} else if (building.equals("orc-scout-tower")) {
						return true;					
					} else if (building.equals("orc-shipyard")) {
						int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
						WargusPlayer player = state.getPlayer(playerID);
						for(WargusUnit u:player.getUnits()) {
							if (WargusStateImporter.unitTypeToString(u.getType()).equals("troll-lumber-mill")) return true;
						}
						return false;
					} else if (building.equals("orc-foundry")) {
						return true;
					} else if (building.equals("orc-oil-refinery")) {
						return true;
					} else if (building.equals("ogre-mound")) {
						int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
						WargusPlayer player = state.getPlayer(playerID);
						for(WargusUnit u:player.getUnits()) {
							if (WargusStateImporter.unitTypeToString(u.getType()).equals("stronghold")) return true;
						}
						return false;
					} else if (building.equals("goblin-alchemist")) {
						int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
						WargusPlayer player = state.getPlayer(playerID);
						for(WargusUnit u:player.getUnits()) {
							if (WargusStateImporter.unitTypeToString(u.getType()).equals("stronghold")) return true;
						}
						return false;
					} else if (building.equals("temple-of-the-damned")) {
						int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
						WargusPlayer player = state.getPlayer(playerID);
						for(WargusUnit u:player.getUnits()) {
							if (WargusStateImporter.unitTypeToString(u.getType()).equals("fortress")) return true;
						}
						return false;
					} else if (building.equals("dragon-roost")) {
						int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
						WargusPlayer player = state.getPlayer(playerID);
						for(WargusUnit u:player.getUnits()) {
							if (WargusStateImporter.unitTypeToString(u.getType()).equals("fortress")) return true;
						}
						return false;
					} else if (building.equals("altar-of-storms")) {
						int playerID = state.getUnit(plan.getUnitID()).getPlayerID();
						WargusPlayer player = state.getPlayer(playerID);
						for(WargusUnit u:player.getUnits()) {
							if (WargusStateImporter.unitTypeToString(u.getType()).equals("fortress")) return true;
						}
						return false;
					}
					
				// Orc Tankers:
				} else if (unit.getType()==27) {
					BuildBOPlan p = (BuildBOPlan)plan;
					String building = WargusStateImporter.unitTypeToString(p.getBuildType());
					
					if (building.equals("orc-oil-rig")) {
						return true;
					} 
					
					return false;					
			}
		} if (plan instanceof MoveBOPlan) {
			return canMove(WargusStateImporter.unitTypeToString(unit.getType()));
		} if (plan instanceof RepairBOPlan) {
			if (unit.getType()==2 || unit.getType()==3) return true;
			return false;
		} if (plan instanceof ResearchBOPlan) {
			ResearchBOPlan p = (ResearchBOPlan)plan;
			String research = p.getResearch();
			String unit_type = WargusStateImporter.unitTypeToString(unit.getType());
			
			if (unit_type.equals("elven-lumber-mill")) {
				if (research.equals("upgrade-arrow1")) return true;
				else if (research.equals("upgrade-arrow2")) return true;   // requires 4
				else if (research.equals("upgrade-ranger")) return true;  // requires keep
				else if (research.equals("upgrade-longbow")) return true; // requires castle
				else if (research.equals("upgrade-ranger-scouting")) return true; // requires castle
				else if (research.equals("upgrade-ranger-marksmanship")) return true; // requires castle	
				return false;
			} else if (unit_type.equals("human-blacksmith")) {
				if (research.equals("upgrade-sword1")) return true;
				else if (research.equals("upgrade-sword2")) return true; // requires 0
				else if (research.equals("upgrade-human-shield1")) return true;	
				else if (research.equals("upgrade-human-shield2")) return true;  // requires 8
				else if (research.equals("upgrade-ballista1")) return true;
				else if (research.equals("upgrade-ballista2")) return true;  // requires 22
				return false;
			} else if (unit_type.equals("mage-tower")) {
				if (research.equals("upgrade-slow")) return true;
				else if (research.equals("upgrade-flame-shield")) return true;
				else if (research.equals("upgrade-invisibility")) return true;
				else if (research.equals("upgrade-polymorph")) return true;
				else if (research.equals("upgrade-blizzard")) return true;
				return false;
			} else if (unit_type.equals("church")) {
				if (research.equals("upgrade-paladin")) return true;
				else if (research.equals("upgrade-healing")) return true; // requires 32
				else if (research.equals("upgrade-exorcism")) return true; // requires 32
				return false;				
			} else if (unit_type.equals("human-foundry")) {
				if (research.equals("upgrade-human-ship-cannon1")) return true;
				else if (research.equals("upgrade-human-ship-cannon2")) return true;
				else if (research.equals("upgrade-human-ship-armor1")) return true;
				else if (research.equals("upgrade-human-ship-armor2")) return true;				
				return false;				
				
				
			} else if (unit_type.equals("troll-lumber-mill")) {
				if (research.equals("upgrade-throwing-axe1")) return true;
				else if (research.equals("upgrade-throwing-axe2")) return true; // requires 6
				else if (research.equals("upgrade-berserker")) return true; // requires stronghold
				else if (research.equals("upgrade-light-axes")) return true; // requires fortress
				else if (research.equals("upgrade-berserker-scouting")) return true; // requires fortress
				else if (research.equals("upgrade-berserker-regeneration")) return true; // requires fortress		
				return false;
			} else if (unit_type.equals("orc-blacksmith")) {
				if (research.equals("upgrade-battle-axe1")) return true;
				else if (research.equals("upgrade-battle-axe2")) return true;	// requies 2
				else if (research.equals("upgrade-orc-shield1")) return true;
				else if (research.equals("upgrade-orc-shield2")) return true; // requires 10
				else if (research.equals("upgrade-catapult1")) return true;
				else if (research.equals("upgrade-catapult2")) return true; // requires 20
				return false;
			} else if (unit_type.equals("orc-foundry")) {
				if (research.equals("upgrade-orc-ship-cannon1")) return true;
				else if (research.equals("upgrade-orc-ship-cannon2")) return true;	// requires 14
				else if (research.equals("upgrade-orc-ship-armor1")) return true;
				else if (research.equals("upgrade-orc-ship-armor2")) return true;	// requires 18
				return false;
			}
			
		} if (plan instanceof ResourceBOPlan) {
			if (unit.getType()==2 || unit.getType()==3 || unit.getType()==26 || unit.getType()==27) return true;
			return false;		
		} if (plan instanceof ResourceLocationBOPlan) {
			if (unit.getType()==2 || unit.getType()==3 || unit.getType()==26 || unit.getType()==27) return true;
			return false;
		} if (plan instanceof StandGroundBOPlan) {			
			return canAttack(WargusStateImporter.unitTypeToString(unit.getType()));			
		} if (plan instanceof StopBOPlan) {
			return true;
		} if (plan instanceof TrainBOPlan) {
			TrainBOPlan p = (TrainBOPlan)plan;
			return canTrain(WargusStateImporter.unitTypeToString(unit.getType()), WargusStateImporter.unitTypeToString(p.getUnitType()));
		} if (plan instanceof UpgradeBOPlan) {
			
			UpgradeBOPlan p = (UpgradeBOPlan)plan;
			String upgrade = p.getUpgrade();
			String unit_type = WargusStateImporter.unitTypeToString(unit.getType());
			
			if (unit_type.equals("town-hall")) {
				if (upgrade.equals("keep")) return true; // needs balcksmith 				
				return false;
			} else if (unit_type.equals("keep")) {
				if (upgrade.equals("castle")) return true; // needs lumber mill 				
				return false;
			} else if (unit_type.equals("great-hall")) {
				if (upgrade.equals("stronghold")) return true; // needs blacksmith 				
				return false;
			} else if (unit_type.equals("stronghold")) {
				if (upgrade.equals("castle")) return true; // needs lumber mill 				
				return false;
			} else if (unit_type.equals("human-scout-tower")) {
				if (upgrade.equals("human-guard-tower")) return true; // needs lumber mill 				
				if (upgrade.equals("human-cannon-tower")) return true; // needs lumber mill 				
				return false;
			} else if (unit_type.equals("orc-scout-tower")) {
				if (upgrade.equals("orc-guard-tower")) return true; // needs lumber mill 				
				if (upgrade.equals("orc-cannon-tower")) return true; // needs lumber mill 				
				return false;
			}			
		}

		return false;
	}
	*/
	
}
