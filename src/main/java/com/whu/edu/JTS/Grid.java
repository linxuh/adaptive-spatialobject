package com.whu.edu.JTS;

import org.locationtech.geomesa.curve.Z2SFC;
import org.locationtech.jts.geom.Coordinate;

import java.io.Serializable;
import java.util.Objects;

public class Grid implements Comparable<Grid>, Serializable {
    byte level;//level<=256
    boolean contain = false;
    long gridID;

    public Grid(byte level,long gridID){
        this.level = level;
        this.gridID = gridID;
    }

    public Grid(byte level, Coordinate point){
        this.level = level;
        Z2SFC z2 = new Z2SFC(this.level);
        this.gridID = z2.index(point.x,point.y,false);
    }

    public Grid(byte level,long gridID,boolean contain){
        this.level = level;
        this.gridID = gridID;
        this.contain = contain;
    }

    public Grid[] getChildren(){
        return new Grid[]{ new Grid((byte)(level + 1), gridID << 2),
                new Grid((byte)(level + 1),(gridID << 2) | 1),
                new Grid((byte)(level + 1),(gridID << 2) | 2),
                new Grid((byte)(level + 1),(gridID << 2) | 3) };
    }

    public long getFather(){
        return gridID >> 2;
    }

    public long lowestOnBit(){
        return gridID & -gridID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grid grid = (Grid) o;
        return level == grid.level &&
                gridID == grid.gridID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, gridID);
    }

    @Override
    public String toString(){
        return "level="+level+"zid="+gridID;
    }

    @Override
    public int compareTo(Grid o) {
        if(o.level == this.level) {
            return Long.compare(this.gridID,o.gridID);
        }else if(o.level > this.level){
            return Long.compare(this.gridID,o.gridID>>(2*(o.level - this.level)));
        }else{
            return Long.compare(this.gridID>>(2*(this.level - o.level)),o.gridID);
        }

    }

    public byte getLevel() {
        return level;
    }

    public boolean isContainGrid() {
        return contain;
    }

    public long getGridID() {
        return gridID;
    }

    public static void main(String[] args){
        Grid a = new Grid((byte)0,0);
        Grid b = new Grid((byte)1,new Coordinate(179,80));
        for(Grid x:a.getChildren()){
            System.out.println(x.toString());
        }
        System.out.println(b.toString());

    }
}
