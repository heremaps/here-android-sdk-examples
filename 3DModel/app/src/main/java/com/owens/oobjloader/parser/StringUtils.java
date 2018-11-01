package com.owens.oobjloader.parser;

// This code was written by myself, Sean R. Owens, sean at guild dot net,
// and is released to the public domain. Share and enjoy. Since some
// people argue that it is impossible to release software to the public
// domain, you are also free to use this code under any version of the
// GPL, LPGL, Apache, or BSD licenses, or contact me for use of another
// license.  (I generally don't care so I'll almost certainly say yes.)
// In addition this code may also be used under the "unlicense" described
// at http://unlicense.org/ .  See the file UNLICENSE in the repo.

import java.util.*;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;

public class StringUtils {
    private static Logger log = Logger.getLogger(StringUtils.class.getName());

    // ----------------------------------------------------------------------
    // String parsing stuff
    // ----------------------------------------------------------------------
    public static void printErrMsg(String methodName, String errorMsg, int mCount, char message[]) {
        log.log(SEVERE, methodName + ": " + errorMsg);
        String msg1 = "ERROR: " + methodName + ": msg=\\";
        String msg2 = "ERROR: " + methodName + ":      ";
        for (int loopi = 0; loopi < message.length; loopi++) {
            msg1 = msg1 + message[loopi];
            msg2 = msg2 + " ";
        }
        msg1 = msg1 + "\\";
        msg2 = msg2 + "^";
        log.log(SEVERE, msg1);
        log.log(SEVERE, msg1);
    }

    // if errMsg != null, then we test if we've run past end of message
    // and if so, printErrMsg(errMsg), and return -1.  If no error then
    // we return the mCount indexing the next non-whitespace char.
    public static int skipWhiteSpace(int mCount, char messageChars[], String errMsg) {
        //Skip whitespace
        while (mCount < messageChars.length) {
            if (messageChars[mCount] == ' ' || messageChars[mCount] == '\n' || messageChars[mCount] == '\t') {
                mCount++;
            } else {
                break;
            }
        }
        if (errMsg != null) {
            if (mCount >= messageChars.length) {
                printErrMsg("RString.skipWhiteSpace", errMsg, mCount, messageChars);
                return -1;
            }
        }
        return mCount;
    }

    public static float[] parseFloatList(int numFloats, String list, int startIndex) {
        if (list == null) {
            return null;
        }
        if (list.equals("")) {
            return null;
        }

        float[] returnArray = new float[numFloats];
        int returnArrayCount = 0;

        // Copy list into a char array.
        char listChars[];
        listChars = new char[list.length()];
        list.getChars(0, list.length(), listChars, 0);
        int listLength = listChars.length;

        int count = startIndex;
        int itemStart = startIndex;
        int itemEnd = 0;
        int itemLength = 0;

        while (count < listLength) {
            // Skip any leading whitespace
            itemEnd = skipWhiteSpace(count, listChars, null);
            count = itemEnd;
            if (count >= listLength) {
                break;
            }
            itemStart = count;
            itemEnd = itemStart;
            while (itemEnd < listLength) {
                if ((listChars[itemEnd] != ' ') && (listChars[itemEnd] != '\n') && (listChars[itemEnd] != '\t')) {
                    itemEnd++;
                } else {
                    break;
                }
            }
            itemLength = itemEnd - itemStart;
            returnArray[returnArrayCount++] = Float.parseFloat(new String(listChars, itemStart, itemLength));
            if (returnArrayCount >= numFloats) {
                break;
            }

            count = itemEnd;
        }
        return returnArray;
    }

    public static int[] parseIntList(String list, int startIndex) {
        if (list == null) {
            return null;
        }
        if (list.equals("")) {
            return null;
        }

        ArrayList<Integer> returnList = new ArrayList<Integer>();

        // Copy list into a char array.
        char listChars[];
        listChars = new char[list.length()];
        list.getChars(0, list.length(), listChars, 0);
        int listLength = listChars.length;

        int count = startIndex;
        int itemStart = startIndex;
        int itemEnd = 0;
        int itemLength = 0;

        while (count < listLength) {
            // Skip any leading whitespace
            itemEnd = skipWhiteSpace(count, listChars, null);
            count = itemEnd;
            if (count >= listLength) {
                break;
            }
            itemStart = count;
            itemEnd = itemStart;
            while (itemEnd < listLength) {
                if ((listChars[itemEnd] != ' ') && (listChars[itemEnd] != '\n') && (listChars[itemEnd] != '\t')) {
                    itemEnd++;
                } else {
                    break;
                }
            }
            itemLength = itemEnd - itemStart;
            returnList.add(Integer.parseInt(new String(listChars, itemStart, itemLength)));

            count = itemEnd;
        }

        int returnArray[] = new int[returnList.size()];
        for (int loopi = 0; loopi < returnList.size(); loopi++) {
            returnArray[loopi] = returnList.get(loopi);
        }
        return returnArray;
    }

