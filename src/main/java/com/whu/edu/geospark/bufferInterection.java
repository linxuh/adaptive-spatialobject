package com.whu.edu.geospark;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.node.IntNode;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class bufferInterection {

    static BufferedWriter out = null;

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\linxu\\Desktop\\boundary_buffer_data\\originLU_geojson_format.json"));
        BufferedReader in2 = new BufferedReader(new FileReader("D:\\data\\buffer_500m.json"));
        out = new BufferedWriter(new FileWriter("D:\\data\\result\\result500.csv"));
        String str,str2;
        SpatialIndex index = new Quadtree();
        GeoJSONReader geoJSONReader = new GeoJSONReader();

        while ((str = in.readLine()) != null) {
            FeatureCollection geoJson = (FeatureCollection)GeoJSONFactory.create(str);
            for(Feature f: geoJson.getFeatures()){
                Geometry g = geoJSONReader.read(f.getGeometry());
                g.setUserData(f.getId());
                index.insert(g.getEnvelopeInternal(),g);
            }
        }

        while ((str2 = in2.readLine()) != null) {
            FeatureCollection geoJson = (FeatureCollection)GeoJSONFactory.create(str2);
            for(Feature f: geoJson.getFeatures()){
                Geometry g2 = geoJSONReader.read(f.getGeometry());
                g2.setUserData(f.getId());
                List<Geometry> listRe = index.query(g2.getEnvelopeInternal());
                for(Geometry o : listRe){
                    if(g2.intersects(o)){
                        out.newLine();
                        out.write((IntNode)g2.getUserData()+","+(IntNode)o.getUserData());
                    }
                }
            }
        }
        out.close();
        in.close();
        in2.close();

    }

    public static Polygon parsePolygon(String json){
        List<Coordinate> cos = new ArrayList<Coordinate>();
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONObject geometry = jsonObject.getJSONObject("geometry");
        JSONArray co = geometry.getJSONArray("coordinates");
        JSONArray coordinates = co.getJSONArray(0);
        Coordinate firstPoint = new Coordinate(coordinates.getJSONArray(0).getDouble(0),coordinates.getJSONArray(0).getDouble(1));
        cos.add(firstPoint);
        for(int i = 1; i < coordinates.size(); i++){
            JSONArray coordinate = coordinates.getJSONArray(i);
            double lng = coordinate.getDouble(0);
            double lat = coordinate.getDouble(1);
            Coordinate currentPoint = new Coordinate(lng,lat);
            cos.add(new Coordinate(lng,lat));
            if(currentPoint.equals2D(firstPoint)){
                break;
            }
        }
        LinearRing shell = new LinearRing(new CoordinateArraySequence(cos.toArray(new Coordinate[0])),new GeometryFactory());
        return new Polygon(shell,null,new GeometryFactory());
    }

}
