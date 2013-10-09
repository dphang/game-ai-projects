package base;
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;

import org.jdom.Element;


public class WargusMap implements Serializable {
  private int m_width, m_height;
  private char [][] m_map;
  private List<WargusUnit> m_gold_mines;
  private List<WargusUnit> m_oil_patches;

  public WargusMap(int w, int h) {
    m_width = w;
    m_height = h;
    m_map = new char[m_width][m_height];

    for (int y = 0; y < m_height; y++) {
      for (int x = 0; x < m_width; x++) {
	m_map[x][y] = '.';
      }
    }

    m_gold_mines = new LinkedList<WargusUnit>();
    m_oil_patches = new LinkedList<WargusUnit>();
  }

  public WargusMap(WargusMap m) {
    m_width = m.get_width();
    m_height = m.get_height();
    m_map = new char[m_width][m_height];

    for (int y = 0; y < m_height; y++) {
      for (int x = 0; x < m_width; x++) {
	m_map[x][y] = m.get_map()[x][y];
      }
    }

    m_gold_mines = new LinkedList<WargusUnit>();
    for(WargusUnit g:m.get_goldMines()) m_gold_mines.add(new WargusUnit(g));

    m_oil_patches = new LinkedList<WargusUnit>();
    for(WargusUnit o:m.get_oilPatches()) m_oil_patches.add(new WargusUnit(o));
  }

  public int getWaterArea() {
    return getArea(WargusStateImporter.TERRAIN_WATER);
  }
  public int getTreeArea() {
    return getArea(WargusStateImporter.TERRAIN_TREE);
  }
  public int getLandArea() {
    return m_width*m_height - getWaterArea();
  }
  private int getArea(char type) {
    int area = 0;
    for (int y = 0; y < m_height; y++) {
      for (int x = 0; x < m_width; x++) {
	if (m_map[x][y] == type) area++;
      }
    }
    return area;
  }
  public boolean isAreaTree(int x, int y) {
	    int area = 0;
	    if( x >= 0 && y >= 0)
	    {	
	    	if (m_map[x][y] == WargusStateImporter.TERRAIN_TREE) return true;
	    	else 
		 		return false;
	    }
	    else 
	 		return false;
	  }
  public int getFirstAreaTree(int y) {
	    int area = 0;
	    if(y <= m_height)
	    {	
		    for (int x = 0; x < m_width; x++) {
		    	
		 	if (m_map[x][y] == WargusStateImporter.TERRAIN_TREE) return x;
		    }
	    }    
	    return -1;
  }
  public boolean isAreaGold(int x, int y) {
	    int area = 0;
	 	if (m_map[x][y] == WargusStateImporter.TERRAIN_GOLD) return true;
	 	else 
	 		return false;
	  }
  
  public boolean isAreaBuilding(int x, int y) {
	    int area = 0;
	 	if (m_map[x][y] == WargusStateImporter.TERRAIN_BUILDING) return true;
	 	else 
	 		return false;
	  }
  
  /*
  public void writeXML(XMLWriter w) {
    w.tag("map");
    {
      w.tag("size-x", m_width);
      w.tag("size-y", m_height);
      w.tag("land-area", getLandArea());
      w.tag("water-area", getWaterArea());
      w.tag("tree-area", getTreeArea());

      w.tag("gold-mines");
      {
	for(WargusUnit gm:m_gold_mines) gm.writeXML(w);
      }
      w.tag("/gold-mines");

      w.tag("oil-fields");
      {
	for(WargusUnit op:m_oil_patches) op.writeXML(w);
      }
      w.tag("/oil-fields");

      w.tag("terrain");
      {
	for (int y = 0; y < m_height; y++) {
	  String row = "";
	  for (int x = 0; x < m_width; x++) {
	    row += m_map[x][y];
	  }
	  w.tag("row", row);
	}
      }
      w.tag("/terrain");
    }
    w.tag("/map");
  }
*/
  
