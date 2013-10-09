/*
 * Daniel Phang
 * Sui Ying Teoh
 * CSE348 Project 4
 * This class manages the use of resources, e.g. in building structures, training units, and upgrading research.
 */

package example;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import base.ProxyBot;
import base.WargusMap;
import base.WargusPlayer;
import base.WargusStateImporter;
import base.WargusUnit;

public class BuildManager {
	
	// Variables for the AI player
	private ProxyBot pb;
	private WargusMap wm;
	private WargusPlayer wp;
	
	// Resource variables
	private long currentGold;
	private long currentWood;
	private long currentOil;
	
	private long previousBuildCycle = 0;
	
	// Lists of buildings and units to build or train
	private LinkedList<String> toTrainList = new LinkedList<String>(); // List of units scheduled to be trained
	private LinkedList<String> toBuildList = new LinkedList<String>(); // List of buildings scheduled to be built
	private LinkedList<String> toUpgradeList = new LinkedList<String>();
	
	// Predefine building order list
	private LinkedList<String> masterBuildList = new LinkedList<String>();
	private LinkedList<String> masterResearchList = new LinkedList<String>();
	
	// HashMap of worker IDs and tasks they are building/build points. This is so we know not to build in pending locations.
	private HashMap<Integer, Rectangle> currentBuildPoints = new HashMap<Integer, Rectangle>();
	private HashMap<Integer, String> currentTasks = new HashMap<Integer, String>();
	
	// Upgrade building IDs
	private int blacksmithID;
	private int elvenID;
	private int mageID;
	private int churchID;
	
	// Whether we have certain buildings
	private boolean hasStables, hasBarracks, hasElven, hasChurch, hasMage, hasBlackSmith, hasAviary;
	
	public BuildManager(ProxyBot pb) {
		this.pb = pb;
		this.wm = WargusStateImporter.getGameStateMap(pb);
		this.wp = WargusStateImporter.getGameStatePlayer(pb);
		this.currentGold = pb.getGold();
		this.currentWood = pb.getWood();
		this.currentOil = pb.getOil();
		initializeBuildList();
		initializeResearchList();
	}
	
	/*
	 * Need to call this in order to update with new data from a ProxyBot instance
	 */
	public void update(ProxyBot pb) {
		this.pb = pb;
		this.wm = WargusStateImporter.getGameStateMap(pb);
		this.wp = WargusStateImporter.getGameStatePlayer(pb);
		
		hasStables = (wp.getUnitsByType("stables").size() > 0);
		hasBarracks = (wp.getUnitsByType("human-barracks").size() > 0);
		hasElven = (wp.getUnitsByType("elven-lumber-mill").size() > 0);
		hasBlackSmith = (wp.getUnitsByType("human-blacksmith").size() > 0);
		hasChurch = (wp.getUnitsByType("church").size() > 0);
		hasMage = (wp.getUnitsByType("mage-tower").size() > 0);
		hasAviary = (wp.getUnitsByType("gryphon-aviary").size() >0);
		
		// Update gold etc.
		this.currentGold = pb.getGold();
		this.currentWood = pb.getWood();
		this.currentOil = pb.getOil();
	}
	
	/*
	 * See if building is complete so we can make a new one
	 */
	public void checkBuildComplete() {
		try{
		if (true) {//cyclesToLastBuild() <= 200) {
			List<WargusUnit> buildings = wp.getBuildings();
			List<WargusUnit> workers = wp.getUnitsByType("peasant");
			for (WargusUnit u : workers) {
				int id = u.getUnitID();
				String status = WargusStateImporter.statusToString(u.getStatus()[0]);
				if (currentTasks.containsKey(id) && status == "working" || status =="repair" ) {
					for (WargusUnit b : buildings) {
						String status2 = WargusStateImporter.statusToString(b.getStatus()[0]);
						String name = WargusStateImporter.unitTypeToString(b.getType());
						if (status2 == "being-built" && currentTasks.containsValue(name)) {
							currentBuildPoints.remove(id);
							//System.out.println("Peek " + toBuildList.peek());
							toBuildList.remove(currentTasks.get(id));
							//System.out.println("Removed " + currentTasks.get(id));
							currentTasks.remove(id);
						}
					}
				}
				else if (currentTasks.containsKey(id) && status != "working")  {
					currentBuildPoints.remove(id);
					currentTasks.remove(id);
				}
			}
		}
		}
		catch(Exception e) {
			
		}
	}
	
	/*
	 * Set the next building to build.
	 */
	public void setBuild() {
		if (!areWorkersBuilding()) { // If workers are not building anything
			if (!hasSupply() && toBuildList.peek() != "farm") // If no supply add farm to the list but only if farm is not already there
				toBuildList.addFirst("farm");
			else if (toBuildList.isEmpty() && !masterBuildList.isEmpty()) // Else add from the predefined list of building but only if it is empty
				toBuildList.addLast(masterBuildList.removeFirst());
		}
	}
	
