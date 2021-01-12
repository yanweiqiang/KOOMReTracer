package com.myth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.regex.Pattern;

public class OOMReportReTracer {
    private final ReTrace reTrace;

    public OOMReportReTracer(String mappingFilePath) {
        reTrace = new ReTrace();
        reTrace.pump(mappingFilePath);
    }

    public String retrace(String report) {
        JSONObject jsonObject = JSON.parseObject(report);
        JSONArray gcPaths = jsonObject.getJSONArray("gcPaths");
        for (int i = 0; i < gcPaths.size(); i++) {
            JSONArray paths = gcPaths.getJSONObject(i).getJSONArray("path");
            for (int j = 0; j < paths.size(); j++) {
                JSONObject path = paths.getJSONObject(j);
                String declaredClass = path.getString("declaredClass");
                String originDeclaredClass = reTrace.getClassName(declaredClass);

                if (originDeclaredClass == null) {
                    continue;
                }

                String reference = path.getString("reference");
                int index = reference.lastIndexOf('.');
                String refClass = reference.substring(0, index);
                String originRefClass = reTrace.getClassName(refClass);
                String refFieldName = reference.substring(index + 1);
                String originRefFieldName = reTrace.getFieldName(originRefClass, refFieldName);

                if (originRefFieldName == null) {
                    originRefFieldName = reTrace.getFieldName(originDeclaredClass, refFieldName);
                }

                String originReference = originRefClass + "." + originRefFieldName;
                System.out.println("declared class:\n" + declaredClass + "\n" + originDeclaredClass);
                System.out.println("reference:\n" + reference + "\n" + originReference);

                for (int k = 0; k < 8; k++) {
                    System.out.print("----");
                }

                System.out.println();

                path.put("declaredClass", originDeclaredClass);
                path.put("reference", originReference);
            }
        }
        return JSON.toJSONString(jsonObject);
    }
}
