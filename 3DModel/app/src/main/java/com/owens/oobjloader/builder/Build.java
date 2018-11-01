package com.owens.oobjloader.builder;

// This code was written by myself, Sean R. Owens, sean at guild dot net,
// and is released to the public domain. Share and enjoy. Since some
// people argue that it is impossible to release software to the public
// domain, you are also free to use this code under any version of the
// GPL, LPGL, Apache, or BSD licenses, or contact me for use of another
// license.  (I generally don't care so I'll almost certainly say yes.)
// In addition this code may also be used under the "unlicense" described
// at http://unlicense.org/ .  See the file UNLICENSE in the repo.

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import com.owens.oobjloader.parser.BuilderInterface;

public class Build implements BuilderInterface {

    private Logger log = Logger.getLogger(Build.class.getName());

    public String objFilename = null;
    // these accumulate each type of vertex as they are parsed, so they can then be referenced via
    // index.
    public ArrayList<VertexGeometric> verticesG = new ArrayList<VertexGeometric>();
    public ArrayList<VertexTexture> verticesT = new ArrayList<VertexTexture>();
    public ArrayList<VertexNormal> verticesN = new ArrayList<VertexNormal>();
    // we use this map to consolidate redundant face vertices. Since a face is defined as a list of
    // index
    // triplets, each index referring to a vertex within ONE of the three arraylists verticesG,
    // verticesT
    // or verticesN, two faces might end up specifying the same combination. Clearly (@TODO:
    // really?) this
    // combination should be shared between both faces.
    HashMap<String, FaceVertex> faceVerticeMap = new HashMap<String, FaceVertex>();
    // Each face vertex as it is parsed, minus the redundant face vertices. @TODO: Not used anywhere
    // yet, maybe get rid of this.
    public ArrayList<FaceVertex> faceVerticeList = new ArrayList<FaceVertex>();
    public ArrayList<Face> faces = new ArrayList<Face>();
    public HashMap<Integer, ArrayList<Face>> smoothingGroups = new HashMap<Integer, ArrayList<Face>>();
    private int currentSmoothingGroupNumber = NO_SMOOTHING_GROUP;
    private ArrayList<Face> currentSmoothingGroup = null;
    public HashMap<String, Group> groups = new HashMap<String, Group>();
    private Group currentGroup = null;
    private ArrayList<ArrayList<Face>> currentGroupFaceLists = new ArrayList<ArrayList<Face>>();
    public String objectName = null;
    private Material currentMaterial = null;
    private Material currentMap = null;
    public HashMap<String, Material> materialLib = new HashMap<String, Material>();
    private Material currentMaterialBeingParsed = null;
    public HashMap<String, Material> mapLib = new HashMap<String, Material>();
    private Material currentMapBeingParsed = null;
    public int faceTriCount = 0;
    public int faceQuadCount = 0;
    public int facePolyCount = 0;
    public int faceErrorCount = 0;

    public Build() {
    }

    public void setObjFilename(String filename) {
        this.objFilename = filename;
    }

    public void addVertexGeometric(float x, float y, float z) {
        verticesG.add(new VertexGeometric(x, y, z));
        // log.log(INFO,"Added geometric vertex " + verticesG.size() + " = " +
        // verticesG.get(verticesG.size() - 1));
    }

    public void addVertexTexture(float u, float v) {
        verticesT.add(new VertexTexture(u, v));
        // log.log(INFO,"Added texture vertex " + verticesT.size() + " = " +
        // verticesT.get(verticesT.size() - 1));
    }

    public void addVertexNormal(float x, float y, float z) {
        verticesN.add(new VertexNormal(x, y, z));
    }

    public void addPoints(int[] values) {
        log.log(INFO, "@TODO: Got " + values.length + " points in builder, ignoring");
    }

    public void addLine(int[] values) {
        log.log(INFO, "@TODO: Got a line of " + values.length + " segments in builder, ignoring");
    }