	/*
	 * Execute building of next building (set earlier).
	 */
	public void doBuild() {
		while (!toBuildList.isEmpty() && hasResourcesForUnit(toBuildList.peek()) && canBuild()) {
			previousBuildCycle = pb.getCurrCycle();
			String buildingName = toBuildList.peek();
			int buildingType = WargusStateImporter.unitTypeToInteger(buildingName); // Remove first to build on list
			int buildingSize = WargusStateImporter.unitSize(buildingType);
			Point p = findBestBuildPoint(buildingSize); // Get the best building location
			int closestPeasantID = getClosestWorkerID(p); // Find the closest free worker
			if (closestPeasantID == 0 || currentTasks.containsValue(buildingName))// currentBuildPoints.containsValue(new Rectangle(p.x, p.y, buildingSize, buildingSize)))
				return;
			pb.build(closestPeasantID, p.x, p.y, false, buildingType); // Build
			preSubtractFromResources(buildingName); // Subtract from resources (even though it technically hasn't been built yet)
			currentBuildPoints.put(closestPeasantID, new Rectangle(p.x, p.y, buildingSize, buildingSize));
			currentTasks.put(closestPeasantID, buildingName);
			//System.out.println("[BUILD][Cycle = " + pb.getCurrCycle() + " ] PeasantID " + closestPeasantID + " is building " + buildingName + " at ("
			//+ p.x + "," + p.y + ")");
		}
	}
	
	/*
	 * Do repairs
	 */
	public void doRepair() {
		List<WargusUnit> units = wp.getBuildings();
		for (WargusUnit u : units) {
			String status = WargusStateImporter.statusToString(u.getStatus()[0]);
			if (u.getHitPoints() < u.getMaxHP() && status != "being-built" ) {
				Point p = new Point(u.getLocX(), u.getLocY());
				int closestWorkerID = getClosestWorkerID(p);
				pb.repair(closestWorkerID, u.getUnitID());
				return;
			}
		}
	}
	
	/*
	 * Do any upgrade of the main structure
	 */
	public void doUpgrade() {
		List<WargusUnit> units = wp.getBuildings();
		int id = 0;
		for (WargusUnit u : units) {
			String name = WargusStateImporter.unitTypeToString(u.getType());
			if (name == "keep" || name == "castle" || name =="town-hall")
				id = u.getUnitID();
		}
		if (hasBarracks && hasBlackSmith && hasElven && hasStables && hasResourcesForUnit("castle")) {
			pb.upgrade(id, 88);
		}
		else if (hasBarracks && hasResourcesForUnit("keep")) {
			pb.upgrade(id, 90);	
		}
	}
	
	/*
	 * Cycles since initiating last build
	 */
	private long cyclesToLastBuild() {
		return pb.getCurrCycle() - previousBuildCycle;
	}
	
	/*
	 * Whether we can build (keeps track of cycles) to prevent building repeatedly (since building takes time)
	 */
	private boolean canBuild() { // Prevent executing too many build actions, also used to check whether a building was built or not
		if (cyclesToLastBuild() <= 100 && pb.getCurrCycle() <= 100)
			return true;
		return cyclesToLastBuild() >= 100;
	}
	
	/*
	 * Set units to train
	 */
	public void setTrain() {
		if (hasSupply() && toTrainList.isEmpty()) {
			String newUnit = getBestUnitToTrain();
			if (newUnit == "none")
				return;
			toTrainList.add(newUnit); // For now train one unit at a time
		}
	}
	
	/*
	 * Train the units
	 */
	public void doTrain() {
		while (!toTrainList.isEmpty() && hasResourcesForUnit(toTrainList.peek())) {
			String trainingName = toTrainList.remove();
			int trainingType = WargusStateImporter.unitTypeToInteger(trainingName);
			List<WargusUnit> buildings = wp.getBuildings();
			for (WargusUnit u : buildings) { // Pick the appropriate building to train unit
				if (WargusStateImporter.canTrain(WargusStateImporter.unitTypeToString(u.getType()), trainingName)) {
					pb.train(u.getUnitID(), trainingType);
					//System.out.println("[TRAIN] BuildingID " + u.getUnitID() + " is training " + trainingName);
					preSubtractFromResources(trainingName); // Subtract from resources
					return; // Only have one building train at a time
				}
			}
		}
	}
	
