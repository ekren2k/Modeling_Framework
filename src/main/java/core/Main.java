package core;


import models.Model1;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Main {
public static void main(String[] args) {
        try {
            Path resourcePath = Paths.get(System.getProperty("user.dir"), "src/main/resources");
            System.out.println(Model1.class.getName());
            Controller ctl = new Controller(Model1.class.getName());

            File folder = resourcePath.toFile();


            ctl.readDataFrom(folder+"/data2.txt")
                    .runModel();
            ctl.readDataFrom(folder+"/data1.txt")
                    .runModel();
            ctl.readDataFrom(folder+"/data1.txt");
            ctl.runModel();
            ctl.readDataFrom(folder+"/data2.txt")
                    .runModel()
                    .runScriptFromFile(folder+"/script1.groovy");
            String result = ctl.getResultAsTSV();
            System.out.println(result);
    } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
    } catch (InstantiationException e) {
        throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
    } catch (IOException e) {
            throw new RuntimeException(e);
        }
}
}
