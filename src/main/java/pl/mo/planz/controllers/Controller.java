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
import pl.mo.planz.model.DatalistModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.DatalistRepository;
import pl.mo.planz.services.AccessObject;
import pl.mo.planz.services.AccessService;
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
public class Controller {

    public static UUID currentDocumentId;
    
    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    FieldValueRepository fieldValueRepository;

    @Autowired
    PermissionRepository profileRepository;

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
    SeriesRepository seriesRepository;    

    @Autowired
    PageBuilder pageBuilder;

    @Autowired
    AccessService accessService;


    @GetMapping("favicon.ico")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void getFavicon() {
        //TODO: favicon please
    }


    private static Optional<DocumentModel> getCurrentDocument(DocumentRepository docRepo) {
        if (currentDocumentId != null) {

        } else {
            currentDocumentId = findCurrentDocument(docRepo);
        }

        return docRepo.findById(currentDocumentId);
    }

    public static UUID findCurrentDocument(DocumentRepository docRepo) {
        List<DocumentModel> docs = docRepo.findAll();
        
        var now = LocalDate.now();

        DocumentModel current = null;
        long diff = 0;
        for (var doc : docs) {
            if (now.isEqual(doc.getDate()) || (now.isAfter(doc.getDate()) && now.isBefore(doc.getDate().plusDays(7)))) {
                //perfect match
                current = doc;
                break;
            } else if (current == null) {
                current = doc;
                diff = Math.abs(ChronoUnit.DAYS.between(now, doc.getDate()));
            } else {
                long d = Math.abs(ChronoUnit.DAYS.between(now, doc.getDate()));
                if (d < diff) {
                    current = doc;
                    diff = d;
                }
            }
        }

        return current.getId();
    }


    public String createView(DocumentModel doc, AccessObject access, ViewMode viewMode) {

        Set<String> profiles = access.getPermissions();

        boolean isAdmin = false;
        if (profiles.contains("admin")) {
            isAdmin = true;
        }

        boolean isEdit = false;
        if (profiles.contains("edit")) {
            isEdit = true;
        }

        if (isAdmin) {

        } else if (isEdit) {

        } else {
            if (!profiles.contains("view")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }



        String content = "";
        boolean containsChanges = false;
        if (isAdmin || (isEdit && doc.isEditable())) {
            Map<String, FieldValueModel> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().filter((v) -> v.getValue() != null).collect(Collectors.toMap((fv) -> fv.getField().getName(), (fv) -> fv, (val1, val2) -> {return val2;}));
        
            var pair = PageBuilder.buildDocumentForEdit(doc, valueMap, profiles, access.getToken().getValue());
            content = pair.getFirst();
            containsChanges = pair.getSecond();
        } else {
            if (doc.isPublic() || isEdit) {

                if (doc.getGeneratedContent() == null) {
                    content = generateDocumentContent(doc);
                } else {
                    content =  doc.getGeneratedContent();
                }
                
            } else {
                content = "Podgląd niedostępny";
            }

        }


        return pageBuilder.buildPage(isAdmin, isEdit, doc, access.getToken().getValue(), containsChanges, content);
    }

    /**
     * Generates document's content
     * @param doc
     * @return Document's generated content
     */
    public String generateDocumentContent(DocumentModel doc) {

        Map<String, FieldValueModel> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().filter((v) -> v.getValue() != null).collect(Collectors.toMap((fv) -> fv.getField().getName(), (fv) -> fv, (val1, val2) -> {return val2;}));
        String content = PageBuilder.buildTemplateForView(doc, valueMap);

        doc.setGeneratedContent(content);
        doc.setGeneratedTime(Instant.now());
        documentRepository.save(doc);

        return content;

    }


}
