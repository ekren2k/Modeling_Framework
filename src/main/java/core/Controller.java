package core;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static java.lang.Double.parseDouble;


public class Controller {

    Object model;
    LinkedHashMap<String, double[]> dataMap;
    ArrayList<String> header;
    public Controller(String modelName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        model = Class.forName(modelName).getDeclaredConstructor().newInstance();
        dataMap = new LinkedHashMap<>();
    }

    public Controller readDataFrom(String fname) throws IOException {
        System.out.println(fname);
        BufferedReader br = new BufferedReader(new FileReader(fname));
        header = new ArrayList<>(Arrays.stream(br.readLine().split("\\s+")).toList());
        String currentLine;
        while ((currentLine = br.readLine()) != null) {
            ArrayList<String> line = new ArrayList<>(Arrays.stream(currentLine.split("\\s+")).toList());
            double[] values = new double[header.size()-1];
            for (int i = 1; i < line.size(); i++) {
                values[i-1] = parseDouble(line.get(i));
            }
            for (int i = 0; i < values.length; i++) {
                if (values[i] == 0d) values[i] = values[i-1];
            }
           dataMap.put(line.getFirst(),values);
        }
        br.close();
        Arrays.stream(model.getClass().getDeclaredFields()).filter(field -> field.isAnnotationPresent(Bind.class))
                .forEach(field -> {
            try {
                field.setAccessible(true);
                if (field.getName().equals("LL")) field.set(model, header.size()-1);
                else field.set(model, dataMap.get(field.getName()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        return this;
    }

    public Controller runModel() {
        Arrays.stream(model.getClass().getDeclaredMethods()).
                forEach(method -> {method.setAccessible(true);
                try {
                    method.invoke(model);
                }
            catch (IllegalAccessException | InvocationTargetException e) {e.printStackTrace();}});
        Arrays.stream(model.getClass().getDeclaredFields()).filter(field -> field.isAnnotationPresent(Bind.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    try {
                        if (field.getType().equals(double[].class)) dataMap.put(field.getName(), (double[]) field.get(model));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
        System.out.println(getResultAsTSV());
        return this;
    }

    public Controller runScriptFromFile(String fname) {
        StringBuilder script = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fname));
            String line;
            while ((line = br.readLine()) != null) {
                script.append(line).append("\n");
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        runScript(script.toString());
        return this;
    }

    public Controller runScript(String script) {
        System.out.println(script);
        Binding binding = new Binding();
        GroovyShell shell = new GroovyShell(binding);
        Arrays.stream(model.getClass().getDeclaredFields()).filter(field -> field.isAnnotationPresent(Bind.class))
                .forEach(field -> {
                        field.setAccessible(true);
                            try {
                                Object values = field.get(model);
                                binding.setVariable(field.getName(), values);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                });
        Script s = shell.parse(script);
        s.run();
        for (Object var : binding.getVariables().keySet()) {
            Object values = binding.getVariables().get(var);
            if (values instanceof double[] && var instanceof String) {
                dataMap.put((String) var, (double[]) values);
            }
        }
        return this;
    }

    public String getResultAsTSV() {
        StringBuilder result = new StringBuilder();
        for (String s : header) {
            result.append(s).append("\t");
        }
        result.append("\n");
        for (String key : dataMap.keySet()) {
            result.append(key).append("\t");
            for (double d : dataMap.get(key)) {
                result.append(d).append("\t");
            }
            result.append("\n");
        }
        return result.toString();
    }

}

