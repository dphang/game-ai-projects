/*********************************************************************************
Organization 					: 				Georgia Institute of Technology
  
Institute 						:				Cognitive Computing Group(CCL)
 
Authors							: 				Kane
 												
Class							:				Proxy Bot
 
Function						: 				The class provides to send commands
												to wargus over a socket connectio
****************************************************************************/
package base;

/**
 * Reimplementation of GameProxy.cpp in Java
 * renamed as base.java for compatibility reasons
 * @author Sooraj Bhat
 * @author Kane Bonnette
 */

// This class allows you to interface with Stratagus::Wargus
// Written by Benjamin Brewster
// Send questions, suggestions, complaints to ben@brewsters.net
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;



/**
 * Handles all communication between ABL and Wargus. Caches Wargus info
 * for ABL requests
 * @author kane
 */
public class ProxyBot implements Runnable {

	/**
	 * Holds the map: row 1, col 4 is map[1][4] - originally holds tildes 
	 * (~) at initialization
	 */
	private char[][] map;

	private String savedFilePath = " ";
	/**
	 * Holds the map flags 
	 */
	private int [][]mapflags;
	/**
	 * Holds the visible flags
	 */
	private char[][] mapVisible;
	
	/**
	 * Holds the map tiles 
	 */
	private int [][]maptiles;
		
	/**
	 * Holds all of the units under this agent's control - the index into this 
	 * is the actual global unitID for this unit
	 */
	private ArrayList<WargusUnit> myUnits;

	/**
	 * All units known to be enemies - the index into this is the actual 
	 * global unitID for this unit
	 */
	private ArrayList<WargusUnit> enemyUnits;

	/**
	 * All other units - friends, neutrals, or unknowns - the index into this 
	 * is the actual global unitID for this unit
	 */
	private ArrayList<WargusUnit> friendOrNeutralUnits;

	private int game_state_granularity = 10;
	private int m_server_port = 4870;
	private boolean m_paused = false;
	private String m_serverAddress = null;
	private int[] playerID; // The player IDs, Our player is first
	/** true if human, false if orc */
	private boolean[] race;
	private int mapWidth = 0;
	private int mapLength = 0;
	private long gold = 0;
	private long wood = 0;
	private long oil = 0;
	private long food = 0;
	private long demand = 0;
	private long currCycle = 0;
	private Socket mySocket = null; // This is how the proxy communicates with Stratagus
	private InputStream mySocketIn= null;
	private OutputStream mySocketOut= null;
	/** Make sure to create a new bot if we have more than one player
	 * on the same machine*/
	private static ProxyBot proxy;
	private int numOfPlayers;
	/** locks the send/receive method to prevent deadlock*/
	private boolean sendLock = false;
	/** Locks the state modification to prevent concurrent modification */ 
	private boolean stateLock = false;
	/** holds whether each advance (improve shields, Ranger Upgrade etc) */
	private boolean[] advances;
	/** whether to run with debugging on */
	private String last_message_sent = ""; 
	private boolean m_connected = false;
	private boolean m_reseted = false;	// Stores whether the ProxyBat has at least got the state o the game once
	private boolean m_stopSignal = false;

	/** Stores the list of messages that have been ignored by Stratagus, to resend them again */
	private LinkedList<String> ignoredMessages = new LinkedList<String>();	
	
	private static int DEBUG_LEVEL = 2; // 0: silent, 1: some messages, 2: verbose
	private static final void DEBUG(int lvl, Object msg) {
		if (DEBUG_LEVEL >= lvl) {
			System.out.println(msg);
			System.out.flush();
		}
	}
	
	
	public void stop() {
		m_stopSignal=true;
	}
	
	public boolean stopped() {
		return m_stopSignal;
	}	
	
	public synchronized boolean lockStateCheck() {
		return stateLock;
	}
	
	public synchronized void lockState() throws InterruptedException {
		while(stateLock) {
			System.out.flush();
			wait(); 
		}
		stateLock = true;
	}

	public synchronized void unlockState() {
		stateLock = false;
		notifyAll();
	}
	
	public synchronized void lockSend() throws InterruptedException {
		while(sendLock) {
			System.out.flush();
			wait(); 
		}
		sendLock = true;
	}

	public synchronized void unlockSend() {
		sendLock = false;
		notifyAll();
	}
	
	public long getGold() {
		return gold;
	}
	
	public long getWood() {
		return wood;
	}
	
	public long getOil() {
		return oil;
	}
	
	public long getDemand() {
		return demand;
	}

	public long getFood() {
		return food;
	}
	
	void connect() {
		try {
			proxy = this;
			mySocket = new Socket();
			mySocket.setTcpNoDelay(true);
			mySocket.setSoLinger(true, 100);
			mySocket.connect(new InetSocketAddress(m_serverAddress, m_server_port));
			mySocketIn = new BufferedInputStream(mySocket.getInputStream());
			mySocketOut = new BufferedOutputStream(mySocket.getOutputStream());
			m_connected = true;
		} catch (Exception e) {
			System.out.println("base: Unable to connect to stratagus!");
			m_connected = false;
		}		
	}
	
