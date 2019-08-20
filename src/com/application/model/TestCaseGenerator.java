package com.application.model;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang3.SerializationUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;

public class TestCaseGenerator {

    public static boolean newObject = false;

    private static JSONObject getObject(String path) {
        try {
            Object obj = new JSONParser().parse(new FileReader(path));
            return (JSONObject) obj;
        } catch (Exception e) {
            System.out.println("error " + e);
        }
        return null;
    }

    public static void createTestCases(LinkedHashMap jo, String link,String outputDir,String method){
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("user.dir")+separator+"conf"+separator+"testJSON"+separator+"newTestCase.json";
        JSONObject testCase = getObject(path);
        JSONObject properties = (JSONObject)jo.get("Template");

        System.out.println("path : "+link);
        System.out.println("properties : "+properties);
        Set<String> keySet = properties.keySet();
        ArrayList<String> propKeys = new ArrayList<>(keySet);

        Collection<String> values = properties.values();
        ArrayList<String>  propValues= new ArrayList<>(values);

        ArrayList<ArrayList> validDataTypes = new ArrayList<>();
        ArrayList<ArrayList> invalidDataTypes = new ArrayList<>();
        int validElementCombination = 1;
        int invalidElementCombination = 0;
        System.out.println("propvalues : "+propValues);
        for(String val : propValues){
            //System.out.println("TestCase : "+testCase);
            //System.out.println("val : "+val);
            System.out.println("val : "+val);
            JSONObject obj = (JSONObject)testCase.get(val);
            System.out.println("obj : "+obj);
            validElementCombination *= ((JSONArray) obj.get("validInput")).size();
            validDataTypes.add( (JSONArray) ((JSONArray) obj.get("validInput")).clone());
            invalidElementCombination += ((JSONArray) obj.get("invalidInput")).size();
            invalidDataTypes.add( (JSONArray) ((JSONArray) obj.get("invalidInput")).clone());
        }
        if(validElementCombination>10000){
            validElementCombination=10000;
        }
        if(invalidElementCombination>10000){
            invalidElementCombination = 10000;
        }
        LinkedHashMap validCombination = getValidCombinations(validDataTypes,validElementCombination,propKeys,propValues,testCase,jo,link,outputDir,method);
        getInValidCombinations(invalidDataTypes,invalidElementCombination,propKeys,validCombination,jo,link,outputDir,method);
        propKeys.clear();
        propValues.clear();

    }

    private static LinkedHashMap getValidCombinations(ArrayList validDataTypes, int validElementCombination, ArrayList propKeys, ArrayList propValues, JSONObject testCase,LinkedHashMap jo,String link,String outputDir,String method){

        int n = validDataTypes.size();
        int[] indices = new int[n];

        for (int i = 0; i < n; i++) {
            indices[i] = 0;
        }

        long noOfElements = 0;
        JSONArray allCombination = new JSONArray();
        LinkedHashMap validCombination = new LinkedHashMap();
        while (true) {
            LinkedHashMap combination = new LinkedHashMap();
            combination.put("testDataMeta",new JSONObject());
            combination.put("testData",new JSONObject());
            ( (JSONObject)combination.get("testDataMeta") ).put("isValidInput",true);
            for (int i = 0; i < n; i++) {
                //combination.put(propKeys.get(i), ((JSONArray) validDataTypes.get(i)).get(indices[i]));
                ( (JSONObject)combination.get("testData") ).put(propKeys.get(i), ((JSONArray) validDataTypes.get(i)).get(indices[i]));
            }

            allCombination.add(combination);
            validCombination = combination;
            noOfElements++;
            if(noOfElements == validElementCombination){
                writeJSON(jo,link,allCombination,outputDir,method);
                allCombination.clear();
                noOfElements = 0;
            }

            int next = n - 1;
            while (next >= 0 && (indices[next] + 1 >= ((JSONArray) validDataTypes.get(next)).size())) {
                next--;
            }
            if (next < 0) {
                break;
            }
            indices[next]++;
            for (int i = next + 1; i < n; i++) {
                indices[i] = 0;
            }
        }
        if(noOfElements!=0){
            writeJSON(jo,link,allCombination,outputDir,method);
        }
        return validCombination;
    }

    private static void getInValidCombinations(ArrayList invalidDataTypes, int invalidElementCombination, ArrayList propKeys, LinkedHashMap validCombination,LinkedHashMap jo,String link,String outputDir,String method){
        JSONArray allCombination = new JSONArray();
        ((JSONObject) validCombination.get("testDataMeta")).put("isValidInput",false);
        System.out.println("invalidDataTypes : "+invalidDataTypes.toString());
        long noOfElements = 0;
        for(int i =0;i<invalidDataTypes.size();i++){
            LinkedHashMap validTestCase = SerializationUtils.clone(validCombination);
            ((JSONObject) validTestCase.get("testDataMeta")).put("invalidParameter",propKeys.get(i));
            for(int j=0;j<((JSONArray)invalidDataTypes.get(i)).size();j++) {
                LinkedHashMap cloneValidTestCase = SerializationUtils.clone(validTestCase);
                ((JSONObject) cloneValidTestCase.get("testData")).put(propKeys.get(i), ((JSONArray)invalidDataTypes.get(i)).get(j));
                allCombination.add(cloneValidTestCase);
                noOfElements++;
                if(noOfElements == invalidElementCombination){
                    writeJSON(jo,link,allCombination,outputDir,method);
                    allCombination.clear();
                    noOfElements = 0;
                }
            }
        }
        if(noOfElements!=0){
            writeJSON(jo,link,allCombination,outputDir,method);
        }

    }

    private static void writeJSON(LinkedHashMap obj,String link,JSONArray combination,String outputDir,String method){
        String separator = System.getProperty("file.separator");
        //System.out.println("Entered write");
        ObjectMapper mapperMain=new ObjectMapper();
        ObjectWriter writer = mapperMain.writer(new DefaultPrettyPrinter());
        link = link.replaceAll("/","_");
        File fileName = new File(outputDir+separator+link+"-"+method+".json");

        String element =  combination.toJSONString();
        element = element.substring(1,element.length()-1);

        try {
            if(!fileName.exists()){
                writer.writeValue(new FileWriter(fileName,false),obj);
                //newFile = true;
            }
            else if(newObject){
                //newObject = false;
                writer.writeValue(new FileWriter(fileName,true),obj);
            }
            RandomAccessFile randomAccessFile = new RandomAccessFile(fileName, "rw");
            long pos = randomAccessFile.length();
            while (randomAccessFile.length() > 0) {
                pos--;
                randomAccessFile.seek(pos);
                if (randomAccessFile.readByte() == ']') {
                    //System.out.println("Found it");
                    randomAccessFile.seek(pos);
                    break;
                }
            }
            if(newObject){
                newObject = false;
                randomAccessFile.writeBytes(element+"]\n}");
            }
            else {
                randomAccessFile.writeBytes(",\n\t\t" + element + "]\n}");
            }
            randomAccessFile.close();

        }
        catch(FileNotFoundException e){
            System.out.println("error : Filenotfound in writeJSON"+e);
        }
        catch (IOException e){
            System.out.println("error : IOException in writeJSON");
        }

        //System.out.println("Going to the end");
    }
}
