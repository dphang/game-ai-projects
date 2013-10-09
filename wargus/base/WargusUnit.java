/*********************************************************************************
Organization 					: 				Georgia Institute of Technology
  
Institute 						:				Cognitive Computing Group(CCL)
 
Authors							: 				Santi Onta��n
												Kane Bonnette
 												Sooraj Bhat
 												
Class							:				Unit 
 
Function						: 				Class to hold the unit information
****************************************************************************/
package base;

import java.io.Serializable;
import java.util.Arrays;

import org.jdom.Element;


/**
 * Reimplementation of Unit.cpp in Java
 * Note that it also handles buildings
 * Santi Edits:
 * 	- disentangled this class from the proxybot so that it can be reused for other purposes
 */
public class WargusUnit implements Serializable {

	/**
	 * The globally unique, never repeated ID of this unit - 
	 * even after this unit dies, this number will not be reused!
	 */
	int m_unitID;

	/**
	 * Until other wise noted, I'm using the following values
	 * for unit types. Since Orcs/Humans have the same units, the value
	 * is the same for the equivalent unit.
	 *	-1, -1, unused
	 *	0, 1, unit-footman unit-grunt
	 *	2, 3, unit-peasant unit-peon
	 *	4, 5, unit-ballista unit-catapult
	 *	6, 7, unit-knight unit-ogre
	 *	8, 9, unit-archer unit-axethrower
	 *	10, 11, unit-mage unit-death-knight
	 *  12, 13, unit-paladin, unit-ogre-mage
	 *	14, 15, unit-dwarves unit-goblin-sappers
	 *	26, 27, unit-human-oil-tanker unit-orc-oil-tanker
	 *	28, 29, unit-human-transport unit-orc-transport
	 *	30, 31, unit-elven-destroyer unit-troll-destroyer
	 *	32, 33, unit-battleship unit-ogre-juggernaught
	 *	38, 39, unit-gnomish-submarine unit-giant-turtle
	 *	40, 41, unit-gnomish-flying-machine unit-goblin-zeppelin
	 *	42, 43, unit-gryphon-rider unit-dragon
	 *  45,     eye of kilrogg
	 *  54,     skeleton
	 *  57,     critter
	 *	58, 59, unit-farm unit-pig-farm
	 *	60, 61, unit-human-barracks unit-orc-barracks
	 *	62, 63, unit-church unit-altar-of-storms
	 *	64, 65, unit-human-watch-tower unit-orc-watch-tower
	 *	66, 67, unit-stables unit-ogre-mound
	 *	68, 69, unit-gnomish-inventor unit-goblin-alchemist
	 *	70, 71, unit-gryphon-aviary unit-dragon-roost
	 *	72, 73, unit-human-shipyard unit-orc-shipyard
	 *	74, 75, unit-town-hall unit-great-hall
	 *	76, 77, unit-elven-lumber-mill unit-troll-lumber-mill
	 *	78, 79, unit-human-foundry unit-orc-foundry
	 *	80, 81, unit-mage-tower unit-temple-of-the-damned
	 *	82, 83, unit-human-blacksmith unit-orc-blacksmith
	 *	84, 85, unit-human-refinery unit-orc-refinery
	 *	88, 89, unit-keep unit-stronghold
	 *	90, 91, unit-castle unit-fortress
	 *  96, 97, unit-human-guard-tower
	 *  98, 99, unit-human-cannon-tower
	 *  92, 93	unit-goldmine, unit-oil-patch
	 *	103,104 unit-human-wall unit-orc-wall
	 */
	int m_type;	
	int m_lifetime = 0;
	int m_x,m_y;
	int m_hitPoints; 			// The current hit points of this unit
	int m_magicPoints; 			// The current magic points of this unit
	int m_resourcesCarrying; 	// how much gold/wood/oil the unit is carrying
	int m_kills; 				// How many kills this unit has
	int m_lastAttacked;			// Last cycle that the unit was attacked
	int m_playerID; 			//what player the unit belongs to.

	
	
	/**
	 * 0 is the currently performing action
	 * 1 and 2 are the status arguments that relate to each status flag - some
	 * statuses use both flags, others, just one - check GetUnitStatus() 
	 * in stratagus/src/socket/socket.c
	 */
	int[] m_statusFlags = {-1, -1, -1}; // Status flag for this unit
	