	/**
	 * handles socket creation and calls next constructor
	 * @param serverAddress - address in either hostname or IP
	 */
	public void reset()
	{		
		if (m_connected) {
			String[] response;	
			response = sendAndReceiveMessage("LISPGET GAMEINFO\n", false).split(" ");
			//#s(gameinfo player-id 0 race 0 width 32 length 32 numPlayers 2 )
			currCycle = 0;
			numOfPlayers = Integer.parseInt(response[10]);
			playerID = new int[numOfPlayers];
			race = new boolean[numOfPlayers];
			playerID[0] = Integer.parseInt(response[2]);
			race[0] = (response[4].contains("0") ? true : false);
			mapWidth = Integer.parseInt(response[6]);
			mapLength = Integer.parseInt(response[8]);
			//Initialize the map
			map = new char[mapLength][mapWidth];
			for (int rowIndex = 0; rowIndex < mapLength; rowIndex++)
				for (int columnIndex = 0; columnIndex < mapWidth; columnIndex++)
					map[rowIndex][columnIndex] = '~';
			gold = wood = oil = 0;
			myUnits = new ArrayList<WargusUnit>();
			enemyUnits = new ArrayList<WargusUnit>();
			friendOrNeutralUnits = new ArrayList<WargusUnit>();
			advances = new boolean[53];
			for(int i = 0; i < 53; i++) {
				advances[i] = false;
			}
			try {
				GetMapState();
			} catch (IOException e) {
				e.printStackTrace();
			}
			GetState();
			getResearch();
			m_reseted = true;
		}
	}
	
	
	public ProxyBot(Properties configuration) {
		m_serverAddress = configuration.getProperty("PROXYBOT_server_address");
		m_server_port = Integer.parseInt(configuration.getProperty("PROXYBOT_server_port"));
		DEBUG_LEVEL = Integer.parseInt(configuration.getProperty("PROXYBOT_debug"));
		connect();
		reset();
	}