    public void addFace(int[] vertexIndices) {
        Face face = new Face();
        currentGroup.getFaceList().add(face);
        face.material = currentMaterial;
        face.map = currentMap;

        int loopi = 0;
        // @TODO: add better error checking - make sure values is not empty and that it is a
        // multiple of 3
        while (loopi < vertexIndices.length) {
            // > v is the vertex reference number for a point element. Each point
            // > element requires one vertex. Positive values indicate absolute
            // > vertex numbers. Negative values indicate relative vertex numbers.

            FaceVertex fv = new FaceVertex();
            // log.log(INFO,"Adding vertex g=" + vertexIndices[loopi] + " t=" + vertexIndices[loopi
            // + 1] + " n=" + vertexIndices[loopi + 2]);
            int vertexIndex;
            vertexIndex = vertexIndices[loopi++];
            currentGroup.getVertIndicesList().add(vertexIndex);
            // Note that we can use negative references to denote vertices in manner relative to the
            // current point in the file, i.e.
            // rather than "the 5th vertice in the file" we can say "the 5th vertice before now"
            if (vertexIndex < 0) {
                vertexIndex = vertexIndex + verticesG.size();
            }
            if (((vertexIndex - 1) >= 0) && ((vertexIndex - 1) < verticesG.size())) {
                // Note: vertex indices are 1-indexed, i.e. they start at
                // one, so we offset by -1 for the 0-indexed array lists.
                fv.v = verticesG.get(vertexIndex - 1);
            } else {
                log.log(SEVERE,
                        "Index for geometric vertex=" + vertexIndex
                                + " is out of the current range of geometric vertex values 1 to "
                                + verticesG.size() + ", ignoring");
            }

            vertexIndex = vertexIndices[loopi++];
            currentGroup.getVertIndicesList().add(vertexIndex);
            if (vertexIndex != EMPTY_VERTEX_VALUE) {
                if (vertexIndex < 0) {
                    // Note that we can use negative references to denote vertices in manner
                    // relative to the current point in the file, i.e.
                    // rather than "the 5th vertice in the file" we can say "the 5th vertice before
                    // now"
                    vertexIndex = vertexIndex + verticesT.size();
                }
                if (((vertexIndex - 1) >= 0) && ((vertexIndex - 1) < verticesT.size())) {
                    // Note: vertex indices are 1-indexed, i.e. they start at
                    // one, so we offset by -1 for the 0-indexed array lists.
                    fv.t = verticesT.get(vertexIndex - 1);
                } else {
                    log.log(SEVERE,
                            "Index for texture vertex=" + vertexIndex
                                    + " is out of the current range of texture vertex values 1 to "
                                    + verticesT.size() + ", ignoring");
                }
            }

            vertexIndex = vertexIndices[loopi++];
            currentGroup.getVertIndicesList().add(vertexIndex);
            if (vertexIndex != EMPTY_VERTEX_VALUE) {
                if (vertexIndex < 0) {
                    // Note that we can use negative references to denote vertices in manner
                    // relative to the current point in the file, i.e.
                    // rather than "the 5th vertice in the file" we can say "the 5th vertice before
                    // now"
                    vertexIndex = vertexIndex + verticesN.size();
                }
                if (((vertexIndex - 1) >= 0) && ((vertexIndex - 1) < verticesN.size())) {
                    // Note: vertex indices are 1-indexed, i.e. they start at
                    // one, so we offset by -1 for the 0-indexed array lists.
                    fv.n = verticesN.get(vertexIndex - 1);
                } else {
                    log.log(SEVERE,
                            "Index for vertex normal=" + vertexIndex
                                    + " is out of the current range of vertex normal values 1 to "
                                    + verticesN.size() + ", ignoring");
                }
            }

            if (fv.v == null) {
                log.log(SEVERE,
                        "Can't add vertex to face with missing vertex!  Throwing away face.");
                faceErrorCount++;
                return;
            }

            // Make sure we don't end up with redundant vertice
            // combinations - i.e. any specific combination of g,v and
            // t is only stored once and is reused instead.
            String key = fv.toString();
            FaceVertex fv2 = faceVerticeMap.get(key);
            if (null == fv2) {
                faceVerticeMap.put(key, fv);
                fv.index = faceVerticeList.size();
                faceVerticeList.add(fv);
            } else {
                fv = fv2;
            }

            face.add(fv);
        }
        // log.log(INFO,"Parsed face=" + face);
        if (currentSmoothingGroup != null) {
            currentSmoothingGroup.add(face);
        }

        if (currentGroupFaceLists.size() > 0) {
            for (loopi = 0; loopi < currentGroupFaceLists.size(); loopi++) {
                currentGroupFaceLists.get(loopi).add(face);
            }
        }

        faces.add(face);

        // collect some stats for laughs
        if (face.vertices.size() == 3) {
            faceTriCount++;
        } else if (face.vertices.size() == 4) {
            faceQuadCount++;
        } else {
            facePolyCount++;
        }
    }

