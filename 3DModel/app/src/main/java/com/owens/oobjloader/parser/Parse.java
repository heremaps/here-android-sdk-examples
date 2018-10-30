package com.owens.oobjloader.parser;

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
import static java.util.logging.Level.WARNING;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import android.app.Activity;

public class Parse {
    private Logger log = Logger.getLogger(Parse.class.getName());

    // Tokens for parsing.
    private final static String OBJ_VERTEX_TEXTURE = "vt";
    private final static String OBJ_VERTEX_NORMAL = "vn";
    private final static String OBJ_VERTEX = "v";
    private final static String OBJ_FACE = "f";
    private final static String OBJ_GROUP_NAME = "g";
    private final static String OBJ_OBJECT_NAME = "o";
    private final static String OBJ_SMOOTHING_GROUP = "s";
    private final static String OBJ_POINT = "p";
    private final static String OBJ_LINE = "l";
    private final static String OBJ_MAPLIB = "maplib";
    private final static String OBJ_USEMAP = "usemap";
    private final static String OBJ_MTLLIB = "mtllib";
    private final static String OBJ_USEMTL = "usemtl";
    private final static String MTL_NEWMTL = "newmtl";
    private final static String MTL_KA = "Ka";
    private final static String MTL_KD = "Kd";
    private final static String MTL_KS = "Ks";
    private final static String MTL_TF = "Tf";
    private final static String MTL_ILLUM = "illum";
    private final static String MTL_D = "d";
    private final static String MTL_D_DASHHALO = "-halo";
    private final static String MTL_NS = "Ns";
    private final static String MTL_SHARPNESS = "sharpness";
    private final static String MTL_NI = "Ni";
    private final static String MTL_MAP_KA = "map_Ka";
    private final static String MTL_MAP_KD = "map_Kd";
    private final static String MTL_MAP_KS = "map_Ks";
    private final static String MTL_MAP_NS = "map_Ns";
    private final static String MTL_MAP_D = "map_d";
    private final static String MTL_DISP = "disp";
    private final static String MTL_DECAL = "decal";
    private final static String MTL_BUMP = "bump";
    private final static String MTL_REFL = "refl";
    public final static String MTL_REFL_TYPE_SPHERE = "sphere";
    public final static String MTL_REFL_TYPE_CUBE_TOP = "cube_top";
    public final static String MTL_REFL_TYPE_CUBE_BOTTOM = "cube_bottom";
    public final static String MTL_REFL_TYPE_CUBE_FRONT = "cube_front";
    public final static String MTL_REFL_TYPE_CUBE_BACK = "cube_back";
    public final static String MTL_REFL_TYPE_CUBE_LEFT = "cube_left";
    public final static String MTL_REFL_TYPE_CUBE_RIGHT = "cube_right";
    BuilderInterface builder = null;
    File objFile = null;
    Activity m_activity = null;

    public Parse(BuilderInterface builder, Activity activity, String filename)
            throws FileNotFoundException, IOException {
        this.builder = builder;
        m_activity = activity;
        builder.setObjFilename(filename);
        parseObjFile(m_activity, filename);

        builder.doneParsingObj(filename);
    }

    private void parseObjFile(Activity activity, String objFilename)
            throws FileNotFoundException, IOException {
        int lineCount = 0;

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(activity.getAssets().open(objFilename)));

        String line = null;

