package pl.mo.planz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import pl.mo.planz.controllers.TemplateController;
import pl.mo.planz.model.TemplateModel;

@SpringBootTest
public class TemplatesTest {
    
    @Autowired
    private TemplateController templateController;

    @Test
    public void testTemplateBuild() throws TemplateParsingException {

        TemplateModel template1 = new TemplateModel();

        var inputStream = this.getClass().getResourceAsStream("/test.html");

        String templateString1 = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));

        templateController.parseTemplateAndSave(templateString1, template1);

        String templateString2 = templateController.getTemplateString(template1);

        TemplateModel template2 = new TemplateModel();
        templateController.parseTemplateAndSave(templateString2, template2);

        String templateString3 = templateController.getTemplateString(template2);

        assertEquals(templateString2, templateString3);
    }
}