    // @TODO: http://local.wasp.uwa.edu.au/~pbourke/dataformats/obj/
    //
    // > Grouping
    // >
    // > There are four statements in the .obj file to help you manipulate groups
    // > of elements:
    // >
    // > o Gropu name statements are used to organize collections of
    // > elements and simplify data manipulation for operations in
    // > Model.
    // ...
    // > o Object name statements let you assign a name to an entire object
    // > in a single file.
    // >
    // > All grouping statements are state-setting. This means that once a
    // > group statement is set, it alpplies to all elements that follow
    // > until the next group statement.
    // >
    // > This portion of a sample file shows a single element which belongs to
    // > three groups. The smoothing group is turned off.
    // >
    // > g square thing all
    // > s off
    // > f 1 2 3 4
    // >
    // > This example shows two surfaces in merging group 1 with a merge
    // > resolution of 0.5.
    // >
    // > mg 1 .5
    // > surf 0.0 1.0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16
    // > surf 0.0 1.0 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32
    // >
    // > Syntax
    // >
    // > g group_name1 group_name2 . . .
    // >
    // > Polygonal and free-form geometry statement.
    // >
    // > Specifies the group name for the elements that follow it. You can
    // > have multiple group names. If there are multiple groups on one
    // > line, the data that follows belong to all groups. Group information
    // > is optional.
    // >
    // > group_name is the name for the group. Letters, numbers, and
    // > combinations of letters and numbers are accepted for group names.
    // > The default group name is default.
    // ...
    // > 1. Cube with group names
    // >
    // > The following example is a cube with each of its faces placed in a
    // > separate group. In addition, all elements belong to the group cube.
    // >
    // > v 0.000000 2.000000 2.000000
    // > v 0.000000 0.000000 2.000000
    // > v 2.000000 0.000000 2.000000
    // > v 2.000000 2.000000 2.000000
    // > v 0.000000 2.000000 0.000000
    // > v 0.000000 0.000000 0.000000
    // > v 2.000000 0.000000 0.000000
    // > v 2.000000 2.000000 0.000000
    // > # 8 vertices
    // >
    // > g front cube
    // > f 1 2 3 4
    // > g back cube
    // > f 8 7 6 5
    // > g right cube
    // > f 4 3 7 8
    // > g top cube
    // > f 5 1 4 8
    // > g left cube
    // > f 5 6 2 1
    // > g bottom cube
    // > f 2 6 7 3
    // > # 6 elements
    public void setCurrentGroupNames(String[] names) {

        currentGroupFaceLists.clear();
        if (null == names) {
            // Set current group to 'none' - so since we've already
            // cleared the currentGroups lists, just return.
            return;
        }
        for (int loopi = 0; loopi < names.length; loopi++) {
            String group = names[loopi].trim();

            if (null == groups.get(group)) {
                currentGroup = new Group();
                groups.put(group, currentGroup);
            }
        }
    }

    public void addObjectName(String name) {
        this.objectName = name;
    }

    public void setCurrentSmoothingGroup(int groupNumber) {
        currentSmoothingGroupNumber = groupNumber;
        if (currentSmoothingGroupNumber == NO_SMOOTHING_GROUP) {
            return;
        }
        if (null == smoothingGroups.get(currentSmoothingGroupNumber)) {
            currentSmoothingGroup = new ArrayList<Face>();
            smoothingGroups.put(currentSmoothingGroupNumber, currentSmoothingGroup);
        }
    }

