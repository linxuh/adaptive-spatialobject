package com.whu.edu.JTS;

import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.sweepline.SweepLineIndex;
import org.locationtech.jts.index.sweepline.SweepLineInterval;
import org.locationtech.jts.index.sweepline.SweepLineOverlapAction;

import java.util.*;

public class GridLineIntersection2 {
    public static int filterTime = 0;
    public static int filterCount = 0;
    public static int refinementTime = 0;
    public static double intersectionPointCount = 0;
    public static double intersectionCount = 0;
    public static double intersectionTrueCount = 0;

    public static GeometryFactory gf = new GeometryFactory();

    public static boolean intersection(GridLineString g1, GridLineString g2){

        /** filter step **/
        long startTime = System.currentTimeMillis();
        Long[] intersectGrid = intersection(g1.gridIDs,g2.gridIDs);
        long endTime = System.currentTimeMillis();
        filterTime += endTime - startTime;
        if(intersectGrid.length == 0){
            filterCount++;
            return false;
        }

        /** refinement step **/
        Envelope[] env = new Envelope[intersectGrid.length];

        int i = 0;
        for(Long id : intersectGrid){
            double[] boundary = GridUtils.getGridBoundary(new Grid(g1.level,id));
            env[i] = new Envelope(boundary[0],boundary[1],boundary[2],boundary[3]);
            i++;
        }
        GeometryEnvelopsFilter gef = new GeometryEnvelopsFilter(env);
        gef.initLineCrosser();


        SweepLineIndex sw = new SweepLineIndex();
        LineIntersector lis = new RobustLineIntersector();


        g1.apply(gef);
        Coordinate[] cos = g1.getCoordinates();
        for(Integer[] kv : gef.getMap()){
            for(int k = kv[0]; k < kv[1]; k++){
                LineSegmentWithLID ls = new LineSegmentWithLID(cos[k],cos[k+1],1);
                sw.add(new SweepLineInterval(cos[k].getX(),cos[k+1].getX(),ls));
            }
        }

        g2.apply(gef);
        Coordinate[] cos2 = g2.getCoordinates();
        for(Integer[] kv : gef.getMap()){
            for(int k = kv[0]; k < kv[1]; k++){
                LineSegmentWithLID ls = new LineSegmentWithLID(cos2[k],cos2[k+1],2);
                sw.add(new SweepLineInterval(cos2[k].getX(),cos2[k+1].getX(),ls));
            }
        }

        long start = System.currentTimeMillis();
        SegmentIntersectAction SO = new SegmentIntersectAction(lis);
        sw.computeOverlaps(SO);
        long end = System.currentTimeMillis();
        refinementTime += end - start;

        return SO.isIntersection();
    }

    //sort first and double pointer to find intersection
    //O(nlogn), but faster than hashset method via leetcode
    public static Long[] intersection(Long[] nums1, Long[] nums2) {
        int length1 = nums1.length, length2 = nums2.length;
        if(length1 == 0 || length2 == 0 || nums1[0] > nums2[length2 - 1] || nums2[0] > nums1[length1 - 1] ){
            return new Long[0];
        }

        int index = 0, index1 = 0, index2 = 0;
        Long[] intersection = new Long[length1 + length2];
        while (index1 < length1 && index2 < length2) {
            long num1 = nums1[index1];
            long num2 = nums2[index2] ;
            if (num1 == num2) {
                //保证元素的唯一性
                if (index == 0 || num1 != intersection[index - 1]) {
                    intersection[index++] = num1;
                }
                index1++;
                index2++;
            } else if (num1 < num2) {
                index1++;
            } else {
                index2++;
            }
        }
        return Arrays.copyOfRange(intersection, 0, index);
    }

    //faster in theory but actually slow
    public static Long[] getIntersection(Long[] num1, Long[] num2) {
        Set<Long> set1 = new HashSet<>();
        Set<Long> intersectionSet = new HashSet<>();

        for(Long num : num1){
            set1.add(num);
        }

        for(Long num : num2){
            if(set1.contains(num)){
                intersectionSet.add(num);
            }
        }

        Long[] intersection = new Long[intersectionSet.size()];
        int index = 0;
        for (Long num : intersectionSet) {
            intersection[index++] = num;
        }
        return intersection;
    }

    public static void printTime(){
        System.out.println("filterTime : "+filterTime);
        System.out.println("grid filter count : "+filterCount);
        System.out.println("refineTime : "+refinementTime);
        System.out.println("points of intersection line : "+intersectionPointCount/(intersectionCount*2));
        System.out.println("refinement ratio : "+intersectionTrueCount/intersectionCount);
    }
}

class LineSegmentWithLID extends LineSegment{
    int id;
    LineSegmentWithLID(Coordinate c1, Coordinate c2, int id){
        super(c1,c2);
        this.id = id;
    }
}

class SegmentIntersectAction implements SweepLineOverlapAction {

    private LineIntersector li = null;
    private boolean findIntersection = false;

    public SegmentIntersectAction(LineIntersector li){
        this.li = li;
    }

    public boolean isIntersection(){
        return findIntersection;
    }

    @Override
    public void overlap(SweepLineInterval sweepLineInterval, SweepLineInterval sweepLineInterval1) {
        LineSegmentWithLID ls1 = (LineSegmentWithLID) sweepLineInterval.getItem();
        LineSegmentWithLID ls2 = (LineSegmentWithLID) sweepLineInterval1.getItem();

        if(ls1.id == ls2.id){
            return;
        }else{
            li.computeIntersection(ls1.p0,ls1.p1,ls2.p0,ls2.p1);
            if(li.hasIntersection()){
                findIntersection = true;
            }
        }
    }
}