  public static WargusMap loadFromXML(Element e) {
    assert e.getName().equals("map") :
      "WargusMap.loadFromXML: Invalid XML Element " + e.getName();

    int width = Integer.parseInt(e.getChildText("size-x"));
    int height = Integer.parseInt(e.getChildText("size-y"));

    WargusMap map = new WargusMap(width, height);

    List goldElements = e.getChild("gold-mines").getChildren("unit");
    for (Object o : goldElements) {
      Element goldElement = (Element) o;
      map.add_goldMine(WargusUnit.loadFromXML(goldElement));
    }

    List oilElements = e.getChild("oil-fields").getChildren("unit");
    for (Object o : oilElements) {
      Element oilElement = (Element) o;
      map.add_oilPatch(WargusUnit.loadFromXML(oilElement));
    }

    int y = 0;
    for (Object o : e.getChild("terrain").getChildren("row")) {
      Element terrainRow = (Element) o;
      String row = terrainRow.getText();

      assert row.length() == width :
	"WargusMap.loadFromXML: Invalid terrain row size! " 
	+ terrainRow.getText().length();
      assert y < height : "WargusMap.loadFromXML: Too many terrain rows!";

      for (int x = 0; x < width; ++x) {
	map.set_map(x, y, row.charAt(x));
      }
      ++y;
    }

    for(WargusUnit gm : map.get_goldMines()) {
      for (int i = 0; i < 3; ++i) {
	for (int j = 0; j < 3; ++j) {
	  map.set_map(gm.getLocX() + i, gm.getLocY() + j, 
	      WargusStateImporter.TERRAIN_GOLD);
	}
      }
    }
    return map;
  }

  public int getTreeRow(char type, int maxcount)
  {
	  Vector<Integer> treecount = new Vector<Integer>();
	  for (int y = 0; y < m_height; y++) {
		  int count = 0;
	      for (int x = 0; x < m_width; x++) {
	    	  if (m_map[x][y] == type) count++;
	    }
    	if(count>=maxcount)
    		  return y;
	  }   
	  return -1;
  }
  
  public int getTreeCount(int y)
  {
	  int count = 0;
	  for (int x = 0; x < m_width; x++) {
	    	  if (m_map[x][y] == WargusStateImporter.TERRAIN_TREE) count++;
	    }
    return count;
  }
  public String toStringTerrain() {
    String terrain = "";

    for (int y = 0; y < m_height; y++) {
      String row = "";
      for (int x = 0; x < m_width; x++) {
	row += m_map[x][y];
      }
      terrain += row + "\n";
    }		
    return terrain;
  }

  /**
   * Get a tile from the map
   * @param x coord
   * @param y coord
   * @return the tile at the given x,y coordinate.  Returns 
   * WargusStateImporter.TERRAIN_EDGE if the coordinate is outside the bounds
   * of the map. 
   */
  public char get_map(int x,int y) {
    if (x < 0 || y < 0 || x >= m_width || y >= m_height) 
      return WargusStateImporter.TERRAIN_EDGE;
    else
      return m_map[x][y];
  }
  /**
   * @return the width
   */
  public int get_width() {
    return m_width;
  }

  /**
   * @return the height
   */
  public int get_height() {
    return m_height;
  }

  /**
   * @return the map
   */
  public char[][] get_map() {
    return m_map;
  }

  public void set_map(int x, int y, char val) {
    m_map[x][y] = val;
  }

  public void add_goldMine(WargusUnit gm) {
    m_gold_mines.add(gm);
  }

  public void add_oilPatch(WargusUnit op) {
    m_oil_patches.add(op);
  }

  public List<WargusUnit> get_goldMines() {
    return m_gold_mines;
  }

  public List<WargusUnit> get_oilPatches() {
    return m_oil_patches;
  }

  /**
   * Path helpers which return distance of the shortest path between a and b
   * or a negative number if no path exists.
   * 
   * @param a
   * @param b
   * @return distance of path or negative number if no path
   */
  public double isWaterPath(Point a, Point b) {
    return isPath(a, b, new Character[]{WargusStateImporter.TERRAIN_WATER, 
	WargusStateImporter.TERRAIN_OIL});
  }
  // Use for land units & buildings
  public double isLandPath(Point a, Point b) {
    return isPath(a, b, new Character[]{WargusStateImporter.TERRAIN_GRASS,WargusStateImporter.TERRAIN_GRASS_NOBUILD,WargusStateImporter.TERRAIN_BUILDING,WargusStateImporter.TERRAIN_BUILDINGOPP,WargusStateImporter.TERRAIN_GOLD,
	WargusStateImporter.TERRAIN_UNIT, WargusStateImporter.TERRAIN_UNITOPP}); 
  } // needs to be changed to valid path tiles...
  // what about for land units attacking water units
  // air units attacking anything...
  // ... other cases?