        while (true) {
            line = bufferedReader.readLine();
            if (null == line) {
                break;
            }

            line = line.trim();

            if (line.length() == 0) {
                continue;
            }

            // NOTE: we don't check for the space after the char
            // because sometimes it's not there - most notably in the
            // grouupname, we seem to get a lot of times where we have
            // "g\n", i.e. setting the group name to blank (or
            // default?)
            if (line.startsWith("#")) // comment
            {
                continue;
            } else if (line.startsWith(OBJ_VERTEX_TEXTURE)) {
                processVertexTexture(line);
            } else if (line.startsWith(OBJ_VERTEX_NORMAL)) {
                processVertexNormal(line);
            } else if (line.startsWith(OBJ_VERTEX)) {
                processVertex(line);
            } else if (line.startsWith(OBJ_FACE)) {
                processFace(line);
            } else if (line.startsWith(OBJ_GROUP_NAME)) {
                processGroupName(line);
            } else if (line.startsWith(OBJ_OBJECT_NAME)) {
                processObjectName(line);
            } else if (line.startsWith(OBJ_SMOOTHING_GROUP)) {
                processSmoothingGroup(line);
            } else if (line.startsWith(OBJ_POINT)) {
                processPoint(line);
            } else if (line.startsWith(OBJ_LINE)) {
                processLine(line);
            } else if (line.startsWith(OBJ_MAPLIB)) {
                processMapLib(line);
            } else if (line.startsWith(OBJ_USEMAP)) {
                processUseMap(line);
            } else if (line.startsWith(OBJ_USEMTL)) {
                processUseMaterial(line);
            } else if (line.startsWith(OBJ_MTLLIB)) {
                processMaterialLib(line);
            } else {
                log.log(WARNING, "line " + lineCount + " unknown line |" + line + "|");
            }
            lineCount++;
        }
        bufferedReader.close();

