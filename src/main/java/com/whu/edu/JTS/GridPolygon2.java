package com.whu.edu.JTS;

import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**we only consider the polygon without hole**/
public class GridPolygon2 extends Polygon {
    Grid[] gridiDs;
    int recursiveTimes;
    int maxLevel;
    int containNum = 0;
    int intersection = 0;

    public GridPolygon2(LinearRing shell, PrecisionModel precisionModel, int SRID) {
        super(shell, precisionModel, SRID);
    }

    public GridPolygon2(LinearRing shell, LinearRing[] holes, GeometryFactory factory, int maxLevel, int recursiveTimes) {
        super(shell, holes,factory);
        this.recursiveTimes = recursiveTimes;
        this.maxLevel = maxLevel;
        getGridIDs(shell,recursiveTimes);
    }

    public GridPolygon2(LinearRing shell, LinearRing[] holes, GeometryFactory factory, Grid[] ids) {
        super(shell, holes,factory);
        this.recursiveTimes = recursiveTimes;
        this.maxLevel = maxLevel;
        gridiDs = ids;
    }

    /** Generates a covering and stores it in result. */
    private void getGridIDs(LinearRing shell, int recursiveTimes) {
        LinkedList<Grid> candidateQueue = new LinkedList<>();
        ArrayList<Grid> result = new ArrayList<>();
        ArrayList<Grid> intersectResult = new ArrayList<>();
        ArrayList<Grid> coveredResult = new ArrayList<>();

        /** Computes a set of initial candidates that cover the given region. */
        Envelope e = shell.getEnvelopeInternal();
        int level = (int) Math.floor( Math.log( 360 / Math.max(e.getWidth(),e.getHeight()*2)) / Math.log(2));

        maxLevel = Math.min(level + recursiveTimes, this.maxLevel);
        //System.out.println(level);
        getInitGrid(level,e,candidateQueue);

        //TODO:
        // if a grid children which interect the polygon number is 1 ,then add it into result
        // it will not add the number of result but have good performance
        // And then if we do not distinct the cover cell and the intersection cell ,then we can
        // add the father cell to result if children cells all intersect with the polygon

        while (!candidateQueue.isEmpty()) {
            Grid candidate = candidateQueue.poll();
            //System.out.println("Pop: " + candidate.toString());
            Grid[] childCells = candidate.getChildren();
            for (int i = 0; i < 4; ++i) {
                if(this.mayIntersect(childCells[i])){
                    if(judgeContain(childCells[i])){
                        childCells[i].contain = true;
                        result.add(childCells[i]);
                        containNum++;
                    }else if(childCells[i].level >= (byte)maxLevel){
                        maxLevel = childCells[i].level;
                        result.add(childCells[i]);
                        intersection++;
                    }else{
                        candidateQueue.add(childCells[i]);
                    }
                }
            }
        }
        gridiDs = result.toArray(new Grid[0]);
        Arrays.sort(gridiDs);
    }

    public boolean mayIntersect(Grid cell) {

        double[] boundary = GridUtils.getGridBoundary(cell);
        Envelope cellBound = new Envelope(boundary[0],boundary[1],boundary[2],boundary[3]);
        if (!this.getEnvelopeInternal().intersects(cellBound)) {
            return false;
        }

        return EnvelopeIntersects.intersects(cellBound,this);
    }

    public boolean judgeContain(Grid cell) {
        double[] boundary = GridUtils.getGridBoundary(cell);
        Envelope cellBound = new Envelope(boundary[0],boundary[1],boundary[2],boundary[3]);
        EnvelopeContainsAllPointVisitor ecav = new EnvelopeContainsAllPointVisitor(cellBound);
        ecav.visit(this);

        if (ecav.containsAllPoint()) {
            RectangleIntersectsSegmentVisitor risv = new RectangleIntersectsSegmentVisitor(cellBound);
            risv.visit(this);
            if(!risv.intersects()){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Geometry g){
        if(g instanceof GridPoint){
            return GridPointInPolygon.contain(this,(GridPoint)g);
        }
        return false;
    }

    @Override
    public boolean intersects(Geometry g){
        if(g instanceof GridPolygon2){
            return GridPolygonIntersection.intersection(this,(GridPolygon2)g);
        }
        return false;
    }

    public void apply(CoordinateFilter filter){
        ((EnvelopeFilter)filter).initPointer();
        ((EnvelopeFilter)filter).preCo = this.shell.getCoordinate();
        for(int i = 1; i < this.shell.getCoordinates().length; ++i) {
            filter.filter(this.shell.getCoordinateN(i));
        }
        ((EnvelopeFilter)filter).done();

        for(int i = 0; i < this.holes.length; ++i) {
            this.holes[i].apply(filter);
        }
    }

    public void getInitGrid(int level, Envelope e, LinkedList<Grid> candidateQueue){
        double maxX = e.getMaxX();
        double maxY = e.getMaxY();
        double minX = e.getMinX();
        double minY = e.getMinY();

        Grid leftBottom = new Grid((byte)level,new Coordinate(minX,minY));
        if(mayIntersect(leftBottom)){
            candidateQueue.add(leftBottom);
        }

        Grid rightTop = new Grid((byte)level,new Coordinate(maxX,maxY));
        if(rightTop.equals(leftBottom)){
            if(candidateQueue.size() == 0){
                candidateQueue.add(rightTop);
            }
            return;
        }else if(mayIntersect(rightTop)){
            candidateQueue.add(rightTop);
        }

        Grid leftTop = new Grid((byte)level,new Coordinate(minX,maxY));
        if(leftTop.equals(leftBottom) || leftTop.equals(rightTop)){
            return;
        }else if(mayIntersect(leftTop)){
            candidateQueue.add(leftTop);
        }

        Grid rightBottom = new Grid((byte)level,new Coordinate(maxX,minY));
        if(mayIntersect(rightBottom)){
            candidateQueue.add(rightBottom);
        }
        return;
    }


    public static int getLevel(int num) {
        if (num <= 1) return 1;
        num |= num >> 1;
        num |= num >> 2;
        num |= num >> 4;
        num |= num >> 8;
        num |= num >> 16;
        return num + 1;
    }

    public Grid[] getGridID(){
        return this.gridiDs;
    }
}
