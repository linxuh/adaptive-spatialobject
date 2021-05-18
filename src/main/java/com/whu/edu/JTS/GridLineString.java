package com.whu.edu.JTS;

import org.locationtech.geomesa.curve.Z2SFC;
import org.locationtech.jts.algorithm.RectangleLineIntersector;
import org.locationtech.jts.geom.*;
import org.locationtech.sfcurve.zorder.Z2;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class GridLineString extends LineString{
    public Long[] gridIDs;
    public byte level;

    public GridLineString(CoordinateSequence points, GeometryFactory factory, byte gridLevel) {
        super(points, factory);
        this.level = gridLevel;
        this.gridIDs = getGridIDs(points);
    }

    public GridLineString(CoordinateSequence points, GeometryFactory factory, Long[] gridIDs, byte gridLevel){
        super(points, factory);
        this.level = gridLevel;
        this.gridIDs = gridIDs;

    }

    public Long[] getGridIDs(CoordinateSequence points){
        HashSet<Long> all = new HashSet<Long>();
        ArrayList<Long> frontier = new ArrayList<Long>();
        ArrayList<Long> output = new ArrayList<Long>();
        Z2SFC z2 = new Z2SFC(level);
        long startID = z2.index(points.getX(0),points.getY(0),true);
        //Grid start = new Grid(level, startID);
        all.add(startID);
        frontier.add(startID);

        while (!frontier.isEmpty()) {
            long id = frontier.get(frontier.size() - 1);
            frontier.remove(frontier.size() - 1);
            if (!this.mayIntersect(id, points)) {
                continue;
            }
            output.add(id);

            long[] neighbors = new long[4];
            getEdgeNeighbors(id,neighbors);
            for (int edge = 0; edge < 4; ++edge) {
                long nbr = neighbors[edge];
                if (!all.contains(nbr)) {
                    frontier.add(nbr);
                    all.add(nbr);
                }
            }
        }
        //sort before find intersection then O(n) can solve
        Long[] result = output.toArray(new Long[0]);
        Arrays.sort(result);
        return result;
    }

    public boolean mayIntersect(long id, CoordinateSequence points){
        if (points.size() == 0) {
            return false;
        }
        double[] boundary = GridUtils.getGridBoundary(new Grid(level,id));
        Envelope e = new Envelope(boundary[0],boundary[1],boundary[2],boundary[3]);
        RectangleLineIntersector gridLineCrosser = new RectangleLineIntersector(e);

        for (int i = 0; i < points.size(); ++i) {
            if (e.covers(points.getCoordinate(i))) {
                return true;
            }
        }

        for (int i = 1; i < points.size(); ++i) {
            if (gridLineCrosser.intersects(points.getCoordinate(i-1),points.getCoordinate(i))) {
                // There is a proper crossing, or two vertices were the same.
                return true;
            }
        }

        return false;
    }

    public static void getEdgeNeighbors(long id,long[] neighbors){
        Tuple2<Object, Object> XY;
        Z2 z2 = new Z2(id);
        XY = z2.decode();
        neighbors[0] = Z2.apply((Integer)XY._1(), (Integer)XY._2() + 1);
        neighbors[1] = Z2.apply((Integer)XY._1() + 1, (Integer)XY._2());
        neighbors[2] = Z2.apply((Integer)XY._1(), (Integer)XY._2() - 1);
        neighbors[3] = Z2.apply((Integer)XY._1() - 1, (Integer)XY._2());
    }

    public void apply(CoordinateFilter filter){
        ((EnvelopeFilter)filter).initPointer();
        ((EnvelopeFilter)filter).preCo = this.points.getCoordinate(0);
        for(int i = 1; i < this.points.size(); ++i) {
            filter.filter(this.points.getCoordinate(i));
        }
        ((EnvelopeFilter)filter).done();
    }

    @Override
    public boolean intersects(Geometry g){
        if(g instanceof GridLineString){
            return GridLineIntersection2.intersection(this,(GridLineString)g);
        }else{

        }
        return false;
    }
}
