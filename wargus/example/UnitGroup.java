 /*
  * Daniel Phang
  * Sui Ying Teoh
  * CSE348 Project 4
  * This class is contains the IDs of units (simulates a control group in RTS games)
  */
 

package example;
import java.awt.Point;
import java.util.Vector;

@SuppressWarnings("hiding")
public class UnitGroup<Integer> extends Vector<Integer> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8031037349920246329L;
	private static final int CAPACITY = 200;

	private boolean isActive = false; // Does it have an order?
	private Priority priority;
	
	// Previous move point so we don't move back to same place
	private Point previousMovePoint;
	
	public enum Priority {
		ATTACK, DEFEND, EXPLORE;
	}
	
	public UnitGroup() {
		super();
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	/*
	 * Is the capacity reached? (15 units per group)
	 */
	public boolean isFull() {
		return super.size() == CAPACITY;
	}
	
	@Override
	public boolean add(Integer e){
		if (isFull())
			return false;
		return super.add(e);
	}

	/*
	 * What the group is supposed to be doing
	 */
	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public Point getPreviousMovePoint() {
		return previousMovePoint;
	}

	public void setPreviousMovePoint(Point previousMovePoint) {
		this.previousMovePoint = previousMovePoint;
	}
	
	
}