	public void cycle() {
		if (!connected()) {
			connect();
			reset();
		} else {
			if (!m_paused) {
				//System.out.println("PROXYBOT: cycle " + currCycle + "(" + ignoredMessages.size() + " ignored)");
				try {
					GetMapState();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				GetState();
				getResearch();
				// since we aren't getting the data every cycle, move forward 10
				// increase to decrease granularity and increase speed
				// lower to increase granularity and decrease speed
				AdvanceNCycles(game_state_granularity);
				currCycle += game_state_granularity;
				if (ignoredMessages.size() > 0) {
					LinkedList<String> tmp = new LinkedList<String>();
					String msg;
					DEBUG(2, "PROXYBOT: " + ignoredMessages.size() + " ignored messages");

					while(ignoredMessages.size() > 0) {
						tmp.add(ignoredMessages.removeFirst());				
					}
				
					while(tmp.size() > 0) {
						msg = tmp.removeFirst();
						DEBUG(1, "PROXYBOT: Resending Message: " + msg);
						
						{
							int unit;
							Scanner s = new Scanner(msg);
							boolean found = false;
							s.next();
							try { // Added by Andrew to try to catch an exception
								// which crashes the system -- haven't caught it yet..
								unit = s.nextInt();
							} catch (Exception e) {
								System.err.println("bad message:" + msg);
								e.printStackTrace();
								unit = -1;
							}
							
							for(WargusUnit u:myUnits) {
								if (u.getUnitID()==unit) found=true;
							}
							
							if (found) sendAndReceiveMessage(msg,true);													
						}
					}
				}
			}
		}
	}

	/**
	 * threaded means of collecting info from Stratagus
	 */
	public void run() {
		while(!m_stopSignal) {
			cycle();
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("base: Finished");
	}
	
	
	public void LoadSavedFileName(String filename) {
		String response = "";
		try{
			response = sendAndReceiveMessage("FORWARDSTATE " + filename + "\n", false);
			if (!response.startsWith("OK")) throw new Exception();
		} catch (Exception e) {
			System.out.println("base: Exception while loading the saved Map State, retrying!");
			if(!m_stopSignal) LoadSavedFileName(filename);
		}
	}
	
	public void GetSavedFileName()
	{
		String response = " ";
		try{
			response = sendAndReceiveMessage("LISPGET ZSAVEFILENAME\n", false);
			if (response!=null) {
				savedFilePath = response.trim();
			} else {
				System.out.println("GetSavedFileName: no connection...");
				savedFilePath = null;
			}		
		} catch (Exception e) {
			unlockState();
			System.out.println("base: Exception while getting the saved Map State, retrying!");
			GetSavedFileName();//try again, we didn't screw our map last time
		}		
	}
	
	public String GetSaveMapFileName()
	{
		return savedFilePath;		
	}
		
	public String GetMapTile(int row, int col) {
		return Integer.toString(maptiles[row][col], 10);
	}
	
	public String GetMapFlag(int row, int col) {
		return Integer.toString(mapflags[row][col], 10);
	}
	
	
	/**
	 * Sends LISPGET RESEARCH, and parses results for advances acquired
	 */
	public void getResearch() {
		String resp = sendAndReceiveMessage("LISPGET RESEARCH\n", false);
		
		if (resp!=null) {
			String[] response = resp.split(" ");
			if (!response[0].contains("research")) {
				return;
			}
					
			for(int i = 2; i < response.length; i+=2) {
				if(response[i].contains("R")) {				
					try {
						int j = WargusStateImporter.researchStringToType(response[i-1]);
						advances[j]=true;
					} catch (Exception e) {
	//					System.out.println("Proxybot.getResearch: unknown research: " + response[i-1]);					
					}				
				} else {
					try {
						int j = WargusStateImporter.researchStringToType(response[i-1]);
						advances[j]=false;
					} catch (Exception e) {
	//					System.out.println("Proxybot.getResearch: unknown research: " + response[i-1]);					
					}				
				}
			}
		} else {
			System.out.println("getResearch: no connection...");
		}
	}		

	/**
	 * Sends LISPGET STATE, and parses results for player info and unit
	 * information. Stores info in appropriate fields
	 * @throws InterruptedException 
	 */
	public synchronized void GetState() {
		String response;
		String globalState[];
		String globalStateAsAWord = "#S(global-state"; 
		int globalStatePosition = 0; 
		String unitAsAWord = "#s(unit"; 
		String paren = "(";
		String spaceAndParens = " ()";
		int unitStartIndex = 0;
		int unitEndIndex = 0; 
		String unitString; 
		boolean haltUnitSearch = false; 
		int unitID; 
		int playerIDOfUnit;
		int type;
		int locX;
		int locY;
		int hp;
		int mp;
		int resourceAmount;
		int kills;
		int lastAttacked;
		int[] statusArgs = new int[3];
		
		response = sendAndReceiveMessage("LISPGET STATE\n", false);
		
		if (response!=null) {
	
			try {
				lockState();
			} catch (Exception e) {
				e.printStackTrace();
			}
			myUnits.clear();
			friendOrNeutralUnits.clear();
			enemyUnits.clear();
	
			// Find where the global state is given
			globalStatePosition = response.indexOf(globalStateAsAWord);
			int endOfGlobalState = response.indexOf(")", globalStatePosition);
			if (globalStatePosition != -1){ // Make sure the global state was found
				globalState =  response.substring(globalStatePosition,
						endOfGlobalState).split(" "); 
				//#S(global-state :gold %d :wood %d :oil %d :food %d :demand %d )
				gold = Integer.parseInt(globalState[2]);
				wood = Integer.parseInt(globalState[4]);
				oil = Integer.parseInt(globalState[6]);
				food = Integer.parseInt(globalState[8]);
				demand = Integer.parseInt(globalState[10]);
			}
	
			/**
			 * Record all unit states
			 */
			while (true) {
				statusArgs = new int[3]; //need so the same array isn't passed to all Units
				// Look for the start of a unit, starting at the end of the 
				//previous unit - unitEndIndex starts at zero the first time
				unitStartIndex = response.indexOf(unitAsAWord, unitEndIndex);
				unitEndIndex = response.indexOf(unitAsAWord, unitStartIndex + 1);
	
				if (unitEndIndex == -1) {
					// We found a unit, but there is no next unit - 
					//this the normal loop termination method
					haltUnitSearch = true; 
					unitStartIndex = response.lastIndexOf(paren, unitStartIndex - 1);
					unitEndIndex = response.length();
				} else {
					// We found a unit, and are aware of the next unit
					unitStartIndex = response.lastIndexOf(paren, unitStartIndex - 1);
					unitEndIndex = response.lastIndexOf(paren, unitEndIndex - 1);
				}
				//System.out.println("Start: " + unitStartIndex + " End: " + unitEndIndex);
				if(unitEndIndex == -1 || unitStartIndex == -1) {
	//				System.out.println("GetState: unlock");
					unlockState();
					return;//something is fubar
				}
				unitString =  response.substring(unitStartIndex, 
						unitEndIndex);
				//( 1 . #s(unit player-id 0 type 0 loc (1 4) hp 60 r-amt 0 kills 0 status 1 status-args ()))
				StringTokenizer unitTokenizer = new StringTokenizer(unitString,
						spaceAndParens);
						
				unitID = Integer.parseInt(unitTokenizer.nextToken());
				unitTokenizer.nextToken(); // Eat the String "."
				unitTokenizer.nextToken(); // Eat the String "#s"
				unitTokenizer.nextToken(); // Eat the String "unit"
				unitTokenizer.nextToken(); // Eat the String "player-id"
				playerIDOfUnit = Integer.parseInt(unitTokenizer.nextToken());
				unitTokenizer.nextToken(); // Eat the String "type"
				type = Integer.parseInt(unitTokenizer.nextToken()); 
				unitTokenizer.nextToken(); // Eat the String "loc"
				locX = Integer.parseInt(unitTokenizer.nextToken()); 
				locY = Integer.parseInt(unitTokenizer.nextToken()); 
				unitTokenizer.nextToken(); // Eat the String "hp"
				hp = Integer.parseInt(unitTokenizer.nextToken());
				unitTokenizer.nextToken(); // Eat the String "mp"
				mp = Integer.parseInt(unitTokenizer.nextToken());
				unitTokenizer.nextToken(); // Eat the String "r-amt"
				resourceAmount = Integer.parseInt(unitTokenizer.nextToken()); 
				unitTokenizer.nextToken(); // Eat the String "kills"
				kills = Integer.parseInt(unitTokenizer.nextToken());
				unitTokenizer.nextToken().trim(); // Eat the String "last-attacked"
				lastAttacked = Integer.parseInt(unitTokenizer.nextToken());		
				unitTokenizer.nextToken(); // Eat the String "status"
				statusArgs[0] = Integer.parseInt(unitTokenizer.nextToken());
				unitTokenizer.nextToken().trim(); // Eat the String "status-args"
	
				for(int i = 1; i < 3;i++) {
					if (!unitTokenizer.hasMoreTokens()) break;
					try {
						statusArgs[i] = Integer.parseInt(unitTokenizer.nextToken());
					} catch (NumberFormatException e) {
						//No more status args, last unit, and trying to parse the global state (whoops)
					}
				}
	
				
				if (playerIDOfUnit == playerID[0]){ // Its our unit
					WargusUnit newUnit = new WargusUnit(unitID, type,
												  		hp, mp, locX, locY, resourceAmount,
												  		kills, lastAttacked, statusArgs, playerIDOfUnit); 
					myUnits.add(newUnit);
				} else if (playerIDOfUnit == 15) { // It's a friendly or neutral unit
					WargusUnit newUnit = new WargusUnit(unitID, type,
														hp, mp,locX, locY, resourceAmount,
														kills, lastAttacked, statusArgs, playerIDOfUnit); 
					friendOrNeutralUnits.add(newUnit);
				} else { // It's an enemy unit!
					WargusUnit newUnit = new WargusUnit(unitID, type,
														hp, mp, locX, locY, resourceAmount,
														kills, lastAttacked, statusArgs, playerIDOfUnit); 
					enemyUnits.add(newUnit);
				}
	
				if (haltUnitSearch == true) 
					break;
			} // End while (true)
			unlockState();
		} else {
			System.out.println("GetState: no connection...");
		}
	} // End void GetUnitStates(ClientSocket& inc_socketAccessToGame)

	/**
	 * Gets the state of the map and puts it into map.
	 * We initialize mapWidth/Length in constructor, stopped resetting it every
	 * time this was run. 
	 * @throws IOException 
	 */
	public void GetMapState() throws IOException {
		int rowIndex = 0;
		int columnIndex = 0;
		String response = "";
		String mapRow;
		String delims = "()\n";
		char[][] myMap = new char[mapWidth][mapLength];
		//getting the map
		try{
			response = sendAndReceiveMessage("LISPGET MAP\n", false);
			
			if (response!=null) {
	//			System.out.println(response);
				//tokenize the map response; each tile is a char
				lockState();			
				StringTokenizer tokenizer = new StringTokenizer(response, delims);			
				mapRow = tokenizer.nextToken(); // first row of map
				while (mapRow != null) {
	//				System.out.println(mapRow + " " + "Width: " + mapWidth);
					for (columnIndex = 0; columnIndex < mapWidth; columnIndex++)
						// Save all columns in this row to the map variable
						myMap[rowIndex][columnIndex] = mapRow.charAt(columnIndex);
					if (tokenizer.hasMoreTokens()) {
						mapRow = tokenizer.nextToken();
						rowIndex++; 
					} else {
						mapRow = null;
					}
				}
				map = myMap;
				unlockState();			
			} else {
				System.out.println("GetMapState: No connection...");
			}
		} catch (Exception e) {
			unlockState();
			releaseSockets();
		}
	} //End void GetMapState()

	/**
	 * returns character corresponding to that cell of the map
	 * @param row
	 * @param col
	 * @return either T (tree), B (Building), + (coast), ^ (sea), # (rock)
	 *         or ~ (behind fog of war)
	 */
	public char GetMapCell(int row, int col) {
		return map[row][col];
	}
	
	public int GetMapLength() {
		return mapLength;
	}

	public int GetMapWidth() {
		return mapWidth;
	}

	/**
	 * orders wargus to advance a certain number of cycles and return
	 * @param N
	 */
	public void AdvanceNCycles(int N) {
		sendAndReceiveMessage("TRANSITION " + N + "\n", true);
	}

	/**
	 * uses the same scenario as was loaded
	 */
	public void RestartScenario() {
		sendAndReceiveMessage("RESTART\n", true);
	}

	/**
	 * self-documenting
	 * @param newSeed
	 */
	public void SendRandomNumSeedToStratagus(long newSeed) {
		sendAndReceiveMessage("Z " + newSeed + "\n", true);
	}

	/**
	 * uses to switch scenarios
	 * @param newMapPath
	 */
	public void LoadMapAndRestartScenario(String newMapPath) {
		sendAndReceiveMessage("MAP " + newMapPath + "\n", true);
		reset();
	}

	/**
	 * orders a unit to move to an x, y location. ignore enemies along the way
	 * @param unitID
	 * @param x
	 * @param y
	 * @param relative if false, use absolute positioning
	 */
	public void move(int unitID, int x, int y,
			boolean relative) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		sendAndReceiveMessage("COMMAND " + unitID + " 2 " + x + " " + y + "\n", true);
		System.out.println("-----------------------MOVE:" + this.getCurrCycle());
	}

	/**
	 * orders unit to attack given other unit
	 * @param unitIDOfAttacker
	 * @param attackThisUnitID
	 */
	public void attack(int unitIDOfAttacker, int attackThisUnitID) {
		DEBUG(3, "Proxybot: ATTACK " + unitIDOfAttacker + " " + attackThisUnitID);
		String response = sendAndReceiveMessage("COMMAND " + unitIDOfAttacker + " 4 " + attackThisUnitID + "\n", true);
		DEBUG(3, "Proxybot: response is '" + response + "'");
		DEBUG(4,"-----------------------ATTACK" + this.getCurrCycle());		
	}

	/**
	 * orders unit to cease all current actions and wait at current location.
	 * Note that unit may still move and attack if it sees another unit or is
	 * attacked from outside sight distance.
	 * @param unitID
	 */
	public void stop(int unitID) {
		sendAndReceiveMessage("COMMAND " + unitID + " 1\n", true);
	}

	/**
	 * Orders unit to attack a certain patch of ground. Only valid for ranged
	 * units.
	 * @param unit
	 * @param x
	 * @param y
	 * @param relative if false, use absolute positioning
	 */
	public void attackGround(int unitID, int x, int y, boolean relative) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		DEBUG(3, "Proxybot: ATTACKGROUND " + unitID + " " + x + " " + y);		
		String response = sendAndReceiveMessage("COMMAND " + unitID + " 8 " + x + " " + y + "\n", true);
		DEBUG(3, "Proxybot: response is '" + response + "'");
		DEBUG(4, "-----------------------ATTACKGROUND" + this.getCurrCycle());
	}

