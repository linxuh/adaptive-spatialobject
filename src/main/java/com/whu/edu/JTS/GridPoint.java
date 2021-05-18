package com.whu.edu.JTS;

import org.locationtech.geomesa.curve.Z2SFC;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class GridPoint extends Point {
    Grid id;

    public GridPoint(CoordinateSequence coordinates, GeometryFactory factory, int level) {
        super(coordinates, factory);
        getGridID(coordinates,level);
    }

    public GridPoint(CoordinateSequence coordinates, GeometryFactory factory, Grid id) {
        super(coordinates, factory);
        this.id = id;
    }

    public void getGridID(CoordinateSequence coordinates, int level){
        if(!this.isEmpty()){
            Z2SFC z2 = new Z2SFC(level);
            long z2index = z2.index(coordinates.getX(0),coordinates.getY(0),false);
            this.id = new Grid((byte)level,z2index);
        }
    }

    public Grid getId(){
        return id;
    }
}