	public WargusUnit(int inc_unitID, int inc_type, int inc_hitPoints, int mp, int inc_locX,
			int inc_locY, int inc_rAmount, int inc_kills, int lastAttacked,
			int[] stat, int player) {
		m_unitID = inc_unitID;
		m_type = inc_type;
		m_x = inc_locX;
		m_y = inc_locY;
		m_hitPoints = inc_hitPoints;
		m_magicPoints = mp;
		m_resourcesCarrying = inc_rAmount;
		m_kills = inc_kills;
		m_lastAttacked = lastAttacked;
		m_statusFlags[0] = stat[0];
		m_statusFlags[1] = stat[1];
		m_statusFlags[2] = stat[2];		
		m_playerID = player;
				
	}
	
	public WargusUnit(int unitid,int player,String type,int x,int y,int currenthitpoints, int currentmagicpoints)
	{
		// Remove that stupid "unit-" that Stratagus adds to some unit names
		if (type.startsWith("unit-")) {
			type = type.substring("unit-".length());
		} // if 
		
		m_unitID = unitid;
		m_type = WargusStateImporter.unitTypeToInteger(type);
		m_x = x;
		m_y = y;
		m_hitPoints = currenthitpoints;
		m_magicPoints = currentmagicpoints;
		m_resourcesCarrying = 0;
		m_kills = 0;
		m_statusFlags[0] = 0;
		m_statusFlags[1] = 0;
		m_statusFlags[2] = 0;
		m_playerID = player;
	}
	
	public WargusUnit(int unitid,int player,String type,int x,int y,int currenthitpoints, int currentmagicpoints, String status)
	{
		// Remove that stupid "unit-" that Stratagus adds to some unit names
		if (type.startsWith("unit-")) {
			type = type.substring("unit-".length());
		}
		
		m_unitID = unitid;
		m_type = WargusStateImporter.unitTypeToInteger(type);
		m_x = x;
		m_y = y;
		m_hitPoints = currenthitpoints;
		m_magicPoints = currentmagicpoints;
		m_resourcesCarrying = 0;
		m_kills = 0;
		m_statusFlags[0] = WargusStateImporter.statusToInteger(status);
		m_statusFlags[1] = 0;
		m_statusFlags[2] = 0;
		m_playerID = player;
	}	

	public WargusUnit(WargusUnit u)
	{
		m_unitID = u.m_unitID;
		m_type = u.m_type;
		m_x = u.m_x;
		m_y = u.m_y;
		m_hitPoints = u.m_hitPoints;
		m_magicPoints = u.m_magicPoints;
		m_resourcesCarrying = u.m_resourcesCarrying;
		m_kills = u.m_kills;
		m_statusFlags[0] = u.m_statusFlags[0];
		m_statusFlags[1] = u.m_statusFlags[1];
		m_statusFlags[2] = u.m_statusFlags[2];
		m_playerID = u.m_playerID;	
	}	

	public static final int[] CivilianUnits;
	static {
		CivilianUnits = new int[]{2,3,58,59,74,75,76,77};
		Arrays.sort(CivilianUnits);
		// assume all non-Civilian are Military
	}
	
	/**
	 * Special classes for identifying different unit types. 
	 */
	public abstract class IsUnit { abstract public boolean test(int type); }
	// i am so bad...
	private static final WargusUnit FAKEUNIT = new WargusUnit(-1,-1,"sheep",-1,-1,-1,-1);
	private class IsFlyingUnit extends IsUnit {
		public boolean test(int type) {
			return (type >= 40 && type <= 43);
		}
	}
	private static IsFlyingUnit ifu = null;
	public static IsFlyingUnit isFlyingUnit() {
		if (ifu == null) ifu = FAKEUNIT.new IsFlyingUnit();
		return ifu;
	}
	private class IsLandUnit extends IsUnit {
		// catches buildings and mobile units
		public boolean test(int type) {
			return (type >= 0 && type <= 15 || type == 45 || type == 54) 
				&& !isWaterUnit().test(type);
			// not considering critters...
		}	
	}
	private static IsLandUnit ilu = null;
	public static IsLandUnit isLandUnit() {
		if (ilu == null) ilu = FAKEUNIT.new IsLandUnit();
		return ilu;
	}
	private class IsWaterUnit extends IsUnit {
		// all the ORs catch the water buildings
		public boolean test(int type) {
			return type >= 26 && type <= 39 || type == 72 || type == 73 ||
				type == 78 || type == 79 || type == 84 || type == 85;    
		}
	}
	private static IsWaterUnit iwu = null;
	public static IsWaterUnit isWaterUnit() {
		if (iwu == null) iwu = FAKEUNIT.new IsWaterUnit();
		return iwu;
	}
	public static boolean isWaterUnit(int type) {
		return isWaterUnit().test(type);
	}
	
