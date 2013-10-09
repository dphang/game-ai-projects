/*
 * Daniel Phang
 * Sui Ying Teoh
 * CSE348 Project 4
 * This class manages the control of military units and orders to attack etc.
 */

package example;

import java.awt.Point;
import java.util.List;
import java.util.Vector;

import example.UnitGroup.Priority;

import base.ProxyBot;
import base.WargusPlayer;
import base.WargusStateImporter;
import base.WargusUnit;

public class UnitManager {
	
	private ProxyBot pb;
	private WargusPlayer wp;
	private WargusPlayer opponent;
	
	private Vector<UnitGroup<Integer>> unitGroups = new Vector<UnitGroup<Integer>>();
	public UnitManager(ProxyBot pb) {
		this.pb = pb;
		this.wp = WargusStateImporter.getGameStatePlayer(pb);
		if (!WargusStateImporter.getGameStateOpponents(pb).isEmpty()) // Check that we have opponents
			this.opponent = WargusStateImporter.getGameStateOpponents(pb).get(0);
		
		unitGroups.add(new UnitGroup<Integer>());
		unitGroups.add(new UnitGroup<Integer>());
		unitGroups.add(new UnitGroup<Integer>());
		unitGroups.add(new UnitGroup<Integer>());
		
		unitGroups.get(0).setPriority(Priority.ATTACK); // Attack group
		unitGroups.get(1).setPriority(Priority.DEFEND); // Defend group
		unitGroups.get(2).setPriority(Priority.DEFEND); // Defend group
		unitGroups.get(3).setPriority(Priority.ATTACK); // Attack group
	}
	
	public void update(ProxyBot pb) {
		this.pb = pb;
		this.wp = WargusStateImporter.getGameStatePlayer(pb);
		if (!WargusStateImporter.getGameStateOpponents(pb).isEmpty())
				this.opponent = WargusStateImporter.getGameStateOpponents(pb).get(0);
	}
	
	public void checkUnits() {
		Vector<WargusUnit> units = wp.getUnits();
		boolean unactive = true;
		for (int u : unitGroups.get(0)) {
			for (WargusUnit w : units) {
				String status = WargusStateImporter.statusToString(w.getStatus()[0]);
				if (status != "idle" && w.getUnitID() == u)
				{
					unactive = false;
				}
			}
		}
		for (WargusUnit w: units) {
			if (w.IsDead()) {
				unitGroups.get(0).remove(w.getUnitID());
				unitGroups.get(0).remove(w.getUnitID());
			}
		}
		if (unactive)
			unitGroups.get(0).setActive(false);
	}
	
	/*
	 * Organizes units into different groups (emulates control groups of RTS games).
	 */
	public void setGroups() {
		Vector<WargusUnit> units = wp.getUnits();
		for (WargusUnit u : units) {
			String name = WargusStateImporter.unitTypeToString(u.getType());
			boolean inGroup = false;
			for (UnitGroup<Integer> unitGroup : unitGroups) {
				if (unitGroup.contains(u.getUnitID()))
					inGroup = true;
			}
			if (!inGroup && u.isUnit() && name != "peasant") {
				double random = Math.random();
				if (random > 0.2) { // Probability of getting put in a certain group
					unitGroups.get(0).add(u.getUnitID());
				}
				else
					unitGroups.get(1).add(u.getUnitID());
			}
		}
	}

	/*
	 * Do defending group actions (patrolling mostly)
	 */
	public void doDefend() {
		for (UnitGroup<Integer> u : unitGroups) {
			if (u.getPriority() == Priority.DEFEND) {
				for (int id : u) {
					Point movePoint = getPossiblePoint(wp);
					pb.attackMove(id, movePoint.x, movePoint.y, false);
				}
			}
		}
	}
	
	/*
	 * Do attacking group actions (depends on size)
	 */
	public void doAttack() {
		for (UnitGroup<Integer> u : unitGroups) {
			if (u.getPriority() == Priority.ATTACK) { // isActive allows us to have a cohesive group action
				if (u.size() >= 24){ // Once it is a big enough group
					Point movePoint = getPossiblePoint(opponent);
					for (int id : u) {
						pb.attackMove(id, movePoint.x, movePoint.y, false);
					}
					u.setActive(true);
				}
				else {
					for (int id : u) {
						Point movePoint = getPossiblePoint(wp);
						pb.attackMove(id, movePoint.x, movePoint.y, false);
					}
				}
			}
		}
	}

	/*
	 * Finds location of our units or opponents units so our army can attack move towards it
	 */
	private Point getPossiblePoint(WargusPlayer wp) {
		List<WargusUnit> units = wp.getUnits();
		Point p = null;
		for (WargusUnit u : units) {
			int x = u.getLocX();
			int y = u.getLocY();
			p = new Point(x, y);
			for (WargusUnit w : opponent.getUnits()) {
				if (w.isUnit() && p.distance(w.getLocX(), w.getLocY()) < 10)
						return p;
			}
			Point previous = unitGroups.get(0).getPreviousMovePoint();
			if (previous != null && p.distance(previous) > 15) // Make patrol distance further
				return p;
		}
		return p;
	}
}
