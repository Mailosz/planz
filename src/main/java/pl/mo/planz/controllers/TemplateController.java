package pl.mo.planz.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.planz.StringUtils;
import pl.mo.planz.TemplateParsingException;
import pl.mo.planz.dto.TemplateDTO;
import pl.mo.planz.dto.TemplateFieldDTO;
import pl.mo.planz.model.FieldType;
import pl.mo.planz.model.PermissionModel;
import pl.mo.planz.model.TemplateFieldModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.DatalistRepository;
import pl.mo.planz.services.AccessService;
import pl.mo.planz.services.IdentityService;
import pl.mo.planz.templates.TemplateParser;

@RestController
@CrossOrigin
public class TemplateController {

    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    DatalistRepository listRepository;

    @Autowired
    FieldRepository fieldRepository;
    
    @Autowired
    IdentityService identityService; 

    @Autowired
    AccessService accessService;
    
    @Transactional
    public void parseTemplateAndSave(String templateString, TemplateModel tm) throws TemplateParsingException {
        TemplateParser tp = new TemplateParser();
        TemplateDTO template = tp.parse(templateString);
        
        tm.setContent(template.getContent());

        List<TemplateFieldModel> oldFields = tm.getFields();
        if (oldFields != null) {
            for (var oldField : oldFields) {
                oldField.setTemplate(null);
            }
        } else {
            oldFields = new ArrayList<>();
        }


        List<TemplateFieldDTO> foundFields = template.getFields();
        List<TemplateFieldModel> newFields = new ArrayList<>(foundFields.size());
        for (var field : foundFields) {
            TemplateFieldModel tfm;
            if (field.getName() != null) {
                Optional<TemplateFieldModel> oldOpt = oldFields.stream().filter((f) -> field.getName().equals(f.getName())).findFirst();
                if (oldOpt.isPresent()) {
                    tfm = oldOpt.get();
                    oldFields.remove(oldOpt.get());
                } else {
                    tfm = new TemplateFieldModel();
                }
            } else {
                tfm = new TemplateFieldModel();
            }

            tfm.setTemplate(tm);
            tfm.setName(field.getName());
            tfm.setType(Optional.ofNullable(field.getType()).orElse(FieldType.TEXT));
            tfm.setPos(field.getPos());
            tfm.setDefaultValue(Optional.ofNullable(field.getDefaultValue()).orElse(""));
            tfm.setPublic(Optional.ofNullable(field.getIsPublic()).orElse(false));

            if (field.getEdit() != null) {
                var opt = permissionRepository.findByName(field.getEdit());
                if (opt.isPresent()) {
                    tfm.setEditPermission(opt.get());
                } else {
                    var permission = createPermission(field.getEdit());
                    tfm.setEditPermission(permission);
                    // throw new TemplateParsingException("No profile: " + field.getEdit());
                }
            } else {
                tfm.setEditPermission(null);
            }

            if (field.getList() != null) {
                var opt = listRepository.findByName(field.getList());
                if (opt.isPresent()) {
                    tfm.setDatalist(opt.get());
                } else {
                    throw new TemplateParsingException("No list: " + field.getList());
                }
            } else {
                tfm.setDatalist(null);
            }

            newFields.add(tfm);
        }

        tm.setFields(newFields);
        try {
        fieldRepository.deleteAllOrphanedFields();
        } catch (Exception ex) {
            System.out.println("Field orphan removal failed:");
            ex.printStackTrace();
        }
        
        // for (var oldField : oldFields) {
        //     try {
        //         fieldRepository.delete(oldField);
        //     } catch (Exception ex) {
        //         System.out.println("Nie udało się usunąć pola o id " + oldField.getId());
        //         ex.printStackTrace();
        //     }
        // }

        templateRepository.save(tm);
    }

    private PermissionModel createPermission(String name) {
        var permission = new PermissionModel();
        permission.setName(name);
        permission.setDescription("Uprawnienie do edycji pola '" + name + "'");

        permissionRepository.save(permission);
        return permission;
    }

    @Transactional
    @PostMapping(value="templates")
    public String postTemplate(@RequestBody String template, @RequestParam(name = "token", required = false) String token, @RequestParam(name = "name", required = false) String name) {

        //"security"
        accessService.adminOrThrow(token);
        
        TemplateModel tm = new TemplateModel();
        if (name != null) tm.setName(name);

        try {
            parseTemplateAndSave(template, tm);
        } catch (TemplateParsingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Parsing error: " + e.getDesc());
        }
        
        return tm.getId().toString();
    }

    @Transactional
    @PutMapping(value="templates/{uuid}")
    public String updateTemplate(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token, @RequestParam(name = "name", required = false) String name, @RequestBody(required = false) String content) {
        
        //"security"
        accessService.adminOrThrow(token);

        var templateOpt = templateRepository.findById(id);

        if (templateOpt.isPresent()) {
            if (name != null) templateOpt.get().setName(name);
            if (!StringUtils.isNullOrEmpty(content)) {
                try {
                    parseTemplateAndSave(content, templateOpt.get());
                } catch (TemplateParsingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Parsing error: " + e.getDesc());
                }
            }
            return "ok";
        } else {
            return "no template";
        }
    }

    @GetMapping(value="templates")
    public List<UUID> getTemplates(@RequestParam(name = "token", required = false) String token) {
        
        //"security"
        accessService.adminOrThrow(token);

        List<UUID> ids = templateRepository.getAllIds();
        
        return ids;
    }

    @GetMapping(value="templates/{uuid}")
    public String getTemplate(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token) {

        //"security"
        accessService.adminOrThrow(token);
        
        var templateOpt = templateRepository.findById(id);

        if (templateOpt.isPresent()) {
            return getTemplateString(templateOpt.get());
        } else {
            return "no template";
        }
        
    }


    public String getTemplateString(TemplateModel template) {
        List<TemplateFieldModel> fields = template.getFields();
        fields.sort((f1, f2) -> Integer.compare(f2.getPos(), f1.getPos()));

        StringBuffer templateBuffer = new StringBuffer(template.getContent());
        for (var field : fields) {

            String fieldString = getFieldString(field);

            templateBuffer.insert(field.getPos(), fieldString);
        }

        return templateBuffer.toString();
    }

    private String getFieldString(TemplateFieldModel field) {
        
        StringBuilder sb = new StringBuilder();
        sb.append('@');
        if (field.getName() != null && !field.getName().isBlank()) {
            sb.append("name=");
            sb.append(field.getName());
            sb.append("|");
        }
        sb.append("type=");
        sb.append(field.getType());
        if (field.getDatalist() != null) {
            sb.append("|list=");
            sb.append(field.getDatalist().getName());
        }
        if (field.getEditPermission() != null) {
            sb.append("|edit=");
            sb.append(field.getEditPermission().getName());
        }
        if (field.isPublic()) {
            sb.append("|public=true");
        }
        if (field.getDefaultValue() != null && !field.getDefaultValue().isBlank()) {
            sb.append("|value=");
            sb.append(field.getDefaultValue());
        }
        sb.append('@');
        return sb.toString();
    }
}
