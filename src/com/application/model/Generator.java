package com.application.model;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.XML;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.util.*;

import static java.io.File.separator;

public class Generator {

    private static JSONObject finalObj = new JSONObject();

//    private static void clearDirectory(String outputDir){
//        File file = new File(outputDir);
//        String[] myFiles;
//        if(file.isDirectory()){
//            myFiles = file.list();
//            for (int i=0; i<myFiles.length; i++) {
//                File myFile = new File(file, myFiles[i]);
//                myFile.delete();
//            }
//        }
//    }

    private static void zipFolder(String zipDir,String outputDir)
    {
        ZipUtil.pack(new File(zipDir), new File(outputDir+"out.zip"));
    }

    public static void clearDirectory(String outputDir){
        File filename = new File(outputDir);

        try {
            FileUtils.cleanDirectory(filename);
        }
        catch (IOException e){
            System.out.println("error in clearing directory");
        }
    }

    /** This function used to convert xml to string **/
    private static String Xml2String(String fileName) {
        String XmlData="";
        try {
            /* Start of Reading the data as String in the XML file */
            String filePath = System.getProperty("user.dir")+ separator+"conf"+ separator+"XML File"+ separator+fileName+".xml";
            File file = new File(filePath);
            Reader fileReader = new FileReader(file);

            BufferedReader bufReader = new BufferedReader(fileReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufReader.readLine();

            while( line != null)
            {
                stringBuilder.append(line).append("\n");
                line = bufReader.readLine();
            }

            bufReader.close();

            /* Reading the XML file completed */

            /* XML data are converted to String and Stored at the xml2String variable*/
            XmlData = stringBuilder.toString();


        }
        catch (Exception error) {
            System.out.println(error);
        }

        /* The XmlString is returned */
        return XmlData;

    }

    /** This function is used to check whether input in JSONArray else it typecast to JSONArray **/
    private static JSONArray checkJsonArray(Object tempObj) {
        if(tempObj instanceof JSONObject) {
            JSONArray temp = new JSONArray();
            temp.add(tempObj);
            return temp;
        }
        return (JSONArray)tempObj;
    }

    /** This function create key-value pair like "jsonTemplate name"--"key in jsonTemplate"  :: Refer the jsoninputObj**/
    private static Map<String,JSONArray> getJsonFromTemplate(JSONObject inputObj) {
        JSONArray jsonTemplate = (JSONArray) ( (JSONObject) ( (JSONObject) inputObj.get("security") ).get("jsontemplates") ).get("jsontemplate");
        Map<String,JSONArray> template = new HashMap<>();
        for(int itr=0; itr<jsonTemplate.size(); itr++) {
            JSONObject tempObj = (JSONObject) jsonTemplate.get(itr);
            template.put((String) tempObj.get("name"), checkJsonArray(tempObj.get("key"))  );
        }
        return template;
    }

    /** It is used to update the template object using JsonTemplate Map object generated from "getJsonFromTemplate" function **/
    private static void updateTemplateJsonTemplate(String streamTemplate, Map<String,JSONArray> jsonTemplate, JSONObject template, Set<String> tempSet) {
        JSONArray key = jsonTemplate.get(streamTemplate);
        for(int itr=0; itr<key.size(); itr++) {
            JSONObject keyObj = (JSONObject)key.get(itr);
            String type = (String) keyObj.get("type");
            if(type==null) {
                template.put(keyObj.get("name"), keyObj.get("regex"));
                System.out.println("I am null then "+keyObj.get("regex"));
                tempSet.add((String) keyObj.get("regex"));
            }
            else if(type.contentEquals("JSONArray") || type.contentEquals("JSONObject")) {
                updateTemplateJsonTemplate((String) keyObj.get("template"), jsonTemplate, template, tempSet);
            }
            else {
                template.put(keyObj.get("name"), type);
                tempSet.add(type);
            }
        }
    }

    /** It is used to update the template object for param attributes **/
    private static void updateTemplateParam( JSONObject template, JSONArray param, Set<String> tempSet) {
        for(int itr=0; itr<param.size(); itr++) {
            String key = (String) ( (JSONObject) param.get(itr) ).get("name");
            String value;
            value = (String) ( (JSONObject) param.get(itr) ).get("type");
            if(value==null) {
                value = (String) ( (JSONObject) param.get(itr) ).get("regex");
            }
            System.out.println("value : "+value);
//            System.out.println("TemplateParam");
//            System.out.println(key+" ----------- "+value);
            template.put(key,value);
            tempSet.add(value);
        }
    }

    /** This function is used to update the template object for inputstream attributers **/
    private static void updateTemplateInputStream(JSONObject template, JSONArray inputStream, Map<String,JSONArray> jsonTemplate, Set<String> tempSet) {

        for(int itr=0; itr<inputStream.size(); itr++) {
            String streamTemplate = (String) ( (JSONObject) inputStream.get(itr) ).get("template");
            String type = (String) ( (JSONObject) inputStream.get(itr) ).get("type");
//            System.out.println("TemplateInputStream");
//            System.out.println(type+" ------------ "+streamTemplate);
            updateTemplateJsonTemplate(streamTemplate, jsonTemplate, template, tempSet);
        }
    }

    /** This is the main function to update the template object **/
    private static void updateTemplate(String key, JSONObject template, JSONObject urlArrayObj, Map<String,JSONArray> jsonTemplate, Set<String> tempSet) {
        try {
            Object obj = urlArrayObj.get(key);     /** It will result in error if the key is not available **/
            if(key=="param") {
                JSONArray param = checkJsonArray(urlArrayObj.get(key));
                updateTemplateParam(template,param,tempSet);
            }
            else if(key=="inputstream") {
                JSONArray inputStream = checkJsonArray(urlArrayObj.get(key));
                updateTemplateInputStream(template,inputStream,jsonTemplate,tempSet);
            }
        }
        catch (Exception e) {
        }
    }

    /** The main execution starts from this function **/
    private static void createCases(JSONObject inputObj, String outputDir) {

        /** If there is only one "urls" then it will be JSONObject instead of JSONArray so we need to convert it**/
        JSONArray urls = checkJsonArray( ( (JSONObject) inputObj.get("security") ).get("urls") );

        Set<String> tempSet = new HashSet<>();

        /** Iterating main url **/
        for (int urlsItr = 0; urlsItr < urls.size(); urlsItr++) {
            JSONObject urlsObj = (JSONObject) urls.get(urlsItr);

            /** Storing urlsObj key-value pair **/
            String prefix = (String) urlsObj.get("prefix");
            //System.out.println(prefix+"\n");
            prefix = prefix.replaceAll("/","_");
            new File(outputDir+separator+prefix).mkdirs();

            /** Similarly if there is only one "url" then it will be JSONObject we need to convert it into JSONArray **/
            JSONArray urlArray = checkJsonArray( urlsObj.get("url") );

            /** Iterating sub url with info **/
            for (int urlArrayItr = 0; urlArrayItr < urlArray.size(); urlArrayItr++) {
                LinkedHashMap jo = new LinkedHashMap();
                JSONObject urlArrayObj = (JSONObject) urlArray.get(urlArrayItr);

                /** Storing urlArrayObj key-value pair **/
                String path = (String) urlArrayObj.get("path");
                String method = (String) urlArrayObj.get("method");

                /** Creating template for all parameters **/
                JSONObject template = new JSONObject();

                System.out.println(path+" ----- "+method);
                System.out.println(template);
                Map<String,JSONArray> jsonTemplate = getJsonFromTemplate(inputObj);
                updateTemplate("param",template,urlArrayObj,jsonTemplate,tempSet);
                updateTemplate("inputstream",template,urlArrayObj,jsonTemplate,tempSet);
                //System.out.println(template);
                System.out.println("\n");
                if(template.size()!=0) {
                    //finalObj.put(path,new JSONObject().put("Temlate",template));
                    jo.put("Link", path);
                    jo.put("Template", template);
                    jo.put("TestCases", new JSONArray());
                    TestCaseGenerator.newObject = true;
                    TestCaseGenerator.createTestCases(jo,path,outputDir+separator+prefix,method);
                }
            }
        }
    }

//    private static JSONObject convertToJson(String xml_data) {
//        try
//        {
//            XmlMapper xmlMapper = new XmlMapper();
//            JsonNode jsonNode = xmlMapper.readTree(xml_data.getBytes());
//            ObjectMapper objectMapper = new ObjectMapper();
//            String value = objectMapper.writeValueAsString(jsonNode);
//
//
//            JSONParser parser = new JSONParser();
//            JSONObject json = (JSONObject) parser.parse(value);
//            return json;
//        } catch (Exception e)
//        {
//            e.printStackTrace();
//            return null;
//        }
//    }
    public static void main(String...s){
        String xml_data = Xml2String("security-inventory");
        String jsonString = XML.toJSONObject(xml_data).toString();
        JSONObject inputObj = new JSONObject();
        try {
            JSONParser parser = new JSONParser();
            inputObj = (JSONObject) parser.parse(jsonString);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(xml_data);
        //converting xml to json
        //JSONObject inputObj = convertToJson(xml_data);
        System.out.println(inputObj);
        String outputDir = System.getProperty("user.dir")+separator+"out"+separator+"myFiles";
        clearDirectory(outputDir);
        createCases(inputObj,outputDir);
        zipFolder(System.getProperty("user.dir")+separator+"out"+separator+"myFiles"+separator,System.getProperty("user.dir")+separator+"out"+separator+"zipDirectory"+separator);

    }
}
