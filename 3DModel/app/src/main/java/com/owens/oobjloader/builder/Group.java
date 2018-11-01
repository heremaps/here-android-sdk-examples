
package com.owens.oobjloader.builder;

import java.util.ArrayList;

public class Group {
    private ArrayList<Face> faceList;
    private String materialName;
    private ArrayList<Integer> vertIndicesList;

    public Group() {
        faceList = new ArrayList<>();
        vertIndicesList = new ArrayList<>();
    }

    /**
     * @return the faceList
     */
    public ArrayList<Face> getFaceList() {
        return faceList;
    }

    /**
     * @return the materialName
     */
    public String getMaterialName() {
        return materialName;
    }

    /**
     * @param materialName
     *            the materialName to set
     */
    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    /**
     * @return the vertIndicesList
     */
    public ArrayList<Integer> getVertIndicesList() {
        return vertIndicesList;
    }
}
