package pl.mo.planz.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class ResourcesManager {
    static Cache<String,String> resources = new Cache<String,String>((url) -> {
        InputStream inputStream = PageBuilder.class.getClassLoader().getResourceAsStream(url);

        if (inputStream == null) {
            throw new RuntimeException("No such resource: " + url);
        }

        StringBuilder tb = new StringBuilder(10000);
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                tb.append((char) c);
            }

            return tb.toString();
        } catch (IOException e) {
            System.out.println("Error loading resource: " + url);
            e.printStackTrace();

            return null;
        }

    });


    /**
     * Loads resource or retrieves it from memory cache
     * @param url
     * @return
     */
    public static String getResource(String url) {
       return resources.get(url);
    }
}
