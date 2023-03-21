package pl.mo.planz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.TemplateFieldModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.ValueListModel;

@Component
public class PageBuilder {

    static String htmlStart = loadResource("page/htmlstart.html");
    static String htmlEnd = "</body></html>";
    static String editScript = loadResource("page/editScript.html");
    static String adminScript = loadResource("page/adminScript.html");

    
    public static String loadResource(String resource) {

        InputStream inputStream = PageBuilder.class.getClassLoader().getResourceAsStream(resource);

        StringBuilder tb = new StringBuilder(10000);
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                tb.append((char) c);
            }

            return tb.toString();
        } catch (IOException e) {
            System.out.println("Error loading resource: " + resource);
            e.printStackTrace();

            return null;
        }
        
    }

    public static String buildPage(boolean isAdmin, boolean isEdit, String prev, String next, String document, DocumentModel doc, String token, TemplateRepository templateRepository) {
        
        String content = htmlStart;

        if (isAdmin) {
            content += "<div id=\"top-panel\">";
            content += "<label title=\"Udostępnij wszystkim\"><input type=\"checkbox\" id=\"showcheckbox\" class=\"switch\" " + (doc.isPublic()?"checked ":"") + "onchange=\"changePublic(event);\")>Pokaż</label>&emsp;";
            content += "<label title=\"Zezwól na edycję\"><input type=\"checkbox\" id=\"editcheckbox\" class=\"switch\" " + (doc.isEditable()?"checked ":"") + "onchange=\"changeEditable(event);\")>Edycja</label>&emsp;";
            
            List<TemplateModel> templates = templateRepository.findAll();
            content += "<label>Szablon: <select onchange=\"templateChange(event)\">";
            for (var template : templates) {
                content += "<option" + (template == doc.getTemplate()?" selected":"") + " value=\"" + template.getId().toString() + "\">" + (StringUtils.isNullOrEmpty(template.getName())?template.getId().toString():template.getName()) + "</option>";
            }
            content += "</select></label>";

            content += "</div>";
            content += adminScript;
        }

        if (prev != null) {
            content += "<a id=\"prev-button\" href=\"" + prev + "\"><div class=\"central\">&#10094;</div></a>";
        }

        if (next != null) {
            content += "<a id=\"next-button\" href=\"" + next + "\"><div class=\"central\">&#10095;</div></a>";
        }

        String className = null;
        if (isAdmin || isEdit) {
            className = "edit-mode";
        } else {
            className = "show-mode";
        }

        content += "<div id=\"doc-container\" class=\"page " + className +"\">";
        content += document;
        content += "</div>";

        if (isAdmin || isEdit) {
            content += editScript;
        }

        return content;
    }

    public static String buildDocumentForEdit(DocumentModel docModel, Map<UUID, String> valueMap, Set<String> profiles, String token) {

        String helpInputs = "<input type=\"hidden\" id=\"token-input\" value=\"" + token + "\">"
            + "<input type=\"hidden\" id=\"document-id-input\" value=\"" + docModel.getId() + "\">";

        List<TemplateFieldModel> fields = docModel.getTemplate().getFields();
        fields.sort((f1, f2) -> Integer.compare(f2.getPos(), f1.getPos()));

        boolean isAdmin = profiles.contains("admin");

        //building template
        String template = docModel.getTemplate().getContent();
        StringBuffer templateBuffer = new StringBuffer(template);
        Set<ValueListModel> datalists = new HashSet<>();
        for (var field : fields) {

            String listname;
            if (field.getDatalist() == null) {
                listname = "";
            } else {
                datalists.add(field.getDatalist());
                listname = "list=\"" + field.getDatalist().getName() + "\"";
            }

            if (field.getAutoMethod() != null) {
                templateBuffer.insert(field.getPos(), getFieldAutoValue(docModel, field.getAutoMethod()));
            } else if (!isAdmin && (field.getEditProfile() == null || !profiles.contains(field.getEditProfile().getName()))) { // just value
                var value = valueMap.get(field.getId());
                if (value == null) {
                    value = field.getDefaultValue();
                }
                templateBuffer.insert(field.getPos(), value);
            } else { // edit
                var value = valueMap.get(field.getId());
                if (value == null) {
                    value = field.getDefaultValue();
                }
                String input = String.format("<input type=\"text\" id=\"%1$s\" name=\"%2$s\" %3$s class=\"user-editable-field\" value=\"%4$s\" maxlength=\"160\" oninput=\"fieldInput(event)\" onchange=\"fieldChange(event)\">", field.getId(), field.getName(), listname, StringUtils.escapeQuotes(value));
                templateBuffer.insert(field.getPos(), input);
            }
        }

        //datalists
        StringBuilder sb = new StringBuilder(10000);
        for (var datalist : datalists) {
            if (datalist != null) {
                sb.append("<datalist id=\"" + datalist.getName() + "\">");

                for (var item : datalist.getValues()) {
                    sb.append("<option value=\"" + StringUtils.escapeQuotes(item.getValue()) + "\">");
                }
                sb.append("</datalist>");
            }
        }

        return helpInputs + sb.toString() + templateBuffer.toString();
    }

    public static String buildTemplateForView(DocumentModel docModel, Map<UUID, String> valueMap) {

        List<TemplateFieldModel> fields = docModel.getTemplate().getFields();
        fields.sort((f1, f2) -> Integer.compare(f2.getPos(), f1.getPos()));

        String template = docModel.getTemplate().getContent();
        StringBuffer templateBuffer = new StringBuffer(template);
        for (var field : fields) {


            if (field.getAutoMethod() != null) {
                templateBuffer.insert(field.getPos(), getFieldAutoValue(docModel, field.getAutoMethod()));
            } else  {
                var value = valueMap.get(field.getId());
                if (value == null) {
                    value = field.getDefaultValue();
                }
                templateBuffer.insert(field.getPos(), value);
            } 
        }

        return templateBuffer.toString();
    }


    private static String getFieldAutoValue(DocumentModel doc, String name) {
        return switch (name) {
            case "tydzien-od" -> doc.getWeek().toString();
            case "czwartek" -> doc.getWeek().plus(3, ChronoUnit.DAYS).toString();
            case "niedziela" -> doc.getWeek().plus(6, ChronoUnit.DAYS).toString();
            default -> "";
        };
    }
}
