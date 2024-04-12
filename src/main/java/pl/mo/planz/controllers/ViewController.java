package pl.mo.planz.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.PathParam;
import pl.mo.planz.DocumentGenerator;
import pl.mo.planz.StringUtils;
import pl.mo.planz.TemplateParser;
import pl.mo.planz.TemplateParsingException;
import pl.mo.planz.dto.DocumentDTO;
import pl.mo.planz.dto.DocumentResponseDTO;
import pl.mo.planz.dto.FieldDeclarationDTO;
import pl.mo.planz.dto.TemplateFieldDTO;
import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.FieldType;
import pl.mo.planz.model.FieldValueHistoryModel;
import pl.mo.planz.model.FieldValueModel;
import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.model.TemplateFieldModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.TokenModel;
import pl.mo.planz.model.ValueListModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.ValueListRepository;
import pl.mo.planz.services.AccessObject;
import pl.mo.planz.services.AccessService;
import pl.mo.planz.services.DocumentService;
import pl.mo.planz.services.IdentityService;
import pl.mo.planz.view.PageBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.chrono.ChronoPeriod;
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
public class ViewController {

    public static Map<UUID,UUID> currentDocumentMap = new HashMap<>();
    
    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    FieldValueRepository fieldValueRepository;

    @Autowired
    PermissionRepository profileRepository;

    @Autowired
    ValueListRepository listRepository;

    @Autowired
    FieldRepository fieldRepository;

    @Autowired
    IdentityRepository identityRepository;

    @Autowired
    FieldValueHistoryRepository historyRepository;

    @Autowired
    IdentityService identityService;

    @Autowired
    SeriesRepository seriesRepository;    

    @Autowired
    PageBuilder pageBuilder;

    @Autowired
    AccessService accessService;

    @Autowired
    DocumentService documentService;



    @GetMapping(value="/create/{token}")
    public String createDocumentForSeries(@PathVariable("token") String token) {

        AccessObject access = accessService.getAccess(token, "admin");


        return pageBuilder.buildCreatePage(access.getSeries(), access.getToken().getValue());
    }


    private String openDocument(Optional<DocumentModel> docOpt, AccessObject access, ViewMode viewMode) {

        if (!docOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return createView(docOpt.get(), access, viewMode);
    }

    @GetMapping(value="/view/{token}")
    public String openCurrentDocumentForView(@PathVariable(name = "token") String token) {

        AccessObject access = accessService.getAccess(token, "view");
        return openDocument(getCurrentDocument(access.getSeries()), access, ViewMode.View);
    }

    @GetMapping(value="/edit/{token}")
    public String openCurrentDocumentForEdit(@PathVariable(name = "token") String token) {

        AccessObject access = accessService.getAccess(token, "edit");
        return openDocument(getCurrentDocument(access.getSeries()), access, ViewMode.Edit);
    }

    @GetMapping(value="/view/{token}/{docId}")
    public String openDocumentForView(@PathVariable(name = "token") String token, @PathVariable(name = "docId") UUID docId) {

        AccessObject access = accessService.getAccess(token, "view");
        return openDocument(documentRepository.findById(docId).or(()-> getCurrentDocument(access.getSeries())), access, ViewMode.View);
    }

    @GetMapping(value="/edit/{token}/{docId}")
    public String openDocumentForEdit(@PathVariable(name = "token") String token, @PathVariable(name = "docId") UUID docId) {

        AccessObject access = accessService.getAccess(token, "edit");
        return openDocument(documentRepository.findById(docId).or(()-> getCurrentDocument(access.getSeries())), access, ViewMode.Edit);
    }


    private Optional<DocumentModel> getCurrentDocument(SeriesModel series) {

        UUID currentUuid = currentDocumentMap.get(series.getId());
        if (currentUuid == null) {
            var opt = documentService.findCurrentDocument(series);
            if (opt.isPresent()) {
                currentDocumentMap.put(series.getId(), opt.get().getId());
            }
            return opt;
        } else {
            var opt = documentRepository.findById(currentUuid);
            if (!opt.isPresent()) {
                currentDocumentMap.put(series.getId(), null);
                return getCurrentDocument(series);
            }
            DocumentModel doc = opt.get();

            if (doc.getNext() != null && LocalDate.now().isAfter(doc.getNext().getWeek())) {
                var foundOpt = documentService.findCurrentDocument(series);
                if (foundOpt.isPresent()) {
                    currentDocumentMap.put(series.getId(), foundOpt.get().getId());
                }
                return foundOpt;
            }

            return Optional.of(doc);
        }
    }

    @GetMapping(value="/")
    public String openCurrentDocument(@RequestParam(name = "token", required = false) String token) {

        AccessObject access = accessService.getAccess(token, "view");

        Optional<DocumentModel> opt = getCurrentDocument(access.getSeries());
        
        DocumentModel doc = null;
        if (opt.isPresent()) {
            doc = opt.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return createView(doc, access, ViewMode.Edit);
    }

    @GetMapping(value="/{docId}")
    public String openDocument(@PathVariable("docId") UUID docId, @RequestParam(name = "token", required = false) String token, HttpServletResponse response) {

        AccessObject access = accessService.getAccess(token, "view");

        var opt = documentRepository.findById(docId);
        
        if (opt.isPresent()) {
            return createView(opt.get(), access, ViewMode.Edit);
        } else {
            if (token != null) {
                response.setHeader("Location", "/?token=" + token);
            } else {
                response.setHeader("Location", "/");
            }
            response.setStatus(307);
            return "";
        }
    }

    public String createView(DocumentModel doc, AccessObject access, ViewMode viewMode) {

        Set<String> permissions = access.getPermissions();

        boolean isAdmin = false;
        if (permissions.contains("admin")) {
            isAdmin = true;
        }

        boolean isEdit = false;
        if (permissions.contains("edit")) {
            isEdit = true;
        }


        String content = "";
        boolean containsChanges = false;
        if (isAdmin || (isEdit && doc.isEditable())) {
            Map<String, FieldValueModel> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().filter((v) -> v.getValue() != null).collect(Collectors.toMap((fv) -> fv.getField().getName(), (fv) -> fv, (val1, val2) -> {return val2;}));
        
            var pair = PageBuilder.buildDocumentForEdit(doc, valueMap, permissions, access.getToken().getValue());
            content = pair.getFirst();
            containsChanges = pair.getSecond();
        } else {
            if (doc.isPublic() || isEdit) {

                if (doc.getGeneratedContent() == null) {
                    content = documentService.generateDocumentContent(doc);
                } else {
                    content =  doc.getGeneratedContent();
                }
                
            } else {
                content = "Podgląd niedostępny";
            }

        }


        return pageBuilder.buildPage(isAdmin, isEdit, doc, access.getToken().getValue(), containsChanges, content);
    }






    @GetMapping(value="test") 
    public void test() {
        System.out.println("Here");
        throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED);
    }


    private DocumentDTO docModelToDto(DocumentModel model) {
        DocumentDTO dto = new DocumentDTO();
        dto.setTemplateId(model.getTemplate().getId());
        dto.setWeek(model.getWeek());
        dto.setId(model.getId());

        return dto;
    }

    private DocumentModel docDtoToModel(DocumentDTO dto) {
        DocumentModel model = new DocumentModel();
        model.setWeek(dto.getWeek());
        model.setTemplate(templateRepository.getReferenceById(dto.getTemplateId()));

        return model;
    }
}