    // @TODO:
    //
    // > maplib filename1 filename2 . . .
    // >
    // > This is a rendering identifier that specifies the map library file
    // > for the texture map definitions set with the usemap identifier. You
    // > can specify multiple filenames with maplib. If multiple filenames
    // > are specified, the first file listed is searched first for the map
    // > definition, the second file is searched next, and so on.
    // >
    // > When you assign a map library using the Model program, Model allows
    // > only one map library per .obj file. You can assign multiple
    // > libraries using a text editor.
    // >
    // > filename is the name of the library file where the texture maps are
    // > defined. There is no default.
    public void addMapLib(String[] names) {
        if (null == names) {
            log.log(INFO,
                    "@TODO: ERROR! Got a maplib line with null names array - blank group line? (i.e. \"g\\n\" ?)");
            return;
        }
        if (names.length == 1) {
            log.log(INFO, "@TODO: Got a maplib line with one name=|" + names[0] + "|");
            return;
        }
        log.log(INFO, "@TODO: Got a maplib line;");
        for (int loopi = 0; loopi < names.length; loopi++) {
            log.log(INFO, "        names[" + loopi + "] = |" + names[loopi] + "|");
        }
    }

    // @TODO:
    //
    // > usemap map_name/off
    // >
    // > This is a rendering identifier that specifies the texture map name
    // > for the element following it. To turn off texture mapping, specify
    // > off instead of the map name.
    // >
    // > If you specify texture mapping for a face without texture vertices,
    // > the texture map will be ignored.
    // >
    // > map_name is the name of the texture map.
    // >
    // > off turns off texture mapping. The default is off.
    public void setCurrentUseMap(String name) {
        currentMap = mapLib.get(name);
    }

    // > usemtl material_name
    // >
    // > Polygonal and free-form geometry statement.
    // >
    // > Specifies the material name for the element following it. Once a
    // > material is assigned, it cannot be turned off; it can only be
    // > changed.
    // >
    // > material_name is the name of the material. If a material name is
    // > not specified, a white material is used.
    public void setCurrentUseMaterial(String name) {
        currentMaterial = materialLib.get(name);
        currentGroup.setMaterialName(name);
    }

    // > mtllib filename1 filename2 . . .
    // >
    // > Polygonal and free-form geometry statement.
    // >
    // > Specifies the material library file for the material definitions
    // > set with the usemtl statement. You can specify multiple filenames
    // > with mtllib. If multiple filenames are specified, the first file
    // > listed is searched first for the material definition, the second
    // > file is searched next, and so on.
    // >
    // > When you assign a material library using the Model program, only
    // > one map library per .obj file is allowed. You can assign multiple
    // > libraries using a text editor.
    // >
    // > filename is the name of the library file that defines the
    // > materials. There is no default.
    // @TODO: I think I need to just delete this... because we now parse material lib files in
    // Parse.java in processMaterialLib()
    // public void addMaterialLib(String[] names) {
    // if (null == names) {
    // log.log(INFO,"@TODO: Got a mtllib line with null names array - blank group line? (i.e.
    // \"g\\n\" ?)");
    // return;
    // }
    // if (names.length == 1) {
    // log.log(INFO,"@TODO: Got a mtllib line with one name=|" + names[0] + "|");
    // return;
    // }
    // log.log(INFO,"@TODO: Got a mtllib line;");
    // for (int loopi = 0; loopi < names.length; loopi++) {
    // log.log(INFO," names[" + loopi + "] = |" + names[loopi] + "|");
    // }
    // }
    public void newMtl(String name) {
        currentMaterialBeingParsed = new Material(name);
        materialLib.put(name, currentMaterialBeingParsed);
    }