	/**
	 * move to location, attacking any enemies encountered on the way.
	 * @param unit
	 * @param x
	 * @param y
	 * @param relative if false, use absolute positioning
	 */
	public void attackMove(int unitID, int x, int y, boolean relative) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		sendAndReceiveMessage("COMMAND " + unitID + " 17 " + x + " " + y + "\n", true);
	}

	/**
	 * unit and transport meet at nearest possible location and the unit
	 * enters the transport. Only valid for ground units
	 * @param unit
	 * @param transport
	 */
	public void boardTransport(int unitID, int transport) {
		sendAndReceiveMessage("COMMAND " + unitID + " 12 " + transport + " \n", true);
	}

	public void forwardstate(String filename)
	{
		String response = sendAndReceiveMessage("FORWARDSTATE " + filename +" \n", true);
		DEBUG(3, "Proxybot: response is '" + response + "'");
	}
	/** 
	 * constructs the given building at the given location, as long as there
	 * are no obstructions and there are enough resources. Note that a unit
	 * may move into the building footprint before construction and become
	 * and obstruction
	 * @param unit
	 * @param x
	 * @param y
	 * @param relative
	 * @param building
	 */
	public void build(int unitID, int x, int y, boolean relative,
			int building) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		DEBUG(3, "Proxybot: BUILD " + unitID + " " + x + " " + y + " " + building);
		String response = 
		sendAndReceiveMessage("COMMAND " + unitID + " 3 " + building + " " + x + " " + y
				+ " \n", true);	
		DEBUG(3, "Proxybot: response is '" + response + "'");
		if((building == 90)||(building == 91)||(building == 96)||(building == 97)
		    ||(building == 98)||(building == 99)||(building == 88)||(building == 89)||(building == 64)||
		    (building == 60)||(building == 82))
		DEBUG(4, "DEFENSE");
		DEBUG(4, "bldg" + building);
		DEBUG(4, "-----------------------BUILD" + this.getCurrCycle());
	}

	/**
	 * casts a spell. ABL should only send appropriate spells for appropriate
	 * units
	 * @param unit
	 * @param x
	 * @param y
	 * @param relative
	 * @param spell
	 * @param target only needed if specified
	 */
	public void castSpell(int unitID, int x, int y, boolean relative,
			int spell, int target) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		sendAndReceiveMessage("COMMAND " + unitID + " 16 " + x + " " + y + " " + spell 
				+ " " + target + " \n", true);	
		DEBUG(4, "-----------------------CASTSPELL" + this.getCurrCycle());
	}

	/**
	 * this is the same as demolishing
	 * TODO need to change SpellID from 0 to what demolish really is.
	 * @param unit
	 * @param x
	 * @param y
	 * @param relative
	 */
	public void detonate(int unitID, int x, int y, boolean relative) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		sendAndReceiveMessage("COMMAND " + unitID + " 16 " + x + " " + y 
				+ "0 -1 \n", true);
	}

	/**
	 * unit remains as close to leader as possible
	 * @param unit
	 * @param leader
	 */
	public void follow(int unitID, int leader) {
		sendAndReceiveMessage("COMMAND " + unitID + " 11 " + leader + " \n", true);
	}

	/**
	 * harvest whatever resource is at the specified location.
	 * @param unit
	 * @param x
	 * @param y
	 * @param relative if true, relative to the units location, else absolute
	 */
	public void harvest(int unitID, int x, int y, boolean relative) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		DEBUG(3, "Proxybot: RESOURCE " + unitID + " " + x + " " + y);
		String response = 
		sendAndReceiveMessage("COMMAND " + unitID + " 6 0 " + x + " " + y 
				+ " \n", true);
		DEBUG(3, "Proxybot: response is '" + response + "'");
		DEBUG(4, "-----------------------HARVEST"+ this.getCurrCycle());
	}
	
	/**
	 * pick a resource, harvest at closest available location
	 * @param unit
	 * @param type 1 == gold/oil, 2 == wood
	 */
	public void harvest(int unitID, int type) {
		if (type < 1 || type > 2) {
			System.err.println("Invalid Type of resource to harvest\nmust be 1 or 2\nvaluepassed: " + type);
		} else {
			DEBUG(3, "Proxybot: RESOURCE " + unitID + " " + type);
			String response = 
			sendAndReceiveMessage("COMMAND " + unitID + " 6 " + type + " \n", true);
			DEBUG(3, "Proxybot: response is '" + response + "'");
			DEBUG(4, "-----------------------HARVEST"+ this.getCurrCycle());
		}
	}
	
	/**
	 * carry collected resources to nearest depot
	 * Probably not used much, since units return automatically after harvest
	 * @param unit
	 */
	public void returnWithResource(int unitID) {
		sendAndReceiveMessage("COMMAND " + unitID + " 7\n", true);
	}

	/**
	 * continously move from current location to given location and back as if
	 * with the attackMove command
	 * @param unit
	 * @param x
	 * @param y
	 * @param relative
	 */
	public void patrol(int unitID, int x, int y,
			boolean relative) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		sendAndReceiveMessage("COMMAND " + unitID + " 9 " + x + " " + y + " \n", true);
		
	}

	/**
	 * repaits current unit given time and resources
	 * @param unit must be a peon or peasant
	 * @param building must be building, ballista/catapult, or naval unit
	 */
	public void repair(int unitID, int building) {
		sendAndReceiveMessage("COMMAND " + unitID + " 5 " + building + " \n", true);
		DEBUG(4, "-----------------------REPAIR"+ this.getCurrCycle());
	}

	/**
	 * begins researching given advance
	 * @param building
	 * @param advance
	 */
	public void research(int building, int advance) {
		sendAndReceiveMessage("COMMAND " + building + " 15 " + advance + " \n", true);	
		DEBUG(4, "-----------------------RESEARCH"+ this.getCurrCycle());
	}

	/**
	 * orders unit to not move. Will still attack from given position
	 * @param unit
	 */
	public void standGround(int unitID) {
		sendAndReceiveMessage("COMMAND " + unitID + " 10\n", true);	
		DEBUG(4, "-----------------------STANDGROUND"+ this.getCurrCycle());
	}

	/**
	 * Train new units at barrack/town hall/etc
	 * @param building
	 * @param unitType
	 */
	public void train(int building, int unitType) {
		DEBUG(3, "Proxybot: TRAIN " + building + " " + unitType);
		String response = 
		sendAndReceiveMessage("COMMAND " + building + " 3 " + unitType + " \n", true);	
		DEBUG(3, "Proxybot: response is '" + response + "'");
		if((unitType == 4)||(unitType == 6)||(unitType == 8))
		DEBUG(4, "ATTACK");
		DEBUG(4, "-----------------------TRAIN"+ this.getCurrCycle());
	}

	/**
	 * unload unit from transport at given location. Does not unload all units
	 * from transport. Units will be unloaded in order of this command being
	 * given, not in the optimal speed ordering
	 * @param unitID
	 * @param transport
	 * @param x
	 * @param y
	 * @param relative
	 */
	public void unloadTransport(int unitID, int transport, int x, int y,
			boolean relative) {
		if (relative) {
			WargusUnit unit = findUnitByID(unitID, myUnits);
			x =  (x + unit.getLocX());
			y =  (y + unit.getLocY());
		}
		sendAndReceiveMessage("COMMAND " + unitID + " 13 " + transport + " " + x + " " 
				+ y + " \n", true);	
	}

	/**
	 * upgrade to advanced building. Only for town centers and towers
	 * @param building
	 * @param advance
	 */
	public void upgrade(int building, int advance) {
		if (88 == advance) {//adjusting for UnitTypes
			advance = 58;
		} else if (90 == advance) {
			advance = 59;
		} else if (89 == advance) {
			advance = 103;
		} else if (91 == advance) {
			advance = 104;
		} else if (97 == advance) {
			advance = 106;
		} else if (99 == advance) {
			advance = 107;
		} else if (96 == advance) {
			advance = 61;
		} else if (98 == advance) {
			advance = 62;
		}
		String message = "COMMAND " + building + " 14 " + advance + " \n";
		sendAndReceiveMessage(message, true);
		System.out.println("-----------------------UPGRADE"+ this.getCurrCycle());
	}

	/**
	 * checks that the returned value has all data
	 * @param stringToCheck
	 * @return if the returned string is complete
	 */
	public boolean _ParenMatch(String stringToCheck) {
		int openBraceCount = 0;
		int closeBraceCount = 0;

		// Go through the entire String and count open and close braces
		for (int i = 0; i < stringToCheck.length(); i++) {
			char c = stringToCheck.charAt(i);
			if (c == '(')
				openBraceCount++;
			else if (c == ')')
				closeBraceCount++;
		}

		return openBraceCount == closeBraceCount; // See if the counts agree
	}

	public WargusUnit findUnitByID(int id, ArrayList list){
		for(int i = 0; i < list.size(); i++) {
			if((((WargusUnit)(list.get(i))).getUnitID()) == id) {
				return (WargusUnit) list.get(i);
			}		
		}
		return null;
	}

	
	public int getNumOfPlayers() {
		return numOfPlayers;
	}

	
	/**
	 * returns the upgrades that have been researched
	 * need the creator due to a issue with the array being 
	 * found null on occaision
	 * @return the advances that have been researched (or not)
	 */
	public boolean[] getAdvances() {
		return advances;
	}
	

	/**
	 * stops the game remotely
	 */
	public void KillStratagus() {
		sendAndReceiveMessage("KILL\n", true);
	}

	/**
	 * stops playing but leaves the game running
	 */
	public void Quit() {
		try {
			sendMsg("QUIT\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * used to atomically send and recieve messages, used to prevent 
	 * deadlock
	 * @param msg
	 * @param endlineCheck
	 * @return the response from the server
	 */
	public synchronized String sendAndReceiveMessage(String msg, 
			boolean endlineCheck) {
		String resp = null;
		
		DEBUG(1, "base: sendAndReceiveMessage: sending '" + msg + "'");
		
		if (!m_connected) {
			connect();
			if (!m_connected) return null;
		}
		
		try {
			lockSend();
			sendMsg(msg);
			
			// it is the stratagus stdout printing ....
			//  or who knows what...
			
			// & then i need to check system performance w/o cbr saving... or is it retaining?
			
			//Thread.sleep(10); // XXX test delay - remove me
			//System.err.print("- sent " + msg.trim());

			// XXX this should be able to be converted to true in all cases with no bad side-effects
			resp = recvMessage(endlineCheck);
			
			/*
			String s = resp.trim();
			if (s.length() > 100) s = s.substring(0, 97) + "...";
			System.err.println(" - got " + s);
			*/
			
			unlockSend();
			
			if (resp.equals("Ignored\n")) {
				DEBUG(1, "\n -------------------------------\n\n IGNORED COMMAND!!!!! \n\n -------------------------------\n");
				ignoredMessages.add(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				releaseSockets();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			resp = null;
		}
		
		DEBUG(1, "base: sendAndReceiveMessage: received '" + resp + "'");
		
		return resp;
	}
	
	/**
	 * sends the given msg across the wire to Wargus
	 * @param msg the command to be sent
	 * @throws IOException 
	 */
	private void sendMsg(String msg) throws IOException {
		try {
			last_message_sent = msg;
			mySocketOut.write(msg.getBytes());
			mySocketOut.flush();
		} catch (Exception e) {
			e.printStackTrace();
			releaseSockets();
		}
	}

	/** replacement for mySocket.receive() the original receive()
	 * returned bool -- we will just return the string itself on
	 * success and return 'null' on failure
	 * @param endlineCheck - checks for /n??
	 * @return the string received
	 * @throws IOException 
	 */
	private String recvMessage(boolean endlineCheck) throws IOException {
		String response = "", partialResponse;
		int numBytesRead;
		byte[] recvBuf = new byte[4096];
		long retries = 0, max_retries = 5;
		long start_time = System.currentTimeMillis();
		long maximum_wait = 10000;	// 10 seconds
		try {
			// Get the first part of the response (this may be the entire response)
			while(mySocketIn!=null && mySocketIn.available()==0) {
				if ((System.currentTimeMillis()-start_time) > maximum_wait) {
					System.out.println("recvMessage(1): More than " + (maximum_wait/1000) + " seconds waiting for the rest of the message from Stratagus.");
					System.out.println("recvMessage(1): The recieved message till now was: '" + response + "'");
					System.out.println("recvMessage(1): The last message sent to Stratagus was: '" + last_message_sent + "'");
					retries++;
					
					if (m_connected  && retries<=max_retries) {
/*
						mySocket.close();
						mySocket = new Socket();
						mySocket.setTcpNoDelay(true);
						mySocket.setSoLinger(true, 100);
						mySocket.connect(new InetSocketAddress(m_serverAddress, 4870));		
						mySocketIn = new BufferedInputStream(mySocket.getInputStream());
						mySocketOut = new BufferedOutputStream(mySocket.getOutputStream());						
*/
						System.out.println("recvMessage(1): Retrying...");			
						sendMsg(last_message_sent);
						start_time = System.currentTimeMillis();
//					} else {
//						throw new Exception("recvMessage(1): waited too long!");
					} // if
				}
				Thread.sleep(10);
			}			
			
			if (mySocketIn!=null) numBytesRead = mySocketIn.read(recvBuf);
							 else numBytesRead = 0; 

			partialResponse = (numBytesRead > 0) ? new String(recvBuf, 0,
					numBytesRead) : "";
			
			while (mySocketIn!=null) {// Make sure we have the complete response
				response = response + partialResponse;
					
				// what type of "incomplete response"-check should I do?
				boolean responseIsComplete = endlineCheck ? response.endsWith("\n")
						: _ParenMatch(response);

				// If the response is size 0, or we deem the response is 
				// incomplete, keep trying to get the entire packet
				
				if ((response.length() == 0) || (responseIsComplete == false)) {
					while(mySocketIn!=null && mySocketIn.available()==0) {
						if ((System.currentTimeMillis()-start_time) > maximum_wait) {
							System.out.println("recvMessage(2): More than " + (maximum_wait/1000) + " seconds waiting for the rest of the message from Stratagus.");
							System.out.println("recvMessage(2): The recieved message till now was: '" + response + "'");
							throw new Exception("recvMessage(2): waited too long!");
						}
						Thread.sleep(10);
					}
					numBytesRead = mySocketIn.read(recvBuf);
					partialResponse = (numBytesRead > 0) ? new String(recvBuf,
							0, numBytesRead) : "";
				} else
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			releaseSockets();
			response = "Ignored";
		}
		return response;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public ArrayList<WargusUnit> getEnemyUnits() {
		return enemyUnits;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public ArrayList<WargusUnit> getFriendOrNeutralUnits() {
		return friendOrNeutralUnits;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public ArrayList<WargusUnit> getMyUnits() {
		return myUnits;
	}

	/**
	 * just a getter
	 * @return value
	 */
	public static ProxyBot getProxyBot() {
		if (null == proxy) {
			System.out.println("!!!!!!!!!!WTF!!!!!!!!!!!!!!");
		}
		return proxy;
	}

	public long getCurrCycle(){
		return currCycle;
	}
	
	public long getCurrGameCycle() {
		String response = sendAndReceiveMessage("X-GAME-CYCLE\n", true);
		if (response==null) return 0;
		return Long.parseLong(response.trim());
	}
	public long getCurrentScore(int EnemyType)
	{
		String score =  sendAndReceiveMessage("SCORE " + EnemyType + "\n", true);
		if (score==null) return 0;
		return Long.parseLong(score.trim());
		
		
	}
	public void pause() {
		m_paused = true;
	}
	
	public void resume() {
		m_paused = false;
	}
	
	public boolean paused() {
		return m_paused;
	}
	
	public boolean connected() {
		return m_connected && m_reseted;
	}
	
	public void releaseSockets() throws IOException {
		if (mySocketIn!=null) mySocketIn.close();
		if (mySocketOut!=null) mySocketOut.close();
		if (mySocket!=null) mySocket.close();
		mySocketIn = null;
		mySocketOut = null;
		mySocket = null;
		m_connected = false;
		
		System.out.println("releaseSockets: sockets closed!");
	}
	
	public void sendAnnotationFlag(boolean flag) {
		String s = flag?"1":"0";
		this.sendAndReceiveMessage("ANNOTATION "+s+"\n", true);
	}
}