	/*
	 * Sets the research we should perform
	 */
	public void setResearch() {
		if (toUpgradeList.isEmpty()) 
			toUpgradeList.add(masterResearchList.remove());
		List<WargusUnit> buildings = wp.getBuildings();
		for (WargusUnit u : buildings) {
			String name = WargusStateImporter.unitTypeToString(u.getType());
			blacksmithID = 0;
			elvenID = 0;
			mageID = 0;
			churchID = 0;
			if (name == "human-blacksmith")
				blacksmithID = u.getUnitID();
			else if (name == "elven-lumber-mill")
				elvenID = u.getUnitID();
			else if (name == "mage-tower")
				mageID = u.getUnitID();
			else if (name == "church")
				churchID = u.getUnitID();
		}
	}
	
	/*
	 * Perform pending research when we have enough resources
	 */
	public void doResearch() {
		while (!toUpgradeList.isEmpty() && hasResourcesForUnit(toUpgradeList.peek())) {
			String upgradeName = toUpgradeList.peek(); // peek first
			int upgradeType = WargusStateImporter.researchStringToType(upgradeName);
			int upgraderID = getUpgraderID(upgradeName);
			if (upgraderID == 0) // no upgrade building available;
				return;
			toUpgradeList.remove(); // remove once we confirm there is an upgrade building available
			pb.research(upgraderID, upgradeType);
			//System.out.println("[UPGRADE] BuildingID " + upgraderID + " is upgrading " + upgradeName);
			preSubtractFromResources(upgradeName); // Subtract from resources
			return; // Only have one building train at a time
		}
	}
	
	/*
	 * Find the appropriate building for our given upgrade
	 */
	private int getUpgraderID(String name) {
		if (isBlacksmithUpgrade(name))
			return blacksmithID;
		else if (isElvenUpgrade(name))
			return elvenID;
		else if (isMageUpgrade(name))
			return mageID;
		else if (isChurchUpgrade(name))
			return churchID;
		return 0;
	}
	
	/*
	 * Whether the given string is a blacksmith upgrade, elven lumber mill upgrade, etc.
	 */
	private boolean isBlacksmithUpgrade(String name) {
		if (name == "upgrade-sword1") return true;
		if (name == "upgrade-sword2") return true;
		if (name == "upgrade-human-shield1") return true;
		if (name == "upgrade-human-shield2") return true;
		return false;
	}
	
	private boolean isElvenUpgrade(String name) {
		if (name == "upgrade-arrow1") return true;
		if (name == "upgrade-arrow2") return true;
		if (name == "upgrade-ranger") return true;
		if (name == "upgrade-longbow") return true;
		if (name == "upgrade-ranger-scouting") return true;
		if (name == "upgrade-ranger-marksmanship") return true;
		return false;
	}
	
	private boolean isMageUpgrade(String name) {
		if (name == "upgrade-flame-shield") return true;
		if (name == "upgrade-slow") return true;
		if (name == "upgrade-invisibility") return true;
		if (name == "upgrade-polymorph") return true;
		if (name == "upgrade-blizzard") return true;
		return false;
	}
	
	private boolean isChurchUpgrade(String name) {
		if (name == "upgrade-healing") return true;
		if (name == "upgrade-paladin") return true;
		if (name == "upgrade-exorcism") return true;
		return false;
	}
	
	/*
	 * Check whether workers are building, important so we don't order working units to build
	 */
	private boolean areWorkersBuilding() {
		List<WargusUnit> workers = wp.getUnitsByType("peasant");
		for (WargusUnit u : workers) {
			if (isWorkerWorking(u)) // If at least one worker is working
				return true;
		}
		//System.out.println("DONE ARE WORKERS BUILDING");
		return false;
	}
	
	private boolean isWorkerWorking(WargusUnit u) {
		String status = WargusStateImporter.statusToString(u.getStatus()[0]);
		if (status == "working" || status == "repair") 
			return true;
		return false;
	}
	
	/*
	 * Logic to get new unit to train based on our current armies
	 */
	private String getBestUnitToTrain() {
		int numWorkers = wp.getUnitsByType("peasant").size();
		int numFootmen = wp.getUnitsByType("footman").size();
		int numArchers = wp.getUnitsByType("archer").size();
		int numKnights = wp.getUnitsByType("knight").size();
		int numGryphons = wp.getUnitsByType("gryphon-rider").size();
		int numPaladins = wp.getUnitsByType("paladin").size();
		int numMages = wp.getUnitsByType("mage").size();
		boolean hasStables = (wp.getUnitsByType("stables").size() > 0);
		boolean hasBarracks = (wp.getUnitsByType("human-barracks").size() > 0);
		boolean hasElven = (wp.getUnitsByType("elven-lumber-mill").size() > 0);
		if (numWorkers < 12)
			return "peasant";
		else if (hasChurch && (numPaladins < 7 || numFootmen/numPaladins > 7))
			return "paladin";
		else if (hasMage && (numMages < 7 || numFootmen/numMages > 7))
			return "mage";
		else if (hasAviary && (numGryphons < 7 || numFootmen/numGryphons > 6))
			return "gryphon-rider";
		else if (hasStables && (numKnights < 7 || numFootmen/numKnights > 5))
			return "knight";
		else if (hasElven && (numArchers < 5 || numFootmen/numArchers > 3))
			return "archer";
		else if (hasBarracks && numFootmen < 35)
			return "footman";
		else
			return "none";
	}
	
