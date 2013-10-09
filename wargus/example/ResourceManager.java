/*
 * Daniel Phang
 * Sui Ying Teoh
 * CSE348 Project 4
 * This class manages resource gathering by worker units.
 */

package example;

import java.util.List;

import base.ProxyBot;
import base.WargusPlayer;
import base.WargusStateImporter;
import base.WargusUnit;

public class ResourceManager {
	
	private ProxyBot pb;
	private WargusPlayer wp;
	
	public ResourceManager(ProxyBot pb) {
		this.pb = pb;
		this.wp = WargusStateImporter.getGameStatePlayer(pb);
	}
	
	/*
	 * Updates the manager with new data
	 */
	public void update(ProxyBot pb) {
		this.pb = pb;
		this.wp = WargusStateImporter.getGameStatePlayer(pb);
	}
	
	/*
	 * Orders units to gather resources
	 */
	public void doGather() {
		List<WargusUnit> workers = wp.getUnitsByType("peasant");
		for (WargusUnit u: workers) {
			String status = WargusStateImporter.statusToString(u.getStatus()[0]);
			if (status == "idle") {
				if (2 * pb.getGold() < pb.getWood()) {
					pb.harvest(u.getUnitID(), 1);
				}
				else
				{
					pb.harvest(u.getUnitID(), 2);
				}
			}
		}
	}
}