	private class IsMobileUnit extends IsUnit {
		public boolean test(int type) {
			return (type < 58 && type >= 0);
		}		
	}
	private static IsMobileUnit imu = null;
	public static IsMobileUnit isMobileUnit() {
		if (imu == null) imu = FAKEUNIT.new IsMobileUnit();
		return imu;
	}
	private class IsBuildingUnit extends IsUnit {
		public boolean test(int type) {
			return (type >= 58 && type <= 104 && 
					// don't return gold mines or oil patches..
					type != 92 && type != 93);
		}		
	}
	private static IsBuildingUnit ibu = null;
	public static IsBuildingUnit isBuildingUnit() {
		if (ibu == null) ibu = FAKEUNIT.new IsBuildingUnit();
		return ibu;
	}
	
	private class IsBombardingUnit extends IsUnit {
		public boolean test(int type) {
			return (type == 4 || type == 5 || type == 32 || type == 33);
		}
	}
	private static IsBombardingUnit iau = null;
	public static IsBombardingUnit isBombardingUnit() {
		if (iau == null) iau = FAKEUNIT.new IsBombardingUnit();
		return iau;
	}
	
	
	/**
	 * finds the cycles left until the unit is destroyed,
	 * or how long the unit has been alive.
	 * eye of kilrogg is limited to 765 cycles
	 * skeletons are limited to 25,500 cycles
	 * corpses are limited to 1,000 cycles
	 * everything else just returns time since it finished training
	 * @return positive if cycles alive, negative if counting down to doom.
	 */
	public int getLifetime() {
		switch (m_type) {
		case -1: //corpses
			return -(1000 - m_lifetime);
		case 45: //eye of kilrogg
			return -(765 - m_lifetime);
		case 54: //skeleton
			return -(255000 - m_lifetime);
		default:
			return m_lifetime;
		}
	}

	/**
	 * current hit points of the unit
	 * @return hitpoints
	 */
	public int getHitPoints() {
		return m_hitPoints;
	}

	/**
	 * maximum HP of given unit. if -1 is returned, unit type
	 * is unknown. Should only be daemons and the like;
	 * all units the play can use are handled. 
	 * @return max HP
	 */
	public int getMaxHP() {
		switch (m_type) {
		case 0://footman
		case 1: 
			return 60;
		case 2://peasant/peon
		case 3: 
			return 30;
		case 4: //ballista
		case 5:
			return 110;
		case 6: //knight
		case 7:
			return 90;
		case 8: //archer
		case 9://axethrower
			return 40;
		case 10: //mage
		case 11:
			return 60;
		case 12: //paladin
		case 13:
			return 90;
		case 14: //Demo Squad
		case 15:
			return 40;
		case 18://axethrower
		case 19:
			return 50;
		case 26: // tanker
		case 27:
			return 90;
		case 28: //transport
		case 29:
			return 150;
		case 30: //Destroyer
		case 31:
			return 100;
		case 32: //Battleship
		case 33:
			return 150;
		case 38: //Sub
		case 39:
			return 60;
		case 40: //Flying machine
		case 41:
			return 150;
		case 42: //Gryphon
		case 43:
			return 100;
		case 45: //eye of kilrogg
			return 100;
		case 54: //skeleton
			return 40;
		case 57: //critter
			return 4;
		case 58: //Farm
		case 59:
			return 400;
		case 60: //Barracks
		case 61:
			return 800;
		case 62: //Church
		case 63:
			return 700;
		case 64: //watch tower
		case 65:
			return 100;
		case 66: //stables
		case 67:
			return 500;
		case 68: //Inventor
		case 69:
			return 500;
		case 70: //Aviary
		case 71:
			return 500;
		case 72: //shipyard
		case 73:
			return 1100;
		case 74: //Town Hall
		case 75:
			return 1200;
		case 76: //Lumber Mill
		case 77:
			return 600;
		case 78: //foundry
		case 79:
			return 750;
		case 80: //Mage Tower
		case 81:
			return 500;
		case 82: //Blacksmith
		case 83:
			return 775;
		case 84: //refinery
		case 85:
			return 600;
		case 88: //keep
		case 89:
			return 1400;
		case 90: //Castle
		case 91:
			return 1600;
		case 92://goldmine
			return 99999999;
		case 96: //guard tower
		case 97:
			return 130;
		case 98: //cannon tower
		case 99:
			return 160;
		case 103://wall
		case 104:
			return 50;
		default:// something is very, very wrong
			throw new Error("Unrecognized unit in WargusUnit.getMaxHP()!");
		}
	}