    public void setXYZ(int type, float x, float y, float z) {
        ReflectivityTransmiss rt = currentMaterialBeingParsed.ka;
        if (type == MTL_KD) {
            rt = currentMaterialBeingParsed.kd;
        } else if (type == MTL_KS) {
            rt = currentMaterialBeingParsed.ks;
        } else if (type == MTL_TF) {
            rt = currentMaterialBeingParsed.tf;
        }

        rt.rx = x;
        rt.gy = y;
        rt.bz = z;
        rt.isXYZ = true;
        rt.isRGB = false;
    }

    public void setRGB(int type, float r, float g, float b) {
        ReflectivityTransmiss rt = currentMaterialBeingParsed.ka;
        if (type == MTL_KD) {
            rt = currentMaterialBeingParsed.kd;
        } else if (type == MTL_KS) {
            rt = currentMaterialBeingParsed.ks;
        } else if (type == MTL_TF) {
            rt = currentMaterialBeingParsed.tf;
        }

        rt.rx = r;
        rt.gy = g;
        rt.bz = b;
        rt.isRGB = true;
        rt.isXYZ = false;
    }

    public void setIllum(int illumModel) {
        currentMaterialBeingParsed.illumModel = illumModel;
    }

    public void setD(boolean halo, float factor) {
        currentMaterialBeingParsed.dHalo = halo;
        currentMaterialBeingParsed.dFactor = factor;
        log.log(INFO, "@TODO: got a setD call!");
    }

    public void setNs(float exponent) {
        currentMaterialBeingParsed.nsExponent = exponent;
        log.log(INFO, "@TODO: got a setNs call!");
    }

    public void setSharpness(float value) {
        currentMaterialBeingParsed.sharpnessValue = value;
    }

    public void setNi(float opticalDensity) {
        currentMaterialBeingParsed.niOpticalDensity = opticalDensity;
    }

    public void setMapDecalDispBump(int type, String filename) {
        if (type == MTL_MAP_KA) {
            currentMaterialBeingParsed.mapKaFilename = filename;
        } else if (type == MTL_MAP_KD) {
            currentMaterialBeingParsed.mapKdFilename = filename;
        } else if (type == MTL_MAP_KS) {
            currentMaterialBeingParsed.mapKsFilename = filename;
        } else if (type == MTL_MAP_NS) {
            currentMaterialBeingParsed.mapNsFilename = filename;
        } else if (type == MTL_MAP_D) {
            currentMaterialBeingParsed.mapDFilename = filename;
        } else if (type == MTL_DECAL) {
            currentMaterialBeingParsed.decalFilename = filename;
        } else if (type == MTL_DISP) {
            currentMaterialBeingParsed.dispFilename = filename;
        } else if (type == MTL_BUMP) {
            currentMaterialBeingParsed.bumpFilename = filename;
        }
    }

    public void setRefl(int type, String filename) {
        currentMaterialBeingParsed.reflType = type;
        currentMaterialBeingParsed.reflFilename = filename;
    }

    public void doneParsingMaterial() {
        // if we finish a .mtl file, and then we parse another mtllib (.mtl) file AND that other
        // file is malformed, and missing the newmtl line, then any statements would quietly
        // overwrite whatever is being pointed to by currentMaterialBeingParsed. Hence we set
        // it to null now. (Now any such malformed .mtl file will cause an exception but that's
        // better than quiet bugs.) This method ( doneParsingMaterial() ) is called by Parse when
        // it finished parsing a .mtl file.
        //
        // @TODO: We can make this not throw an exception if we simply add a check for a null
        // currentMaterialBeingParsed at the start of each material setter method in Build... but
        // that
        // still assumes we'll always have a newmtl line FIRST THING for each material, to create
        // the
        // currentMaterialBeingParsed object. Is that a reasonable assumption?
        currentMaterialBeingParsed = null;
    }

    public void doneParsingObj(String filename) {
        log.log(INFO,
                "Loaded filename '" + filename + "' with " + verticesG.size() + " verticesG, "
                        + verticesT.size() + " verticesT, " + verticesN.size() + " verticesN and "
                        + faces.size() + " faces, of which " + faceTriCount + " triangles, "
                        + faceQuadCount + " quads, and " + facePolyCount
                        + " with more than 4 points, and faces with errors " + faceErrorCount);
    }
}
