package com.application.model;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

public class Generator {

    private static JSONObject finalObj = new JSONObject();

    private static String Xml2String(String fileName)
    {
        String XmlData="";
        try {
            /* Start of Reading the data as String in the XML file */
            String filePath = System.getProperty("user.dir")+ File.separator+"conf"+File.separator+"XML File"+File.separator+fileName+".xml";
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

    private static JSONArray checkJsonArray(Object tempObj) {
        if(tempObj instanceof JSONObject) {
            return new JSONArray().put(tempObj);
        }
        return (JSONArray)tempObj;
    }

    private static void updateTemplateParam( JSONObject template, JSONArray param) {
        for(int itr=0; itr<param.length(); itr++) {
            String key = (String) ( (JSONObject) param.get(itr) ).get("name");
            String value;
            try {
                value = (String) ( (JSONObject) param.get(itr) ).get("type");
            }
            catch (Exception e) {
                value = (String) ( (JSONObject) param.get(itr) ).get("regex");
            }
            System.out.println("TemplateParam");
            System.out.println(key+" ----------- "+value);
        }
    }

    private static void updateTemplateInputStream(JSONObject template, JSONArray inputStream) {
        for(int itr=0; itr<inputStream.length(); itr++) {
            String jsonTemplate = (String) ( (JSONObject) inputStream.get(itr) ).get("template");
            String type = (String) ( (JSONObject) inputStream.get(itr) ).get("type");
            System.out.println("TemplateInputStream");
            System.out.println(type+" ------------ "+jsonTemplate);
        }
    }

    private static void updateTemplate(String key, JSONObject template, JSONObject urlArrayObj) {
        try {
            Object obj = urlArrayObj.get(key);     /** It will result in error if the key is not available **/
            if(key=="param") {
                JSONArray param = checkJsonArray(urlArrayObj.get(key));
                updateTemplateParam(template,param);
            }
            else if(key=="inputstream") {
                JSONArray inputStream = checkJsonArray(urlArrayObj.get(key));
                updateTemplateInputStream(template,inputStream);
            }
        }
        catch (Exception e) {
        }
    }

    private static void createCases(JSONObject inputObj) {

        /** If there is only one "urls" then it will be JSONObject instead of JSONArray so we need to convert it**/
        JSONArray urls = checkJsonArray( ((JSONObject) inputObj.get("security")).get("urls") );

        /** Iterating main url **/
        for (int urlsItr = 0; urlsItr < urls.length(); urlsItr++) {
            JSONObject urlsObj = (JSONObject) urls.get(urlsItr);

            /** Storing urlsObj key-value pair **/
            String prefix = (String) urlsObj.get("prefix");
            System.out.println(prefix);

            /** Similarly if there is only one "url" then it will be JSONObject we need to convert it into JSONArray **/
            JSONArray urlArray = checkJsonArray( urlsObj.get("url") );

            /** Iterating sub url with info **/
            for (int urlArrayItr = 0; urlArrayItr < urlArray.length(); urlArrayItr++) {
                JSONObject urlArrayObj = (JSONObject) urlArray.get(urlArrayItr);

                /** Storing urlArrayObj key-value pair **/
                String path = (String) urlArrayObj.get("path");
                String method = (String) urlArrayObj.get("method");

                /** Creating template for all parameters **/
                JSONObject template = new JSONObject();
                System.out.println(path+" ------ "+method);
                updateTemplate("param",template,urlArrayObj);
                updateTemplate("inputstream",template,urlArrayObj);
                System.out.println("\n\n");
            }
        }
    }

    public static void main(String...s){
        String xml_data = Xml2String("security-inventory");

        //converting xml to json
        JSONObject inputObj = XML.toJSONObject(xml_data);

        createCases(inputObj);

    }
}