	/**
	 * returns current number of magic points
	 * @return current MP
	 */
	public int getMagicPoints() {
		return m_magicPoints;
	}

	/**
	 * returns maximum MP based on class
	 * only affects mage, death-knight, paladin & ogre-magi
	 * @return max MP
	 */
	public int getMaxMP() {
		switch (m_type) {
		case 10: //mage
		case 11: //death-knight
		case 12://paladin
		case 13://ogre-magi
			return 255;
		default:
			return 0;
		}
	}

	/**
	 * if the unit has no hit points, it must be dead
	 * @return true if dead, false if alive
	 */
	public boolean IsDead() {
		if (m_hitPoints < 1) {
			return true;
		}
		return false;
	}

	/**
	 * factors in class and upgrades to find max possible damage
	 * @return maximum amount of damage
	 */
	public int getMaxDamage(boolean[] advances) {
		int damage = 0;
		switch (m_type) {
		case 0://footman
		case 1: 
			if (advances[0] || advances[2]) {
				damage += 2;
			}
			if (advances[1] || advances[3]) {
				damage += 2;
			}
			return damage + 9;
		case 2://peasant/peon
		case 3: 
			return damage + 9;
		case 4: //ballista
		case 5:
			if (advances[20] || advances[22]) {
				damage += 15;
			}
			if (advances[21] || advances[23]) {
				damage += 15;
			}
			return damage + 80;
		case 6: //knight
		case 7:
			if (advances[0] || advances[2]) {
				damage += 2;
			}
			if (advances[1] || advances[3]) {
				damage += 2;
			}
			return damage + 12;
		case 8: //archer
		case 9://axethrower
			if (advances[4] || advances[6]) {
				damage += 1;
			}
			if (advances[5] || advances[7]) {
				damage += 1;
			}
			return damage + 9;
		case 10: //mage
		case 11:
			return 9;
		case 12: //paladin
		case 13:
			if (advances[0] || advances[2]) {
				damage += 2;
			}
			if (advances[1] || advances[3]) {
				damage += 2;
			}
			return damage + 12;
		case 14: //Demo Squad
		case 15:
			return 1;
		case 18://ranger
			if (advances[27]) {
				damage += 1;
			}
		case 19:
			if (advances[4] || advances[6]) {
				damage += 1;
			}
			if (advances[5] || advances[7]) {
				damage += 1;
			}
			return damage + 9;
		case 30: //Destroyer
		case 31:
			if (advances[12] || advances[14]) {
				damage += 5;
			}
			if (advances[13] || advances[15]) {
				damage += 5;
			}
			return damage + 35;
		case 32: //Battleship
		case 33:
			if (advances[12] || advances[14]) {
				damage += 5;
			}
			if (advances[13] || advances[15]) {
				damage += 5;
			}
			return damage + 130;
		case 38: //Sub
		case 39:
			return 50;
		case 42: //Gryphon
		case 43:
			return 16;
		case 54: //skeleton
			return 9;
		case 96: //guard tower
		case 97:
			return 16;
		case 98: //cannon tower
		case 99:
			return 50;
		}
		return 0;
	}

	/**
	 * factors in class and upgrades to find min possible damage
	 * @return minimum amount of damage
	 */
	public int getMinDamage(boolean[] advances) {
		int damage = 0;
		switch (m_type) {
		case 0://footman
		case 1: 
			if (advances[0] || advances[2]) {
				damage += 2;
			}
			if (advances[1] || advances[3]) {
				damage += 2;
			}
			return damage + 2;
		case 2://peasant/peon
		case 3: 
			return 2;
		case 4: //ballista
		case 5:
			if (advances[20] || advances[22]) {
				damage += 15;
			}
			if (advances[21] || advances[23]) {
				damage += 15;
			}
			return damage + 25;
		case 6: //knight
		case 7:
			if (advances[0] || advances[2]) {
				damage += 2;
			}
			if (advances[1] || advances[3]) {
				damage += 2;
			}
			return damage + 2;
		case 8: //archer
		case 9://axethrower
			if (advances[4] || advances[6]) {
				damage += 1;
			}
			if (advances[5] || advances[7]) {
				damage += 1;
			}
			return damage + 3;
		case 10: //mage
		case 11:
			return 0;
		case 12: //paladin
		case 13:
			if (advances[0] || advances[2]) {
				damage += 2;
			}
			if (advances[1] || advances[3]) {
				damage += 2;
			}
			return damage + 2;
		case 14: //Demo Squad
		case 15:
			return damage + 1;
		case 18://ranger
			if (advances[27]) {
				damage += 1;
			}
		case 19:
			if (advances[4] || advances[6]) {
				damage += 1;
			}
			if (advances[5] || advances[7]) {
				damage += 1;
			}
			return damage + 3;
		case 30: //Destroyer
		case 31:
			if (advances[12] || advances[14]) {
				damage += 5;
			}
			if (advances[13] || advances[15]) {
				damage += 5;
			}
			return damage + 2;
		case 32: //Battleship
		case 33:
			if (advances[12] || advances[14]) {
				damage += 5;
			}
			if (advances[13] || advances[15]) {
				damage += 5;
			}
			return damage + 50;
		case 38: //Sub
		case 39:
			return 10;
		case 42: //Gryphon
		case 43:
			return 8;
		case 54: //skeleton
			return 2;
		case 96: //guard tower
		case 97:
			return 6;
		case 98: //cannon tower
		case 99:
			return 10;
		}
		return 0;
	}