  /**
   * Does an A* search to find the length of the shortest path.
   * 
   * @param start point 
   * @param end point
   * @param map_copy
   * @return length of shortest path
   */
  private double pathDistance(Point start, final Point end, char[][] map_copy) {
    /**
     * DistancePoint helper class for A* search.
     */
    class DistancePoint extends Point implements Comparable {
      double f, g, h;

      public DistancePoint(Point p, double a_g) {
	super(p);
	g = a_g;
	h = end.distance(x, y);
	f = g + h;			
      }
      public boolean isEnd() {
	return (end.x == x && end.y == y);
      }
      // This should return correctly the lowest first
      public int compareTo(Object o) {
	double of = ((DistancePoint) o).f;
	if (f < of) {
	  return -1;
	} else if (f > of) {
	  return 1;
	} else {
	  return 0;
	}
      }
      public void updateG(double d) {
	if (d < g) {
	  g = d;
	  f = g + h;
	}				
      }
      public boolean equals(Object obj) {
	if (obj instanceof DistancePoint) {
	  return ((DistancePoint) obj).x == x &&
	  ((DistancePoint) obj).y == y;
	} else {
	  return false;
	}
      }
      public int hashCode() {
	return ("" + x + "" + y).hashCode();
      }
      public String toString() {
	return x + "," + y + " w/ f=" + f + " g=" + g + " h=" + h;
      }
    }

    ArrayList<DistancePoint> points = new ArrayList<DistancePoint>();
    HashSet<Point> visited = new HashSet<Point>();
    Queue<DistancePoint> q = new PriorityQueue<DistancePoint>();
    q.add(new DistancePoint(start, 0.0));

    DistancePoint p;
    while (!q.isEmpty()) {
      p = q.remove();
      visited.add(p);
      if (p.isEnd()) {
	return p.f;
      }

      for (int x = -1; x <= 1; ++x) {
	for (int y = -1; y <= 1; ++y) {
	  // don't search same one again
	  if (x == 0 && y == 0) continue;

	  int adjx, adjy;
	  adjx = p.x + x;
	  adjy = p.y + y;
	  // check that the point is in the map
	  if (0 > adjx || adjx >= m_width || 0 > adjy || adjy >= m_height) {
	    continue;
	  }

	  // don't search obstacle tiles
	  if (map_copy[p.x][p.y] != map_copy[adjx][adjy]) continue;

	  // update memoized DistancePoint with the path we found
	  double g = p.g + p.distance(adjx, adjy);
	  DistancePoint successor = new DistancePoint(new Point(adjx, adjy), g);
	  int idx = points.indexOf(successor);
	  // attempt to update g for the successor
	  if (idx >= 0) {
	    successor = points.get(idx);
	    successor.updateG(g);
	  }

	  if (!visited.contains(successor) && !q.contains(successor)) {
	    q.add(successor);
	  }
	}
      }
    }
    assert false : "WargusMap.pathDistance couldn't find a path!";
    return -1.0;
  }

  /**
   * Determines whether there is a path from a to b by the given tiles and
   * returns the distance of the shortest path.
   * 
   * @param a point on map 
   * @param b point on map
   * @param tiles the tile types to search for a path .................
   * @return length of shortest path from a to b
   */
  private double isPath(Point a, Point b, Character[] tiles) {
    assert a.x >= 0 && a.y >= 0 && a.x < m_width && a.y < m_height :
      "WargusMap.isPath Point a out of bounds!";
    assert b.x >= 0 && b.y >= 0 && b.x < m_width && b.y < m_height :
      "WargusMap.isPath Point b out of bounds!";

    char[][] map_copy = new char[m_width][m_height];
    for (int y = 0; y < m_height; ++y) {
      for (int x = 0; x < m_width; ++x) {
	map_copy[x][y] = m_map[x][y];
      }
    }
    Stack<Point> point = new Stack<Point>();
    point.add(a);
    //point.add(b); // only need to flood fill from one
    char color = 0;
    flood_fill_points(map_copy, tiles, m_width, m_height, color, point);

    if(map_copy == null)
      System.out.println("map_copy is NULL");

    if (map_copy[a.x][a.y] == map_copy[b.x][b.y]) {
      return pathDistance(a, b, map_copy);
    } else {
      return -1.0;
    }
  }

  /**
   * Flood fill the given tiles starting from the given points.
   * 
   * @param sample to flood, will be modified
   * @param tiles to flood
   * @param w width of sample
   * @param h height of sample
   * @param color to use for filling
   * @param tolook coordinates to fill/search from
   * @param border tile character to look for
   */
  private void flood_fill_points(char[][] map_copy, Character[] tiles, int w, int h, 
      char color, Stack<Point> tolook) {
    while (!tolook.isEmpty()) {
      Point p = tolook.pop();
      int x = p.x, y = p.y;

      if (!(x > -1 && x < w && y > -1 && y < h)) {
	// out of map bounds
	continue;
      }
      LinkedList<Character> m_tile = new LinkedList<Character>(); //Added by Kinshuk
      for(int i=0;i<tiles.length;i++)				  //	
	m_tile.add(tiles[i]);					  //	
	
      if (/*Arrays.binarySearch(tiles, map_copy[x][y]) >= 0*/m_tile.contains(map_copy[x][y])) {
	map_copy[x][y] = color;
	tolook.push(new Point(x+1, y));
	tolook.push(new Point(x+1, y+1));
	tolook.push(new Point(x+1, y-1));
	tolook.push(new Point(x-1, y));
	tolook.push(new Point(x-1, y+1));
	tolook.push(new Point(x-1, y-1));
	tolook.push(new Point(x, y+1));
	tolook.push(new Point(x, y-1));
      }
    }
  }

