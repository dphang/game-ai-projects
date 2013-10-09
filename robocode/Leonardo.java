package tmnr;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import robocode.*;
import robocode.util.Utils;

public class Leonardo extends TeamRobot {
    private static Point2D.Double currentLocation;
    private static double distanceLeft;
    private static Point2D.Double lastPosition;
    private static double distClosest;
    private static Point2D.Double destination;
    private static HashMap<String, Robot> robots;
    private static Robot target, closest;
    private static Rectangle2D.Double field;
    private static double myEnergy;
    private static List<Wave> waves = new ArrayList<Wave>();
    private static int direction;
    private static int[][] stats = new int[13][101];

    // Main running method
    public void run() {
        // Independent radar and gun
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        setColors(new Color(0, 100, 0), new Color(25, 25, 80), Color.black);
        field = new Rectangle2D.Double(35, 35, getBattleFieldWidth() - 70,
                getBattleFieldHeight() - 70);
        robots = new HashMap<String, Robot>();
        target = new Robot();
        closest = new Robot();
        closest.pos = lastPosition = currentLocation = destination = new Point2D.Double(
                getX(), getY());
        do {
            // Update location and energy
            myEnergy = getEnergy();
            currentLocation = new Point2D.Double(getX(), getY());

            // Update distance with the closest bot

            distClosest = currentLocation.distance(closest.pos);

            // Movement
            move();

            execute();
        } while (true);

    }

    // Movement
    private void move() { // Minimum risk movement based on distance from
                          // robots, perpendicularity, energy, etc.
        distanceLeft = currentLocation.distance(destination);
        if (distanceLeft <= 15) { // Reevaluate if destination is reached
            int i = 0;
            do {
                Point2D.Double test = getPoint(currentLocation, i * Math.PI
                        / 75, Math.min(distClosest, 50 + Math.random() * 200));
                if (field.contains(test)
                        && getRisk(test) < getRisk(destination)) {
                    destination = test;
                }
            } while (i++ < 300);
            lastPosition = currentLocation;
        } else {
            double angle = absoluteBearing(currentLocation, destination)
                    - getHeadingRadians();
            double direction = 1;

            if (Math.cos(angle) < 0) {
                angle += Math.PI;
                direction = -1;
            }
            setAhead(distanceLeft * direction);
            setTurnRightRadians(angle = Utils.normalRelativeAngle(angle));
            setMaxVelocity(Math.abs(angle) > 1 ? 0 : 8);
        }

    }

    private double getRisk(Point2D.Double destination) { // Risk function based
                                                         // on various functions
        double risk = 0.10 / destination.distanceSq(lastPosition);
        Iterator<Robot> iterate = robots.values().iterator();
        while (iterate.hasNext()) {
            Robot rob = (Robot) iterate.next();
            if (rob.alive)
                risk += Math.min(rob.energy / myEnergy, 2)
                        * (1 + Math.abs(Math.cos(absoluteBearing(destination,
                                currentLocation)
                                - absoluteBearing(currentLocation, rob.pos))))
                        / destination.distanceSq(rob.pos);
            try {
                if (rob.team
                        && Math.abs(absoluteBearing(rob.pos, destination)
                                - absoluteBearing(rob.pos, rob.target.pos)) < Math.PI / 15) {
                    risk += Double.POSITIVE_INFINITY;
                }
            } catch (NullPointerException ex) {
            }

        }
        return risk;
    }

    // Events

    public void onHitRobot(HitRobotEvent e) {
        // Seems to get hit at times, but if it hits a robot it reevaluates a
        // position
        destination = currentLocation;
        move();
    }

