/*
 * Daniel Phang
 * Sui Ying Teoh
 * CSE348 Project 4
 * Main class for controlling Wargus AI
 */

package example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Scanner;

import base.ProxyBot;

public class Example
{
	static boolean m_static_stopSignal = false;
	static ProxyBot m_pb;
	static boolean MRE_PROXYBOT_SYNCHRONOUS = false;
	
	static BuildManager bm;
	static ResourceManager rm;
	static UnitManager um;
	
	public static void main(String []args) throws Exception {
		Properties configuration = new Properties();
		String configuration_filename = "default.cfg";
		
		// Get the configuration file:
		FileInputStream f;
		if (args.length >= 1) {
			configuration_filename = args[0];
		}
			
		System.out.println("Using configuration file: " + configuration_filename);
			
		f = new FileInputStream(configuration_filename);
		configuration.load(f);
		f.close();
			
		m_static_stopSignal = false;
		// Start stratagus if configured
		String stratagusPath = configuration.getProperty("MAIN_stratagus_path");
		String stratagusExec = configuration.getProperty("MAIN_stratagus_exec");
		String wargusMap = configuration.getProperty("MAIN_initial_map");
		String killCmd = configuration.getProperty("MAIN_kill_stratagus");
		if (stratagusExec != null && wargusMap != null && stratagusPath != null) {
			Process p = Runtime.getRuntime().exec(stratagusPath + stratagusExec + " "
					+ wargusMap, null, new File(stratagusPath));
			
			class ProcessOutput extends Thread {
				Process m_p;
				PrintStream m_out;
				public ProcessOutput(Process p, PrintStream out) { m_p = p; m_out = out; }
				public void run() {
					Scanner s = new Scanner(m_p.getInputStream());
					m_out.println("outputting process output:");
					while (!m_static_stopSignal) {
						if (s.hasNext())
							m_out.println(s.nextLine());
						else
							try {
								Thread.sleep(100);
							} catch (Exception e) {} 
					}
				}
			}
			
			ProcessOutput out = new ProcessOutput(p, new PrintStream("stratagus.stdout"));
			// XXX this is a really strange fix to my and maybe others Stratagus network problems
			//     which keeps Stratagus from not responding to LISPGET GAMEINFO after a restart..
			//     i think the root of the problem is a timing issue that could be tricky to nail
			//     down but this seems to alleviate it  --Andrew
			new Thread(out).start();
			
			
			class KillProcess extends Thread {
				Process m_p;
				String m_killCmd;
				public KillProcess(Process p, String killCmd) {	
					m_p = p; 
					m_killCmd = killCmd;
				}
				public void run() {				
					if (m_p != null) {
						m_p.destroy(); // try to be nice
						// now get mean
						try {
							Runtime.getRuntime().exec(m_killCmd);
						} catch (IOException ioe) {}
					}
				}
			}
			Runtime.getRuntime().addShutdownHook(new KillProcess(p, killCmd));
			
			System.out.println("Sleeping to give time for Stratagus to load...");
			Thread.sleep(2500);
		}

    	m_pb = new ProxyBot(configuration);

		System.out.println("MAIN: Creating the Wargus CBR engine...");
				
		connectProxyBot();
		m_pb.reset();
		loadMap();
		m_pb.sendAndReceiveMessage("SPEED " + 2000 + "\n", false);
		m_pb.resume();
		
		bm = new BuildManager(m_pb);
		rm = new ResourceManager(m_pb);
		um = new UnitManager(m_pb);

		while(!m_static_stopSignal) {
			runTest(m_pb);
		}
		
		System.out.println("MAIN: Stopping the Wargus CBR engine...");
		System.out.println("Threads: " + Thread.activeCount());
		m_static_stopSignal = true;
		m_pb.stop();
		m_pb.Quit();
		Runtime.getRuntime().exec(killCmd);
		
		System.out.println("MAIN: Wargus CBR engine stopped.");
		
		System.out.println("Threads: " + Thread.activeCount());
		
	}
	
	static void runTest(ProxyBot pb){
		try {
			
			// Get current map and unit states
			pb.GetMapState();
			pb.GetState();
			
			// Update managers with new data
			rm.update(pb);
			bm.update(pb);
			um.update(pb);
			
			// Check for completion of previous buildings and for new units
			bm.checkBuildComplete();
			um.checkUnits();
			
			// Set actions for each manager
			bm.setBuild();
			bm.setTrain();
			bm.setResearch();
			um.setGroups();
			
			// Execute actions for each manager
			rm.doGather(); // First, assign any idle workers to gather resources
			bm.doRepair(); // Repair if need to
			bm.doResearch(); // Do research first if possible
			bm.doUpgrade(); // Do upgrade if possible
			bm.doTrain(); // Training is done first because it immediately uses up resources
			bm.doBuild();
			um.doAttack();
			um.doDefend();
		
			System.out.println("Advancing cycles...");
			pb.cycle();	
			Thread.sleep(1000);
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	static void connectProxyBot() {
		if (!MRE_PROXYBOT_SYNCHRONOUS)  new Thread(m_pb).start();
		while (!m_pb.connected()) {
			if (MRE_PROXYBOT_SYNCHRONOUS) m_pb.cycle();
//			DEBUG(1, "MRE: still not connected to stratagus.");
//			DEBUG(1, "MRE: retrying in 5 seconds...");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		if (!MRE_PROXYBOT_SYNCHRONOUS) m_pb.pause();
	}
	
	static void loadMap() {
		String map = "maps/GoW-small2.pud.gz";
//		DEBUG(1, "MRE: map selected: " + map);
//		m_logger.println("MRE: map selected: " + map);
//		m_experimentsLogger.println("MRE: map selected: " + map);
		m_pb.LoadMapAndRestartScenario(map);   
	}


}