	/**
	 * factors in class and upgrades to find range of unit's attack
	 * 0 if the unit has no attack
	 * @return range of unit's attack
	 */
	public int getRange(boolean[] advances) {
		switch (m_type) {
		case 0://footman
		case 1: 
			return 1;
		case 2://peasant/peon
		case 3: 
			return 1;
		case 4: //ballista
		case 5:
			return 8;
		case 6: //knight
		case 7:
			return 1;
		case 8: //archer
		case 9://axethrower
			return 4;
		case 10: //mage
		case 11:
			return 2;
		case 12: //paladin
		case 13:
			return 1;
		case 14: //Demo Squad
		case 15:
			return 1;
		case 18://ranger
			
		case 19:
			if (advances == null || advances[25] || advances[29]) {
				return 5;
			}
			return 4;
		case 30: //Destroyer
		case 31:
			return 4;
		case 32: //Battleship
		case 33:
			return 6;
		case 38: //Sub
		case 39:
			return 4;
		case 42: //Gryphon
		case 43:
			return 4;
		case 54: //skeleton
			return 1;
		case 96: //guard tower
		case 97:
			return 6;
		case 98: //cannon tower
		case 99:
			return 7;
		}
		return 0;
	}
	
	/**
	 * base speed of unit. does not handle hasting
	 * 0 if the unit cannot move
	 * @return speed of unit
	 */
	public int getSpeed() {
		switch (m_type) {
		case 0://footman
		case 1: 
			return 10;
		case 2://peasant/peon
		case 3: 
			return 10;
		case 4: //ballista
		case 5:
			return 5;
		case 6: //knight
		case 7:
			return 13;
		case 8: //archer
		case 9://axethrower
			return 10;
		case 10: //mage
		case 11:
			return 8;
		case 12: //paladin
		case 13:
			return 13;
		case 14: //Demo Squad
		case 15:
			return 11;
		case 18://ranger
		case 19:
			return 4;
		case 26: // tanker
		case 27:
			return 10;
		case 28: //transport
		case 29:
			return 10;
		case 30: //Destroyer
		case 31:
			return 10;
		case 32: //Battleship
		case 33:
			return 6;
		case 38: //Sub
		case 39:
			return 7;
		case 40: //Flying machine
		case 41:
			return 17;
		case 42: //Gryphon
		case 43:
			return 14;
		case 54: //skeleton
			return 8;
		default:
			return 0;
		}
	}