    public void onRobotDeath(RobotDeathEvent e) {
        robots.get(e.getName()).alive = false;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // Robot data management
        Robot rob = robots.get(e.getName());
        if (rob == null) {
            rob = new Robot();
            robots.put(e.getName(), rob);
            rob.team = isTeammate(e.getName());
        }
        rob.absBearing = e.getBearingRadians() + getHeadingRadians();
        rob.energy = e.getEnergy();
        rob.alive = true;
        rob.heading = e.getHeadingRadians();
        rob.pos = getPoint(currentLocation, rob.absBearing, e.getDistance());

        // If too close to current closest bot, move
        if (currentLocation.distance(closest.pos) < 10) {
            destination = currentLocation;
            move();
        }

        // Set new target only if not a teammate

        if (!rob.team
                && (!target.alive || e.getDistance() < currentLocation
                        .distance(target.pos))) {
            target = rob;
        }

        // Set new closest bot (can be teammate or enemy)
        if (!closest.alive
                || e.getDistance() < currentLocation.distance(closest.pos)) {
            closest = rob;
        }

        // Evaluate waves generated from firing
        try {
            for (int i = 0; i < waves.size(); i++) {
                Wave currentWave = (Wave) waves.get(i);
                if (currentWave.isHit(target.pos.x, target.pos.y, getTime())) {
                    i--;
                    waves.remove(currentWave);
                }
            }
        } catch (Exception ex) {
        }

        // Sets power based on energy
        double power = (3 - (20 - myEnergy) / 6);
        if (power > 3)
            power = 3;
        else if (power < 0.1)
            power = 0.1;

        if (target.velocity != 0) {
            if (Math.sin(rob.heading - rob.absBearing) * e.getVelocity() < 0)
                direction = -1;
            else
                direction = 1;
        }
        int[] currentStats = stats[(int) (e.getDistance() / 100)];
        Wave wv = new Wave(getX(), getY(), rob.absBearing, power, direction,
                getTime(), currentStats);
        int best = (stats.length - 1) / 2;
        for (int i = 0; i < stats.length; i++)
            if (currentStats[best] < currentStats[i])
                best = i;
        double guess = (double) (best - (stats.length - 1) / 2)
                / ((stats.length - 1) / 2);
        double angleOffset = direction * guess * wv.maxEscapeAngle();
        double gunTurnRadians = Utils.normalRelativeAngle(target.absBearing
                - getGunHeadingRadians() + angleOffset);
        if (myEnergy > 1 && target.alive && getGunHeat() == 0
                && gunTurnRadians < Math.atan2(9, e.getDistance())
                && setFireBullet(power) != null) {
            waves.add(wv);
        }

        setTurnGunRightRadians(gunTurnRadians);
    }

    // Various utility functions

    public static double absoluteBearing(Point2D.Double source,
            Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    public static Point2D.Double getPoint(Point2D.Double source, double angle,
            double length) {
        return new Point2D.Double(source.x + Math.sin(angle) * length, source.y
                + Math.cos(angle) * length);

    }

    class Robot { // Storing stats of each robot on the field.
        private double energy;
        private double heading;
        private double absBearing;
        private double velocity;
        private Point2D.Double pos;
        private boolean alive = false;
        private boolean team = false;
        private Robot target;
    }

    class Wave { // Each time a bullet is fired we have a pulse/wave that shows
                 // where a bullet would have hit
        private double sourceX;
        private double sourceY;
        private double sourceBearing;
        private double firePower;
        private double direction;
        private long fireTime;
        private int[] segment;

        public boolean isHit(double enemyX, double enemyY, long currentTime) {
            if ((currentTime - fireTime) * getBulletSpeed() >= Point2D
                    .distance(sourceX, sourceY, enemyX, enemyY)) {
                double actualAngle = Math.atan2(enemyX - sourceX, enemyY
                        - sourceY);
                double angleOffset = Utils.normalRelativeAngle(actualAngle
                        - sourceBearing);
                double guessFactor = Math.max(-1,
                        Math.min(1, angleOffset / maxEscapeAngle()))
                        * direction;
                int index = (int) Math.round((segment.length - 1) / 2
                        * (guessFactor + 1));
                segment[index]++;
                return true;
            } else
                return false;
        }

        public double getBulletSpeed() {
            return 20 - firePower * 3;
        }

        public double maxEscapeAngle() {
            return Math.asin(8 / getBulletSpeed());
        }

        public Wave(double x, double y, double bearing, double power,
                int direction, long time, int[] segment) {
            sourceX = x;
            sourceY = y;
            sourceBearing = bearing;
            firePower = power;
            fireTime = time;
            this.direction = direction;
            this.segment = segment;
        }
    }

}