    // Note, 'face' lines are like so;
    //
    // f 11 12 13
    // f 24 25 26 27
    //
    // where the numbers are indexes of vertex (v) lines.  I'm
    // guessing they'll be mostly triangles or quads, but I think the
    // spec allows for any number of points in a face, as long as
    // there are at least three.
    //
    // Faces may also look like;
    //
    // f 11/4/1 12/5/2 13/6/3
    // f 21/14/11 22/15/12 23/16/13 24/17/14
    //
    // where the second number in each group is the index of a texture
    // (vt) line, and the third number is the index of verte normal
    // (vn) line; These lines can also leave the middle element blank
    // (but include both slashes) like so;
    //
    // f 11//1 12//2 13//3
    // f 21//11 22//12 23//13 24//14
    //
    // And lastly, I haven't seen it just yet but I've only begun
    // looking, but I'm guessing you can also leave off the last
    // element, so you'd get either;
    //
    // f 11/4 12/5 13/6
    // f 21/14 22/15 23/16 24/17
    //
    // or;
    //
    // f 11/4/ 12/5/ 13/6/
    // f 21/14/ 22/15/ 23/16/ 24/17/
    //
    // Note that in this case whatever builds the object should
    // probably compute the normal for each vertex.  How this is done
    // differs from what I've read but usually you compute the normal
    // for each face using vertices that make up the face (this is
    // _fairly_ straightforward) and then for a specific vertex you
    // average the vertices of every face that uses that vertex to get
    // the vertex normal.  (I've also seen variations that do the
    // averaging but weight it by the area of each face.)
    //
    // The spec states there is no space between number and slash (/)
    // but I'm not sure how strictly that is followed by code that
    // writes .obj files.  But I think it must be strict because if it
    // isn't, then things get way too unclear.  In fact I think you
    // couldn't parse face lines if that were the case.  Anyway, at
    // least my first version of this will assume that there are no
    // spaces between slashes and numbers.
    public static int[] parseListVerticeNTuples(String list, int expectedValuesPerTuple) {
        if (list == null) {
            return null;
        }
        if (list.equals("")) {
            return null;
        }

        //	log.log(INFO, "list=|"+list+"|");

        String[] vertexStrings = parseWhitespaceList(list);

        //	log.log(INFO, "found "+vertexStrings.length+" strings in parseWhitespaceList");

        ArrayList<Integer> returnList = new ArrayList<Integer>();
        Integer emptyMarker = new Integer(BuilderInterface.EMPTY_VERTEX_VALUE);

        for (int loopi = 0; loopi < vertexStrings.length; loopi++) {
            //	    log.log(INFO, "parsing vertexStrings["+loopi+"]=|"+vertexStrings[loopi]+"|");
            parseVerticeNTuple(vertexStrings[loopi], returnList, emptyMarker, expectedValuesPerTuple);
        }

        int returnArray[] = new int[returnList.size()];
        for (int loopi = 0; loopi < returnList.size(); loopi++) {
            returnArray[loopi] = returnList.get(loopi);
        }
        return returnArray;
    }

    private static void parseVerticeNTuple(String list, ArrayList<Integer> returnList, Integer emptyMarker, int expectedValueCount) {

//        	log.log(INFO, "list=|"+list+"|");

        String[] numbers = parseList('/', list);
//        	log.log(INFO, "found "+numbers.length+" strings in parselist with delim /");
        int foundCount = 0;

        int index = 0;
        while (index < numbers.length) {
//            	    log.log(INFO, "examining numbers["+index+"]=|"+numbers[index]+"|");
            if (numbers[index].trim().equals("")) {
//                log.log(INFO, "numbers["+index+"] is empty, adding emptymarker to list");
                returnList.add(emptyMarker);
            } else {
//                                log.log(INFO, "numbers["+index+"] is NOT empty, adding parsed int "+Integer.parseInt(numbers[index])+" to list.");
                returnList.add(Integer.parseInt(numbers[index]));
            }
            foundCount++;
            index++;
        }
        while (foundCount < expectedValueCount) {
            returnList.add(emptyMarker);
            foundCount++;
        }
    }

    public static String[] parseList(char delim, String list) {
        if (list == null) {
            return null;
        }
        if (list.equals("")) {
            return null;
        }

        ArrayList<String> returnVec = new ArrayList<String>();
        String[] returnArray = null;

        // Copy list into a char array.
        char listChars[];
        listChars = new char[list.length()];
        list.getChars(0, list.length(), listChars, 0);

        int count = 0;
        int itemStart = 0;
        int itemEnd = 0;
        String newItem = null;

        while (count < listChars.length) {
            count = itemEnd;
            if (count >= listChars.length) {
                break;
            }
            itemStart = count;
            itemEnd = itemStart;
            while (itemEnd < listChars.length) {
                if (delim != listChars[itemEnd]) {
                    itemEnd++;
                } else {
                    break;
                }
            }
            newItem = new String(listChars, itemStart, itemEnd - itemStart);
            itemEnd++;
            count = itemEnd;
            returnVec.add(newItem);
        }
        // Convert from vector to array, and return it.
        returnArray = new String[1];
        returnArray = (String[]) returnVec.toArray((Object[]) returnArray);
        return returnArray;
    }

    public static String[] parseWhitespaceList(String list) {
        if (list == null) {
            return null;
        }
        if (list.equals("")) {
            return null;
        }

        ArrayList<String> returnVec = new ArrayList<String>();
        String[] returnArray = null;

        // Copy list into a char array.
        char listChars[];
        listChars = new char[list.length()];
        list.getChars(0, list.length(), listChars, 0);

        int count = 0;
        int itemStart = 0;
        int itemEnd = 0;
        String newItem = null;

        while (count < listChars.length) {
            // Skip any leading whitespace
            itemEnd = skipWhiteSpace(count, listChars, null);
            count = itemEnd;
            if (count >= listChars.length) {
                break;
            }
            itemStart = count;
            itemEnd = itemStart;
            while (itemEnd < listChars.length) {
                if ((listChars[itemEnd] != ' ') && (listChars[itemEnd] != '\n') && (listChars[itemEnd] != '\t')) {
                    itemEnd++;
                } else {
                    break;
                }
            }
            newItem = new String(listChars, itemStart, itemEnd - itemStart);
            itemEnd++;
            count = itemEnd;
            returnVec.add(newItem);
        }
        // Convert from vector to array, and return it.
        returnArray = new String[1];
        returnArray = (String[]) returnVec.toArray((Object[]) returnArray);
        return returnArray;
    }
}