	/**
	 * factors in class and upgrades to find unit's defense
	 * 0 if the unit has no defense
	 * @return unit's defense
	 */
	public int getArmor(boolean[] advances) {
		int armor = 0;
		switch (m_type) {
		case 0://footman
		case 1: 
			armor = 2;
			if (advances[8] || advances[10]) {
				armor += 2;
			}
			if (advances[9] || advances[11]) {
				armor += 2;
			}
			break;
		case 6: //knight
		case 7:
			armor = 4;
			if (advances[8] || advances[10]) {
				armor += 2;
			}
			if (advances[9] || advances[11]) {
				armor += 2;
			}
			break;
		case 12: //paladin
		case 13:
			armor = 4;
			if (advances[8] || advances[10]) {
				armor += 2;
			}
			if (advances[9] || advances[11]) {
				armor += 2;
			}
			break;
		case 28: //transport
		case 29:
			armor = 0;
			if (advances[16] || advances[18]) {
				armor += 5;
			}
			if (advances[17] || advances[19]) {
				armor += 5;
			}
			break;
		case 30: //Destroyer
		case 31:
			armor = 10;
			if (advances[16] || advances[18]) {
				armor += 5;
			}
			if (advances[17] || advances[19]) {
				armor += 5;
			}
			break;
		case 32: //Battleship
		case 33:
			armor = 15;
			if (advances[16] || advances[18]) {
				armor += 5;
			}
			if (advances[17] || advances[19]) {
				armor += 5;
			}
			break;
		case 58: //Farm
		case 59:
			armor = 20;
		case 60: //Barracks
		case 61:
			armor = 20;
		case 62: //Church
		case 63:
			armor = 20;
		case 64: //watch tower
		case 65:
			armor = 20;
		case 66: //stables
		case 67:
			armor = 20;
		case 68: //Inventor
		case 69:
			armor = 20;
		case 70: //Aviary
		case 71:
			armor = 20;
		case 72: //shipyard
		case 73:
			armor = 20;
		case 74: //Town Hall
		case 75:
			armor = 20;
		case 76: //Lumber Mill
		case 77:
			armor = 20;
		case 78: //foundry
		case 79:
			armor = 20;
		case 80: //Mage Tower
		case 81:
			armor = 20;
		case 82: //Blacksmith
		case 83:
			armor = 20;
		case 84: //refinery
		case 85:
			armor = 20;
		case 88: //keep
		case 89:
			armor = 20;
		case 90: //Castle
		case 91:
			armor = 20;
		case 92://goldmine
			armor = 20;
		case 96: //guard tower
		case 97:
			armor = 20;
		case 98: //cannon tower
		case 99:
			armor = 20;
		case 103://wall
		case 104:
			armor = 20;
		}
		return armor;
	}
	
	/**
	 * Determines if the unit is a structure
	 * @return true if unit is a structure, false otherwise
	 */
	public boolean isStructure()
	{
		switch (m_type) {
		case 58: //Farm
		case 59:
		case 60: //Barracks
		case 61:
		case 62: //Church
		case 63:
		case 64: //watch tower
		case 65:
		case 66: //stables
		case 67:
		case 68: //Inventor
		case 69:
		case 70: //Aviary
		case 71:
		case 72: //shipyard
		case 73:
		case 74: //Town Hall
		case 75:
		case 76: //Lumber Mill
		case 77:
		case 78: //foundry
		case 79:
		case 80: //Mage Tower
		case 81:
		case 82: //Blacksmith
		case 83:
		case 84: //refinery
		case 85:
		case 88: //keep
		case 89:
		case 90: //Castle
		case 91:
		case 96: //guard tower
		case 97:
		case 98: //cannon tower
		case 99:
		case 103://wall
		case 104:
			return true;
		default:
			return false;			
		}
		
		// NOTE: Are oil rigs handled?
	}
	
	/**
	 * Determines if the unit is a controllable unit
	 * @return true if unit is a controllable unit, false otherwise
	 */
	public boolean isUnit()
	{
		switch (m_type) {
		case 0://footman
		case 1: 
		case 2://peasant/peon
		case 3: 
		case 4: //ballista
		case 5:
		case 6: //knight
		case 7:
		case 8: //archer
		case 9://axethrower
		case 10: //mage
		case 11:
		case 12: //paladin
		case 13:
		case 14: //Demo Squad
		case 15:
		case 18://axethrower
		case 19:
		case 26: // tanker
		case 27:
		case 28: //transport
		case 29:
		case 30: //Destroyer
		case 31:
		case 32: //Battleship
		case 33:
		case 38: //Sub
		case 39:
		case 40: //Flying machine
		case 41:
		case 42: //Gryphon
		case 43:
		case 45: //eye of kilrogg
		case 54: //skeleton
			return true;
		default:
			return false;			
		}
		
		// NOTE: Are death knights handled?
	}
	
	/**
	 * Determines if the unit is a resource
	 * @return true if unit is a resource, false otherwise
	 */
	public boolean isResource()
	{
		switch (m_type) {
		case 92://goldmine
		case 93://oil patch
			return true;
		default:
			return false;			
		}

		// NOTE: Trees?
	}	
	