	/*
	 * Preallocate resources so we don't queue actions that are impossible
	 */
	private void preSubtractFromResources(String unitName) {
		int costGold = WargusStateImporter.unitCostGold(unitName);
		int costWood = WargusStateImporter.unitCostWood(unitName);
		int costOil = WargusStateImporter.unitCostOil(unitName);
		currentGold -= costGold;
		currentWood -= costWood;
		currentOil -= costOil;
	}
	
	/*
	 * Check whether we have enough resources (based on preallocated resources)
	 */
	private boolean hasResourcesForUnit(String unitName) {
		int costGold = WargusStateImporter.unitCostGold(unitName);
		int costWood = WargusStateImporter.unitCostWood(unitName);
		int costOil = WargusStateImporter.unitCostOil(unitName);
		return (currentGold >= costGold && currentWood >= costWood && currentOil >= costOil);
	}
	
	/*
	 * Find the closest worker to a certain point, useful for building structures
	 */
	private int getClosestWorkerID(Point p) {
		List<WargusUnit> workers = wp.getUnitsByType("peasant");
		int closestID = 0;
		double currentDistance = Double.POSITIVE_INFINITY;
		for (WargusUnit u : workers) {
			double tempDistance = p.distance(u.getLocX(), u.getLocY());
			if (wm.isLandPath(p, new Point(u.getLocX(), u.getLocY())) <= tempDistance + 5 && tempDistance < currentDistance && !currentTasks.containsKey(u.getUnitID())) {
				closestID = u.getUnitID();
				currentDistance = tempDistance;
			}
		}
		return closestID;
	}
	
	/*
	 * Find the best build point based on empty land
	 */
	private Point findBestBuildPoint(int size) {
		List<Point> points = wm.findEmptyLand(size);
		Vector<WargusUnit> units = wp.getUnits();
		double currentDistance = Double.POSITIVE_INFINITY;
		Point closestPoint = null;
		for (WargusUnit u : units) {
			//boolean structure = u.isStructure();
			int x = u.getLocX();
			int y = u.getLocY();
			for (Point p : points) {
				// Skip the current point if it's where someone is trying to build, or if it's close to a buildings/trees/goldmine
				double tempDistance = p.distance(x, y);
				if (u.isStructure() && tempDistance != 0 && tempDistance < currentDistance && !closeToOthers(p, size)) {
					closestPoint = p;
					currentDistance = tempDistance;
					
				}
			}
		}
		return closestPoint;
	}
	
	/*
	 * Check whether a point and size is close to a gold mine, tree, or other buildings
	 */
	private boolean closeToOthers(Point p, int size) {
		int xmin = p.x - 1;
		int xmax = p.x + size;
		int ymin = p.y - 1;
		int ymax = p.y + size;
		if (xmin < 0) // In case the coordinates are negative
			xmin = 0;
		if (ymin < 0)
			ymin = 0;
		for (int i = xmin; i <= xmax; i++)
			for (int j = ymin; j <= ymax; j++)
				if (wm.isAreaTree(i, j) || wm.isAreaGold(i, j) || wm.isAreaBuilding(i, j)) {
					return true;
				}
		//System.out.println("return false");
		return false;
	}
	
	/*
	 * Check whether we have supply
	 */
	private boolean hasSupply() {
		return pb.getFood() - pb.getDemand() >= 2;
	}
	
	/*
	 * Predefined build list
	 */
	private void initializeBuildList() {
		masterBuildList.add("human-barracks");
		masterBuildList.add("elven-lumber-mill");
		masterBuildList.add("human-blacksmith");
		masterBuildList.add("stables");
		masterBuildList.add("gnomish-inventor");
		masterBuildList.add("gryphon-aviary");
		masterBuildList.add("mage-tower");
		masterBuildList.add("church");
	}
	
	/*
	 * Predefined build list
	 */
	private void initializeResearchList() {
		masterResearchList.add("upgrade-sword1");
		masterResearchList.add("upgrade-human-shield1");
		masterResearchList.add("upgrade-sword2");
		masterResearchList.add("upgrade-human-shield2");
		masterResearchList.add("upgrade-arrow1");
		masterResearchList.add("upgrade-arrow2");
		masterResearchList.add("upgrade-longbow");
		masterResearchList.add("upgrade-ranger-scouting");
		masterResearchList.add("upgrade-ranger-marksmanship");

	}
}