        log.log(INFO, "Loaded " + lineCount + " lines");
    }

    // @TODO: processVertex calls parseFloatList with params expecting
    // only three floats on the line. If there are more than three
    // floats in line then any extra values will be ignored by
    // parseFloatList. For the 'v' (geometric vertex) lines, there
    // may be three values (x,y,z) or there may be four (x,y,z and w
    // i.e. weight). Currently we're ignoring w if it is present.
    //
    // ------------------------------------------------------
    // From the wavefront OBJ file spec;
    // ------------------------------------------------------
    //
    // > v x y z w
    // >
    // > Polygonal and free-form geometry statement.
    // >
    // > Specifies a geometric vertex and its x y z coordinates. Rational
    // > curves and surfaces require a fourth homogeneous coordinate, also
    // > called the weight.
    // >
    // > x y z are the x, y, and z coordinates for the vertex. These are
    // > floating point numbers that define the position of the vertex in
    // > three dimensions.
    // >
    // > w is the weight required for rational curves and surfaces. It is
    // > not required for non-rational curves and surfaces. If you do not
    // > specify a value for w, the default is 1.0.
    // >
    // > NOTE: A positive weight value is recommended. Using zero or
    // > negative values may result in an undefined point in a curve or
    // > surface.
    private void processVertex(String line) {
        float[] values = StringUtils.parseFloatList(3, line, OBJ_VERTEX.length());
        builder.addVertexGeometric(values[0], values[1], values[2]);
    }

    // ------------------------------------------------------
    // From the wavefront OBJ file spec;
    // ------------------------------------------------------
    //
    // vt u v w
    //
    // Vertex statement for both polygonal and free-form geometry.
    //
    // Specifies a texture vertex and its coordinates. A 1D texture
    // requires only u texture coordinates, a 2D texture requires both u
    // and v texture coordinates, and a 3D texture requires all three
    // coordinates.
    //
    // u is the value for the horizontal direction of the texture.
    //
    // v is an optional argument.
    //
    // v is the value for the vertical direction of the texture. The
    // default is 0.
    //
    // w is an optional argument.
    //
    // w is a value for the depth of the texture. The default is 0.
    private void processVertexTexture(String line) {
        float[] values = StringUtils.parseFloatList(2, line, OBJ_VERTEX_TEXTURE.length());
        builder.addVertexTexture(values[0], values[1]);
    }

    // ------------------------------------------------------
    // From the wavefront OBJ file spec;
    // ------------------------------------------------------
    //
    // vt u v w
    //
    // Vertex statement for both polygonal and free-form geometry.
    //
    // Specifies a texture vertex and its coordinates. A 1D texture
    // requires only u texture coordinates, a 2D texture requires both u
    // and v texture coordinates, and a 3D texture requires all three
    // coordinates.
    //
    // u is the value for the horizontal direction of the texture.
    //
    // v is an optional argument.
    //
    // v is the value for the vertical direction of the texture. The
    // default is 0.
    //
    // w is an optional argument.
    //
    // w is a value for the depth of the texture. The default is 0.
    private void processVertexNormal(String line) {
        float[] values = StringUtils.parseFloatList(3, line, OBJ_VERTEX_NORMAL.length());
        builder.addVertexNormal(values[0], values[1], values[2]);
    }

    // ------------------------------------------------------
    // From the wavefront OBJ file spec;
    // ------------------------------------------------------
    //
    // Referencing groups of vertices
    //
    // Some elements, such as faces and surfaces, may have a triplet of
    // numbers that reference vertex data.These numbers are the reference
    // numbers for a geometric vertex, a texture vertex, and a vertex normal.
    //
    // Each triplet of numbers specifies a geometric vertex, texture vertex,
    // and vertex normal. The reference numbers must be in order and must
    // separated by slashes (/).
    //
    // o The first reference number is the geometric vertex.
    //
    // o The second reference number is the texture vertex. It follows
    // the first slash.
    //
    // o The third reference number is the vertex normal. It follows the
    // second slash.
    //
    // There is no space between numbers and the slashes. There may be more
    // than one series of geometric vertex/texture vertex/vertex normal
    // numbers on a line.
    //
    // The following is a portion of a sample file for a four-sided face
    // element:
    //
    // f 1/1/1 2/2/2 3/3/3 4/4/4
    //
    // Using v, vt, and vn to represent geometric vertices, texture vertices,
    // and vertex normals, the statement would read:
    //
    // f v/vt/vn v/vt/vn v/vt/vn v/vt/vn
    //
    // If there are only vertices and vertex normals for a face element (no
    // texture vertices), you would enter two slashes (//). For example, to
    // specify only the vertex and vertex normal reference numbers, you would
    // enter:
    //
    // f 1//1 2//2 3//3 4//4
    //
    // When you are using a series of triplets, you must be consistent in the
    // way you reference the vertex data. For example, it is illegal to give
    // vertex normals for some vertices, but not all.
    //
    // The following is an example of an illegal statement.
    //
    // f 1/1/1 2/2/2 3//3 4//4
    //
    // ...
    //
    // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 . . .
    //
    // Polygonal geometry statement.
    //
    // Specifies a face element and its vertex reference number. You can
    // optionally include the texture vertex and vertex normal reference
    // numbers.
    //
    // The reference numbers for the vertices, texture vertices, and
    // vertex normals must be separated by slashes (/). There is no space
    // between the number and the slash.
    //
    // v is the reference number for a vertex in the face element. A
    // minimum of three vertices are required.
    //
    // vt is an optional argument.
    //
    // vt is the reference number for a texture vertex in the face
    // element. It always follows the first slash.
    //
    // vn is an optional argument.
    //
    // vn is the reference number for a vertex normal in the face element.
    // It must always follow the second slash.
    //
    // Face elements use surface normals to indicate their orientation. If
    // vertices are ordered counterclockwise around the face, both the
    // face and the normal will point toward the viewer. If the vertex
    // ordering is clockwise, both will point away from the viewer. If
    // vertex normals are assigned, they should point in the general
    // direction of the surface normal, otherwise unpredictable results
    // may occur.
    //
    // If a face has a texture map assigned to it and no texture vertices
    // are assigned in the f statement, the texture map is ignored when
    // the element is rendered.
    //
    // NOTE: Any references to fo (face outline) are no longer valid as of
    // version 2.11. You can use f (face) to get the same results.
    // References to fo in existing .obj files will still be read,
    // however, they will be written out as f when the file is saved.
    private void processFace(String line) {
        line = line.substring(OBJ_FACE.length()).trim();
        int[] verticeIndexAry = StringUtils.parseListVerticeNTuples(line, 3);
        // String parsedList = "";
        // int loopi = 0;
        // while (loopi < verticeIndexAry.length) {
        // parsedList = parsedList + "( "+verticeIndexAry[loopi] + " / "+verticeIndexAry[loopi+1] +
        // " / "+verticeIndexAry[loopi+2] + " ) ";
        // loopi+=3;
        // }
        // System.err.println("Adding original line="+line);
        // System.err.println("Parsed as "+parsedList);
        builder.addFace(verticeIndexAry);
    }

    // @TODO: When I came back to this code after a long break (like a year?) I noticed "merging
    // groups" being
    // mentioned in the file spec - I don't recall these at all and on top of that I have no sign of
    // htem in my code.
    // buh. Going to just leave them out for now.
    //
    //
    // ------------------------------------------------------
    // From the wavefront OBJ file spec;
    // ------------------------------------------------------
    //
    // Grouping
    //
    // There are four statements in the .obj file to help you manipulate groups
    // of elements:
    //
    // o Gropu name statements are used to organize collections of
    // elements and simplify data manipulation for operations in
    // Model.
    //
    // o Smoothing group statements let you identify elements over which
    // normals are to be interpolated to give those elements a smooth,
    // non-faceted appearance. This is a quick way to specify vertex
    // normals.
    //
    // o Merging group statements are used to ideneify free-form elements
    // that should be inspected for adjacency detection. You can also
    // use merging groups to exclude surfaces which are close enough to
    // be considered adjacent but should not be merged.
    //
    // o Object name statements let you assign a name to an entire object
    // in a single file.
    //
    // All grouping statements are state-setting. This means that once a
    // group statement is set, it alpplies to all elements that follow
    // until the next group statement.
    //
    // This portion of a sample file shows a single element which belongs to
    // three groups. The smoothing group is turned off.
    //
    // g square thing all
    // s off
    // f 1 2 3 4
    //
    // This example shows two surfaces in merging group 1 with a merge
    // resolution of 0.5.
    //
    // mg 1 .5
    // surf 0.0 1.0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16
    // surf 0.0 1.0 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32
    //
    // Syntax
    //
    // g group_name1 group_name2 . . .
    //
    // Polygonal and free-form geometry statement.
    //
    // Specifies the group name for the elements that follow it. You can
    // have multiple group names. If there are multiple groups on one
    // line, the data that follows belong to all groups. Group information
    // is optional.
    //
    // group_name is the name for the group. Letters, numbers, and
    // combinations of letters and numbers are accepted for group names.
    // The default group name is default.
    //
    // s group_number
    //
    // Polygonal and free-form geometry statement.
    //
    // Sets the smoothing group for the elements that follow it. If you do
    // not want to use a smoothing group, specify off or a value of 0.
    //
    // To display with smooth shading in Model and PreView, you must
    // create vertex normals after you have assigned the smoothing groups.
    // You can create vertex normals with the vn statement or with the
    // Model program.
    //
    // To smooth polygonal geometry for rendering with Image, it is
    // sufficient to put elements in some smoothing group. However, vertex
    // normals override smoothing information for Image.
    //
    // group_number is the smoothing group number. To turn off smoothing
    // groups, use a value of 0 or off. Polygonal elements use group
    // numbers to put elements in different smoothing groups. For
    // free-form surfaces, smoothing groups are either turned on or off;
    // there is no difference between values greater than 0.
    //
    // mg group_number res
    //
    // Free-form geometry statement.
    //
    // Sets the merging group and merge resolution for the free-form
    // surfaces that follow it. If you do not want to use a merging group,
    // specify off or a value of 0.
    //
    // Adjacency detection is performed only within groups, never between
    // groups. Connectivity between surfaces in different merging groups
    // is not allowed. Surfaces in the same merging group are merged
    // together along edges that are within the distance res apart.
    //
    // NOTE: Adjacency detection is an expensive numerical comparison
    // process. It is best to restrict this process to as small a domain
    // as possible by using small merging groups.
    //
    // group_number is the merging group number. To turn off adjacency
    // detection, use a value of 0 or off.
    //
    // res is the maximum distance between two surfaces that will be
    // merged together. The resolution must be a value greater than 0.
    // This is a required argument only when using merging groups.
    //
    // ...
    //
    // Examples
    //
    // 1. Cube with group names
    //
    // The following example is a cube with each of its faces placed in a
    // separate group. In addition, all elements belong to the group cube.
    //
    // v 0.000000 2.000000 2.000000
    // v 0.000000 0.000000 2.000000
    // v 2.000000 0.000000 2.000000
    // v 2.000000 2.000000 2.000000
    // v 0.000000 2.000000 0.000000
    // v 0.000000 0.000000 0.000000
    // v 2.000000 0.000000 0.000000
    // v 2.000000 2.000000 0.000000
    // # 8 vertices
    //
    // g front cube
    // f 1 2 3 4
    // g back cube
    // f 8 7 6 5
    // g right cube
    // f 4 3 7 8
    // g top cube
    // f 5 1 4 8
    // g left cube
    // f 5 6 2 1
    // g bottom cube
    // f 2 6 7 3
    // # 6 elements
    private void processGroupName(String line) {
        String[] groupnames = StringUtils
                .parseWhitespaceList(line.substring(OBJ_GROUP_NAME.length()).trim());
        builder.setCurrentGroupNames(groupnames);
    }

    // ------------------------------------------------------
    // From the wavefront OBJ file spec;
    // ------------------------------------------------------
    //
    // o object_name
    //
    // Polygonal and free-form geometry statement.
    //
    // Optional statement; it is not processed by any Wavefront programs.
    // It specifies a user-defined object name for the elements defined
    // after this statement.
    //
    // object_name is the user-defined object name. There is no default.
    private void processObjectName(String line) {
        builder.addObjectName(line.substring(OBJ_OBJECT_NAME.length()).trim());
    }

    // ------------------------------------------------------
    // From the wavefront OBJ file spec;
    // ------------------------------------------------------
    //
    // Example
    //
    // 2. Two adjoining squares with a smoothing group
    //
    // This example shows two adjoining squares that share a common edge. The
    // squares are placed in a smoothing group to ensure that their common
    // edge will be smoothed when rendered with Image.
    //
    // v 0.000000 2.000000 0.000000
    // v 0.000000 0.000000 0.000000
    // v 2.000000 0.000000 0.000000
    // v 2.000000 2.000000 0.000000
    // v 4.000000 0.000000 -1.255298
    // v 4.000000 2.000000 -1.255298
    // # 6 vertices
    //
    // g all
    // s 1
    // f 1 2 3 4
    // f 4 3 5 6
    // # 2 elements
    //
    // 3. Two adjoining squares with vertex normals
    //
    // This example also shows two squares that share a common edge. Vertex
    // normals have been added to the corners of each square to ensure that
    // their common edge will be smoothed during display in Model and PreView
    // and when rendered with Image.
    //
    // v 0.000000 2.000000 0.000000
    // v 0.000000 0.000000 0.000000
    // v 2.000000 0.000000 0.000000
    // v 2.000000 2.000000 0.000000
    // v 4.000000 0.000000 -1.255298
    // v 4.000000 2.000000 -1.255298
    // vn 0.000000 0.000000 1.000000
    // vn 0.000000 0.000000 1.000000
    // vn 0.276597 0.000000 0.960986
    // vn 0.276597 0.000000 0.960986
    // vn 0.531611 0.000000 0.846988
    // vn 0.531611 0.000000 0.846988
    // # 6 vertices
    //
    // # 6 normals
    //
    // g all
    // s 1
    // f 1 // 1 2 // 2 3 // 3 4 // 4
    // f 4 // 4 3 // 3 5 // 5 6 // 6
    // # 2 elements
    private void processSmoothingGroup(String line) {
        line = line.substring(OBJ_SMOOTHING_GROUP.length()).trim();
        int groupNumber = 0;
        if (!line.equalsIgnoreCase("off")) {
            groupNumber = Integer.parseInt(line);
        }
        builder.setCurrentSmoothingGroup(groupNumber);
    }

    private void processPoint(String line) {
        line = line.substring(OBJ_POINT.length()).trim();
        int[] values = StringUtils.parseListVerticeNTuples(line, 1);
        builder.addPoints(values);
    }

    private void processLine(String line) {
        line = line.substring(OBJ_LINE.length()).trim();
        int[] values = StringUtils.parseListVerticeNTuples(line, 2);
        builder.addLine(values);
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
    private void processMaterialLib(String line) throws FileNotFoundException, IOException {
        String[] matlibnames = StringUtils
                .parseWhitespaceList(line.substring(OBJ_MTLLIB.length()).trim());

        if (null != matlibnames) {
            for (int loopi = 0; loopi < matlibnames.length; loopi++) {
                try {
                    parseMtlFile(m_activity, matlibnames[loopi]);
                } catch (FileNotFoundException e) {
                    log.log(SEVERE,
                            "Can't find material file name='" + matlibnames[loopi] + "', e=" + e);
                }
            }
        }
    }

    private void processUseMaterial(String line) {
        builder.setCurrentUseMaterial(line.substring(OBJ_USEMTL.length()).trim());
    }

    private void processMapLib(String line) {
        String[] maplibnames = StringUtils
                .parseWhitespaceList(line.substring(OBJ_MAPLIB.length()).trim());
        builder.addMapLib(maplibnames);
    }

    private void processUseMap(String line) {
        builder.setCurrentUseMap(line.substring(OBJ_USEMAP.length()).trim());
    }

    // ----------------------------------------------------------------------
    // material file processing
    // ----------------------------------------------------------------------
    private void parseMtlFile(Activity activity, String mtlFilename)
            throws FileNotFoundException, IOException {
        int lineCount = 0;

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(activity.getAssets().open(mtlFilename)));

        String line = null;

        while (true) {
            line = bufferedReader.readLine();
            if (null == line) {
                break;
            }

            line = line.trim();

            if (line.length() == 0) {
                continue;
            }

            if (line.startsWith("#")) // comment
            {
                continue;
            } else if (line.startsWith(MTL_NEWMTL)) {
                processNewmtl(line);
            } else if (line.startsWith(MTL_KA)) {
                processReflectivityTransmissivity(MTL_KA, line);
            } else if (line.startsWith(MTL_KD)) {
                processReflectivityTransmissivity(MTL_KD, line);
            } else if (line.startsWith(MTL_KS)) {
                processReflectivityTransmissivity(MTL_KS, line);
            } else if (line.startsWith(MTL_TF)) {
                processReflectivityTransmissivity(MTL_TF, line);
            } else if (line.startsWith(MTL_ILLUM)) {
                processIllum(line);
            } else if (line.startsWith(MTL_D)) {
                processD(line);
            } else if (line.startsWith(MTL_NS)) {
                processNs(line);
            } else if (line.startsWith(MTL_SHARPNESS)) {
                processSharpness(line);
            } else if (line.startsWith(MTL_NI)) {
                processNi(line);
            } else if (line.startsWith(MTL_MAP_KA)) {
                processMapDecalDispBump(MTL_MAP_KA, line);
            } else if (line.startsWith(MTL_MAP_KD)) {
                processMapDecalDispBump(MTL_MAP_KD, line);
            } else if (line.startsWith(MTL_MAP_KS)) {
                processMapDecalDispBump(MTL_MAP_KS, line);
            } else if (line.startsWith(MTL_MAP_NS)) {
                processMapDecalDispBump(MTL_MAP_NS, line);
            } else if (line.startsWith(MTL_MAP_D)) {
                processMapDecalDispBump(MTL_MAP_D, line);
            } else if (line.startsWith(MTL_DISP)) {
                processMapDecalDispBump(MTL_DISP, line);
            } else if (line.startsWith(MTL_DECAL)) {
                processMapDecalDispBump(MTL_DECAL, line);
            } else if (line.startsWith(MTL_BUMP)) {
                processMapDecalDispBump(MTL_BUMP, line);
            } else if (line.startsWith(MTL_REFL)) {
                processRefl(line);
            } else {
                log.log(WARNING, "line " + lineCount + " unknown line |" + line + "|");

            }
            lineCount++;
        }
        bufferedReader.close();

        log.log(INFO, "Parse.parseMtlFile: Loaded " + lineCount + " lines");
    }

    private void processNewmtl(String line) {
        line = line.substring(MTL_NEWMTL.length()).trim();
        builder.newMtl(line);
    }

    private void processReflectivityTransmissivity(String fieldName, String line) {
        int type = BuilderInterface.MTL_KA;
        if (fieldName.equals(MTL_KD)) {
            type = BuilderInterface.MTL_KD;
        } else if (fieldName.equals(MTL_KS)) {
            type = BuilderInterface.MTL_KS;
        } else if (fieldName.equals(MTL_TF)) {
            type = BuilderInterface.MTL_TF;
        }

        String[] tokens = StringUtils.parseWhitespaceList(line.substring(fieldName.length()));
        if (null == tokens) {
            log.log(SEVERE, "Got Ka line with no tokens, line = |" + line + "|");
            return;
        }
        if (tokens.length <= 0) {
            log.log(SEVERE, "Got Ka line with no tokens, line = |" + line + "|");
            return;
        }
        if (tokens[0].equals("spectral")) {
            // Ka spectral file.rfl factor_num
            log.log(WARNING,
                    "Sorry Charlie, this parse doesn't handle \'spectral\' parsing.  (Mostly because I can't find any info on the spectra.rfl file.)");
            return;
            // if(tokens.length < 2) {
            // log.log(SEVERE, "Got spectral line with not enough tokens, need at least one token
            // for spectral file and one value for factor, found "+(tokens.length-1)+" line =
            // |"+line+"|");
            // return;
            // }
        } else if (tokens[0].equals("xyz")) {
            // Ka xyz x_num y_num z_num

            if (tokens.length < 2) {
                log.log(SEVERE,
                        "Got xyz line with not enough x/y/z tokens, need at least one value for x, found "
                                + (tokens.length - 1) + " line = |" + line + "|");
                return;
            }
            float x = Float.parseFloat(tokens[1]);
            float y = x;
            float z = x;
            if (tokens.length > 2) {
                y = Float.parseFloat(tokens[2]);
            }
            if (tokens.length > 3) {
                z = Float.parseFloat(tokens[3]);
            }
            builder.setXYZ(type, x, y, z);
        } else {
            // Ka r_num g_num b_num
            float r = Float.parseFloat(tokens[0]);
            float g = r;
            float b = r;
            if (tokens.length > 1) {
                g = Float.parseFloat(tokens[1]);
            }
            if (tokens.length > 2) {
                b = Float.parseFloat(tokens[2]);
            }
            builder.setRGB(type, r, g, b);
        }
    }

    private void processIllum(String line) {
        line = line.substring(MTL_ILLUM.length()).trim();
        int illumModel = Integer.parseInt(line);
        if ((illumModel < 0) || (illumModel > 10)) {
            log.log(SEVERE,
                    "Got illum model value out of range (0 to 10 inclusive is allowed), value="
                            + illumModel + ", line=" + line);
            return;
        }
        builder.setIllum(illumModel);
    }

    // "d nnn.nn" or "d -halo nnn.nn"
    private void processD(String line) {
        line = line.substring(MTL_D.length()).trim();
        boolean halo = false;
        if (line.startsWith(MTL_D_DASHHALO)) {
            halo = true;
            line = line.substring(MTL_D_DASHHALO.length()).trim();
        }
        float factor = Float.parseFloat(line);
        builder.setD(halo, factor);
    }

    private void processNs(String line) {
        line = line.substring(MTL_NS.length()).trim();
        float exponent = Float.parseFloat(line);
        builder.setNs(exponent);
    }

    private void processSharpness(String line) {
        line = line.substring(MTL_SHARPNESS.length()).trim();
        float value = Float.parseFloat(line);
        builder.setSharpness(value);
    }

    private void processNi(String line) {
        line = line.substring(MTL_NI.length()).trim();
        float opticalDensity = Float.parseFloat(line);
        builder.setNi(opticalDensity);
    }

    // NOTE: From what I can tell, nobody ever implements these
    // options. In fact I suspect most people only implement map_Kd,
    // if even that.
    //
    // For map_Ka, map_Kd, or map_Ks the options are;
    //
    // -blendu on | off
    // -blendv on | off
    // -cc on | off
    // -clamp on | off
    // -mm base gain
    // -o u v w
    // -s u v w
    // -t u v w
    // -texres value
    //
    // For map_Ns, map_d, decal, or disp they are;
    //
    // -blendu on | off
    // -blendv on | off
    // -clamp on | off
    // -imfchan r | g | b | m | l | z
    // -mm base gain
    // -o u v w
    // -s u v w
    // -t u v w
    // -texres value
    //
    // Note the absence of -cc adn the addition of imfchan
    //
    // For bump the options are;
    //
    // -bm mult
    // -clamp on | off
    // -blendu on | off
    // -blendv on | off
    // -imfchan r | g | b | m | l | z
    // -mm base gain
    // -o u v w
    // -s u v w
    // -t u v w
    // -texres value
    //
    // Note the addition of -bm.
    private void processMapDecalDispBump(String fieldname, String line) {
        int type = BuilderInterface.MTL_MAP_KA;
        if (fieldname.equals(MTL_MAP_KD)) {
            type = BuilderInterface.MTL_MAP_KD;
        } else if (fieldname.equals(MTL_MAP_KS)) {
            type = BuilderInterface.MTL_MAP_KS;
        } else if (fieldname.equals(MTL_MAP_NS)) {
            type = BuilderInterface.MTL_MAP_NS;
        } else if (fieldname.equals(MTL_MAP_D)) {
            type = BuilderInterface.MTL_MAP_D;
        } else if (fieldname.equals(MTL_DISP)) {
            type = BuilderInterface.MTL_DISP;
        } else if (fieldname.equals(MTL_DECAL)) {
            type = BuilderInterface.MTL_DECAL;
        } else if (fieldname.equals(MTL_BUMP)) {
            type = BuilderInterface.MTL_BUMP;
        }

        String filename = line.substring(fieldname.length()).trim();
        builder.setMapDecalDispBump(type, filename);

        // @TODO: Add processing of the options...?
    }

    // ------------------------------------------------------
    // From the wavefront OBJ file spec;
    // ------------------------------------------------------
    //
    // refl -type sphere -mm 0 1 clouds.mpc
    //
    // refl -type sphere -options -args filename
    //
    // Specifies an infinitely large sphere that casts reflections onto the
    // material. You specify one texture file.
    //
    // "filename" is the color texture file, color procedural texture file, or
    // image file that will be mapped onto the inside of the shape.
    //
    // refl -type cube_side -options -args filenames
    //
    // Specifies an infinitely large sphere that casts reflections onto the
    // material. You can specify different texture files for the "top",
    // "bottom", "front", "back", "left", and "right" with the following
    // statements:
    //
    // refl -type cube_top
    // refl -type cube_bottom
    // refl -type cube_front
    // refl -type cube_back
    // refl -type cube_left
    // refl -type cube_right
    //
    // "filenames" are the color texture files, color procedural texture
    // files, or image files that will be mapped onto the inside of the shape.
    //
    // The "refl" statements for sphere and cube can be used alone or with
    // any combination of the following options. The options and their
    // arguments are inserted between "refl" and "filename".
    private void processRefl(String line) {
        String filename = null;

        int type = BuilderInterface.MTL_REFL_TYPE_UNKNOWN;
        line = line.substring(MTL_REFL.length()).trim();
        if (line.startsWith("-type")) {
            line = line.substring("-type".length()).trim();
            if (line.startsWith(MTL_REFL_TYPE_SPHERE)) {
                type = BuilderInterface.MTL_REFL_TYPE_SPHERE;
                filename = line.substring(MTL_REFL_TYPE_SPHERE.length()).trim();
            } else if (line.startsWith(MTL_REFL_TYPE_CUBE_TOP)) {
                type = BuilderInterface.MTL_REFL_TYPE_CUBE_TOP;
                filename = line.substring(MTL_REFL_TYPE_CUBE_TOP.length()).trim();
            } else if (line.startsWith(MTL_REFL_TYPE_CUBE_BOTTOM)) {
                type = BuilderInterface.MTL_REFL_TYPE_CUBE_BOTTOM;
                filename = line.substring(MTL_REFL_TYPE_CUBE_BOTTOM.length()).trim();
            } else if (line.startsWith(MTL_REFL_TYPE_CUBE_FRONT)) {
                type = BuilderInterface.MTL_REFL_TYPE_CUBE_FRONT;
                filename = line.substring(MTL_REFL_TYPE_CUBE_FRONT.length()).trim();
            } else if (line.startsWith(MTL_REFL_TYPE_CUBE_BACK)) {
                type = BuilderInterface.MTL_REFL_TYPE_CUBE_BACK;
                filename = line.substring(MTL_REFL_TYPE_CUBE_BACK.length()).trim();
            } else if (line.startsWith(MTL_REFL_TYPE_CUBE_LEFT)) {
                type = BuilderInterface.MTL_REFL_TYPE_CUBE_LEFT;
                filename = line.substring(MTL_REFL_TYPE_CUBE_LEFT.length()).trim();
            } else if (line.startsWith(MTL_REFL_TYPE_CUBE_RIGHT)) {
                type = BuilderInterface.MTL_REFL_TYPE_CUBE_RIGHT;
                filename = line.substring(MTL_REFL_TYPE_CUBE_RIGHT.length()).trim();
            } else {
                log.log(SEVERE, "unknown material refl -type, line = |" + line + "|");
                return;
            }
        } else {
            filename = line;
        }

        builder.setRefl(type, filename);
    }
}
