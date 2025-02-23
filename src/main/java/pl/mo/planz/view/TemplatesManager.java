package pl.mo.planz.view;

import java.util.HashMap;
import java.util.Map;

import pl.mo.planz.TemplateParsingException;
import pl.mo.planz.dto.TemplateDTO;
import pl.mo.planz.templates.TemplateParser;

public class TemplatesManager {

    static Cache<String,TemplateDTO> templates = new Cache<String,TemplateDTO>((url) -> {
        TemplateParser tp = new TemplateParser();

        var templateString = ResourcesManager.getResource(url);

        if (templateString == null) {
            throw new RuntimeException("No such resource: " + url);
        }

        TemplateDTO template;
        try {
            template = tp.parse(templateString);
        } catch (TemplateParsingException ex) {
            throw new RuntimeException(ex);
        }
        
        return template;
    });
    
    public static TemplateDTO getTemplate(String url) {
        return templates.get(url);
    }

}