  /**
   * Returns the top left coordinate of all the empty land spaces with
   * the given width and height. 
   */
  public List<Point> findEmptyLand(int size) {
    return findEmptyLand(size, size);
  }
  public List<Point> findEmptyLand(int width, int height) {
    char[] emptyLand = new char[]{WargusStateImporter.TERRAIN_GRASS,
	WargusStateImporter.TERRAIN_UNIT, WargusStateImporter.TERRAIN_UNITOPP};
    return findEmpty(width, height, emptyLand);
  }
  public List<Point> findEmptyWater(int size) {
    return findEmptyWater(size, size);
  }
  public List<Point> findEmptyWater(int width, int height) {
    char[] emptyWater = new char[]{WargusStateImporter.TERRAIN_WATER,
	WargusStateImporter.TERRAIN_UNIT, WargusStateImporter.TERRAIN_UNITOPP};
    return findEmpty(width, height, emptyWater);
  }
  public List<Point> findEmpty(int width, int height, char[] types) {
    if (width > m_width || height > m_height || width < 1 || height < 1)
      throw new Error("WargusMap.findEmptyLand() passed invalid parameters: " + width + ", " + height + ", " + types);
    Arrays.sort(types);

    List<Point> pts = new Vector<Point>(); 
    // very inefficient.... but we have small maps..
    for (int x = 0; x < m_width - (width - 1); ++x) {
      for (int y = 0; y < m_height - (height - 1); ++y) {
	int sx = 0, sy = 0;
	boolean searching = true;
	for (sx = 0; sx < width; ++sx) {
	  for (sy = 0; sy < height; ++sy) {
	    if (Arrays.binarySearch(types, m_map[x+sx][y+sy]) < 0) {
	      // found non-empty land
	      searching = false;
	      break;
	    }
	  }
	  if (!searching) break;
	}
	// check if we found a good spot
	if (searching && sx == width && sy == height) {
	  pts.add(new Point(x, y));
	} // else keep searching
      }
    }		
    return pts;
  }

  public void insertMines() {
    for(WargusUnit gm : get_goldMines()) {
      for (int i = 0; i < 3; ++i) {
	for (int j = 0; j < 3; ++j) {
	  set_map(gm.getLocX() + i, gm.getLocY() + j, 
	      WargusStateImporter.TERRAIN_GOLD);
	}
      }
    }	
  }

  public void insertPlayerUnits(List<WargusUnit> units) {
    int i,j;

    for(WargusUnit u:units) {
      int size = WargusStateImporter.unitSize(u.getType());

      if (size==1) {
//	if (get_map(u.getLocX(),u.getLocY())!=WargusStateImporter.TERRAIN_GOLD &&
//	get_map(u.getLocX(),u.getLocY())!=WargusStateImporter.TERRAIN_OIL &&
//	get_map(u.getLocX(),u.getLocY())!=WargusStateImporter.TERRAIN_BUILDING) 
	set_map(u.getLocX(),u.getLocY(),WargusStateImporter.TERRAIN_UNIT);
      } else {
	for(i=0;i<size;i++) {
	  for(j=0;j<size;j++) {
	    set_map(u.getLocX()+j,u.getLocY()+i,WargusStateImporter.TERRAIN_BUILDING);
	  }
	}
      }									
    }
  }

  public void insertOpponentUnits(List<WargusUnit> units) {
    int i,j;

    for(WargusUnit u:units) {
      int size = WargusStateImporter.unitSize(u.getType());

      if (size==1) {
	set_map(u.getLocX(),u.getLocY(),WargusStateImporter.TERRAIN_UNITOPP);
      } else {
	for(i=0;i<size;i++) {
	  for(j=0;j<size;j++) {
	    set_map(u.getLocX()+j,u.getLocY()+i,WargusStateImporter.TERRAIN_BUILDINGOPP);
	  }
	}
      }									
    }
  }	


  /**
   * Debugging and testing methods.
   */
  public static void main(String[] args) {
    WargusMap m = new WargusMap(3, 3);
    m.m_map = new char[][] {
	{' ','#',' ',},
	{' ','#',' ',},
	{' ','#',' ',}
    };
    System.out.println(
	m.isPath(new Point(0,0), new Point(2, 2), 
	    new Character[]{
	  new Character(' '),
	  new Character('#')					
	}));

  }
}

