package pl.mo.planz.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.mo.planz.StringUtils;
import pl.mo.planz.TemplateParser;
import pl.mo.planz.TemplateParsingException;
import pl.mo.planz.dto.DocumentDTO;
import pl.mo.planz.dto.DocumentResponseDTO;
import pl.mo.planz.dto.FieldDeclarationDTO;
import pl.mo.planz.dto.HistoryItemDTO;
import pl.mo.planz.dto.TemplateFieldDTO;
import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.FieldType;
import pl.mo.planz.model.FieldValueHistoryModel;
import pl.mo.planz.model.FieldValueModel;
import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.TemplateFieldModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.DatalistModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.DatalistRepository;
import pl.mo.planz.services.AccessObject;
import pl.mo.planz.services.AccessService;
import pl.mo.planz.services.IdentityService;
import pl.mo.planz.view.PageBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.chrono.ChronoPeriod;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;


@RestController
@CrossOrigin
public class FieldsController {

    public static UUID currentDocumentId;
    
    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    FieldValueRepository fieldValueRepository;

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    DatalistRepository listRepository;

    @Autowired
    FieldRepository fieldRepository;

    @Autowired
    IdentityRepository identityRepository;

    @Autowired
    FieldValueHistoryRepository historyRepository;

    @Autowired
    IdentityService identityService;

    @Autowired
    AccessService accessService;
    


    @PutMapping(value="values/{docId}/{fieldId}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @ResponseStatus(code = HttpStatus.OK)
    public void updateFieldValue(@PathVariable("docId") UUID docId, @PathVariable("fieldId") UUID fieldId, @RequestParam("token") String token, @RequestBody(required = false) String value) {

        AccessObject access = accessService.getAccess(token);

        var identity = identityRepository.findFromToken(token);
        if (!identity.isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Set<String> userPermissions = access.getPermissions();


        //TODO: get list and remove duplicates
        var opt = fieldValueRepository.findByDocAndField(docId, fieldId);
        
        FieldValueModel model;
        FieldValueHistoryModel fvh = null;
        if (opt.isPresent()) {
            model = opt.get();

            //saving history of edits
            fvh = new FieldValueHistoryModel();
            fvh.setField(model);
            fvh.setValue(model.getValue());
            fvh.setEditIdentity(model.getEditIdentity());
            fvh.setEditTime(model.getEditTime());

            
        } else {
            var doc = documentRepository.findById(docId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No document"));
            var field = fieldRepository.findById(fieldId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No field"));
            
            model = newFieldValueModel(doc, field);
        }
        
        if (!userPermissions.contains("admin")) {
            if (!model.getDocument().isEditable() || model.getField().getEditPermission() == null || !userPermissions.contains(model.getField().getEditPermission().getName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }


        if (fvh != null) { // save history
            historyRepository.save(fvh);
        }

        if (value == null) {
            value = "";
        }

        model.setValue(value);
        model.setEditTime(Instant.now());
        model.setEditIdentity(identity.get());

        fieldValueRepository.save(model); // save value

        if (model.getDocument().isEditable() && model.getDocument().getGeneratedContent() != null) {
            model.getDocument().setGeneratedContent(null);
            model.getDocument().setGeneratedTime(null);
            documentRepository.save(model.getDocument());
        }
    }

    private FieldValueModel newFieldValueModel(DocumentModel doc, TemplateFieldModel field) {

        var model = new FieldValueModel();
        model.setType(field.getType());
        model.setDocument(doc);
        model.setField(field);

        return model;
    }

    @GetMapping(value="fields/{docId}/{fieldId}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FieldDeclarationDTO getFieldDeclaration(@PathVariable("docId") UUID docId, @PathVariable("fieldId") UUID fieldId, @RequestParam("token") String token) {

        accessService.adminOrThrow(token);

        var opt = fieldValueRepository.findByDocAndField(docId, fieldId);
        
        FieldValueModel model;
        if (opt.isPresent()) {
            model = opt.get();
        } else {
            var doc = documentRepository.findById(docId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No document"));
            var field = fieldRepository.findById(fieldId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No field"));

            model = newFieldValueModel(doc, field);
        }

        FieldDeclarationDTO dto = new FieldDeclarationDTO();
        dto.setName(model.getField().getName());
        dto.setType(model.getType());
        dto.setValue(model.getValue());
        dto.setDefaultValue(model.getField().getDefaultValue());
        if (model.getField().getEditPermission() != null) {
            dto.setEditPermission(model.getField().getEditPermission().getName());
            dto.setEligibleEditors(permissionRepository.findIdentitiesWithPermission(model.getDocument().getSeries(), model.getField().getEditPermission()).stream().map((identity) -> identity.getName()).toList());
        }
        if (model.getField().getDatalist() != null) {
            dto.setDatalist(model.getField().getDatalist().getName());
        }

        return dto;
    }

    @PutMapping(value="fields/{docId}/{fieldId}", consumes = {"application/json"})
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @ResponseStatus(code = HttpStatus.OK)
    public void updateFieldDeclaration(@PathVariable("docId") UUID docId, @PathVariable("fieldId") UUID fieldId, @RequestParam("token") String token, @RequestBody(required = true) FieldDeclarationDTO opts) {

        accessService.adminOrThrow(token);


        var opt = fieldValueRepository.findByDocAndField(docId, fieldId);
        
        FieldValueModel model;
        if (opt.isPresent()) {
            model = opt.get();

            //TODO: saving history of edits

        } else {
            var doc = documentRepository.findById(docId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No document"));
            var field = fieldRepository.findById(fieldId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No field"));

            model = newFieldValueModel(doc, field);
        }

        //change
        if (opts.getType() != null) {
            model.setType(opts.getType());
        }
        if (opts.getValue() != null) {
            model.setValue(opts.getValue());
        }

        fieldValueRepository.save(model);

        if (model.getDocument().getGeneratedContent() != null) {
            model.getDocument().setGeneratedContent(null);
            documentRepository.save(model.getDocument());
        }
    }


    @GetMapping(value="history/{docId}/{fieldId}")
    public List<HistoryItemDTO> getHistory(@PathVariable("docId") UUID docId, @PathVariable("fieldId") UUID fieldId, @RequestParam("token") String token) {

        accessService.adminOrThrow(token);

        var value = fieldValueRepository.findByDocAndField(docId, fieldId);
        if (value.isPresent()) {

            //history
            List<FieldValueHistoryModel> items = historyRepository.getHistoryForField(value.get().getId());

            DateTimeFormatter timeFormattter = DateTimeFormatter.ofPattern("YYYY.MM.dd HH:mm:ss");
    
            List<HistoryItemDTO> dtos = items.stream().map((item) -> {
                HistoryItemDTO dto = new HistoryItemDTO();
                dto.setValue(item.getValue());
                dto.setEditor(item.getEditIdentity().getName());
                dto.setTime(item.getEditTime().atZone(ZoneId.systemDefault()).format(timeFormattter));
    
                return dto;
            }).collect(Collectors.toList());

            // add current value
            HistoryItemDTO currentValue = new HistoryItemDTO();
            currentValue.setValue(value.get().getValue());
            currentValue.setEditor(value.get().getEditIdentity().getName());
            currentValue.setTime(value.get().getEditTime().atZone(ZoneId.systemDefault()).format(timeFormattter));

            dtos.add(currentValue); 

            return dtos;
        } else {
            return new ArrayList<>(); 
        }


    }
}