	public float distance(WargusUnit u) //Added by Rushabh 10/28/2007
	{
		float dpos = 1;
		if(u != null) {
			float d = (float)Math.sqrt((m_x-u.m_x)*(m_x-u.m_x) + (m_y-u.m_y)*(m_y-u.m_y));
			dpos = Math.min(d/10,1.0f);
		}
		return dpos;
	}
	public float distancefloat(WargusUnit u) //Added by Rushabh 10/28/2007
	{
		float d = 1;
		if(u != null) {
		 d = (float)Math.sqrt((m_x-u.m_x)*(m_x-u.m_x) + (m_y-u.m_y)*(m_y-u.m_y));
		}
		return d;
		
	}
	
	public float distance(WargusUnit u,float wt,float wp,float wpos,float whp,float ws) {
		float dt = 1;
		float dpos = 1;
		float dhp = 1;
		float dstatus = 1;
		float dplayer = 1;
		
		if (m_type == u.m_type) dt=0;
		
		{
			float d = (float)Math.sqrt((m_x-u.m_x)*(m_x-u.m_x) + (m_y-u.m_y)*(m_y-u.m_y));
			dpos = Math.min(d/10,1.0f);
			
		}
		
		{
			float tmp1,tmp2;
			float pu1 = 1;
			float pu2 = 1;
			tmp1 = WargusStateImporter.unitMaxHitPoints(m_type);
			tmp2 = WargusStateImporter.unitMaxHitPoints(u.m_type);
			if (tmp1>0) {
				pu1 = m_hitPoints / WargusStateImporter.unitMaxHitPoints(m_type);
			} else {
			}
			if (tmp2>0) {
				pu2 = u.m_hitPoints / WargusStateImporter.unitMaxHitPoints(u.m_type);
			} else {
			}
			if (tmp1>0 && tmp2>0) dhp = Math.abs(pu1-pu2);
		}
		
		if (m_statusFlags[0] == u.m_statusFlags[0]) dstatus = 0; 
		
		if( m_playerID == u.m_playerID) dplayer = 0;
		
//		System.out.println("DD: " + dt + " " + dplayer + " " + dpos + " " + dhp + " " + dstatus);
		return wt*dt + wp*dplayer+ wpos*dpos + whp*dhp + ws*dstatus;
	}	
	

	
	
	
	

	/**
	 * returns all pertinent info about a Unit
	 * @ return the unit info
	 */
	public String toString(boolean[] advances) {
		String result = "Unit - ";
		result += "id: " + m_unitID;
		result += " type: " + m_type;
		result += " location: (" + m_x + " " + m_y + ")";
		result += " HP: " + getHitPoints() + "/" + getMaxHP();
		result += " MP: " + getMagicPoints() + "/" + getMaxMP();
		result += " Damage: " + getMinDamage(advances) + "/" + getMaxDamage(advances);
		result += " Range: " + getRange(advances);
		result += " Armor: " + getArmor(advances);
		result += " CurrentAction: " + m_statusFlags[0];
		return result;
	}
	
	public String toBriefString() {
		return "ID: " + m_unitID + "[" + m_playerID + "] type: " + WargusStateImporter.unitTypeToString(m_type) + " (" + m_x + "," + m_y + ")";
	}

	public boolean compare(WargusUnit wunit) {
		if(this.m_unitID == wunit.getUnitID()
		  && this.m_type == wunit.getType())
			return true;
		else return false;
	}
	
	/*
	public void writeXML(XMLWriter w) {
		// Dead bodies or destroyed places:
		if (m_type >= 105 && m_type <= 109) return;
		
		w.tag("unit id=\"" + m_unitID + "\"");
		{
			w.tag("type", WargusStateImporter.unitTypeToString(m_type));
			
			w.tag("player", m_playerID);
			w.tag("x", m_x);
			w.tag("y", m_y);
			w.tag("current-hit-points", getHitPoints());
			w.tag("current-magic-points", getMagicPoints());
			
			w.tag("status");			
			{
				w.tag("action", WargusStateImporter.statusToString(m_statusFlags[0]));
				w.tag("arg1", m_statusFlags[1]);
				w.tag("arg2", m_statusFlags[2]);
			}
			w.tag("/status");
			
			w.tag("last-attacked", m_lastAttacked);
			w.tag("resources", m_resourcesCarrying);
			w.tag("kills", m_kills);
		}
		w.tag("/unit");
	}
	*/
	/*
	// special writer to handle null units
	public static void writeXML(XMLWriter w, WargusUnit u) {
		if (u == null) {
			w.tag("unit", "");
		} else {
			u.writeXML(w);
		}
	}
	*/
	
	public static WargusUnit loadFromXML(Element e) {
		assert e.getName().equals("unit") : 
			"WargusUnit.loadFromXML: Invalid XML Element " + e.getName();
		if (e.getChildren().size() == 0) return null; // handles null units
		
		int unitID = Integer.parseInt(e.getAttributeValue("id"));
		int type = WargusStateImporter.unitTypeToInteger(e.getChildText("type"));
		int playerID = Integer.parseInt(e.getChildText("player"));
		int x = Integer.parseInt(e.getChildText("x"));
		int y = Integer.parseInt(e.getChildText("y"));
		
		int hp = Integer.parseInt(e.getChildText("current-hit-points"));
		int mp = Integer.parseInt(e.getChildText("current-magic-points"));
		
		Element statusElement = e.getChild("status");
		int action = WargusStateImporter.statusToInteger(statusElement.getChildText("action"));
		int arg1 = Integer.parseInt(statusElement.getChildText("arg1"));
		int arg2 = Integer.parseInt(statusElement.getChildText("arg2"));
				
		int lastAttacked = Integer.parseInt(e.getChildText("last-attacked"));
		int resourcesCarrying = Integer.parseInt(e.getChildText("resources"));
		int kills = Integer.parseInt(e.getChildText("kills"));
		
		return new WargusUnit(unitID, type, hp, mp, x, y, resourcesCarrying, 
				kills, lastAttacked, new int[]{action, arg1, arg2}, playerID);
	}
	
	public boolean inRangeOf(WargusUnit u) {
		double d = Math.sqrt((m_x-u.m_x)*(m_x-u.m_x) + (m_y-u.m_y)*(m_y-u.m_y)) - 0.5; // substracting 0.5 to correct the rounding that stratagus does
		
		//System.out.println("Is " + m_unitID + " in range of " + u.m_unitID + "? range = " + u.getRange(null) + " distance = " + d);
		if (d<u.getRange(null)) return true;
		return false;
	}
	
	public boolean inRangeOf(WargusUnit u,boolean []advances) {
		double d = Math.sqrt((m_x-u.m_x)*(m_x-u.m_x) + (m_y-u.m_y)*(m_y-u.m_y)) - 0.5; // substracting 0.5 to correct the rounding that stratagus does
		
		//System.out.println("Is " + m_unitID + " in range of " + u.m_unitID + "? range = " + u.getRange(advances) + " distance = " + d);
		if (d<u.getRange(null)) return true;
		return false;
	}

	/**
	 * just a setter
	 * @param hitPoints
	 */
	public void setHitPoints(int hitPoints) {
		m_hitPoints = hitPoints;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public long getKills() {
		return m_kills;
	}

	/**
	 * just a setter
	 * @param kills
	 */
	public void setKills(int kills) {
		m_kills = kills;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public int getLocX() {
		return m_x;
	}

	/**
	 * just a setter
	 * @param locX
	 */
	public void setLocX(int locX) {
		m_x = locX;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public int getLocY() {
		return m_y;
	}

	/**
	 * just a setter
	 * @param locY
	 */
	public void setLocY(int locY) {
		m_y = locY;
	}

	/**
	 * just a setter
	 * @param magicPoints
	 */
	public void setMagicPoints(int magicPoints) {
		m_magicPoints = magicPoints;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public int getRAmount() {
		return m_resourcesCarrying;
	}

	/**
	 * just a setter
	 * @param amount
	 */
	public void setRAmount(int amount) {
		m_resourcesCarrying = amount;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public int[] getStatus() {
		return m_statusFlags;
	}

	/**
	 * just a setter
	 * @param status
	 */
	public void setStatus(int[] status) {
		m_statusFlags[0] = status[0];
		m_statusFlags[1] = status[1];
		m_statusFlags[2] = status[2];
	}

	/**
	 * just a getter
	 * @return value
	 */
	public int getType() {
		return m_type;
	}

	/**
	 * just a setter
	 * @param type
	 */
	public void setType(int type) {
		m_type = type;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public int getUnitID() {
		return m_unitID;
	}

	/**
	 * just a setter
	 * @param unitID
	 */
	public void setUnitID(int unitID) {
		m_unitID = unitID;
	}

	/**
	 * just a setter
	 * @param lifetime
	 */
	public void setLifetime(int lifetime) {
		m_lifetime = lifetime;
	}

	public int getPlayerID() {
		return m_playerID;
	}

	public void setPlayerID(int playerID) {
		m_playerID = playerID;
	}
	
	public int getLastAttacked() {
		return m_lastAttacked;
	}

}
