package pl.mo.planz.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.planz.DocumentGenerator;
import pl.mo.planz.dto.AdminDTO;
import pl.mo.planz.dto.DocumentDTO;
import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.ValueListRepository;
import pl.mo.planz.services.AccessService;
import pl.mo.planz.services.DocumentService;
import pl.mo.planz.services.IdentityService;
import pl.mo.planz.view.PageBuilder;

@RestController
public class AdminController {
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
    PageBuilder pageBuilder;
    
    @Autowired
    IdentityService identityService;

    @Autowired
    SeriesRepository seriesRepository;

    @Autowired
    DocumentService documentService;

    @Autowired
    AccessService accessService;

    @GetMapping(value="/admin/{seriesId}")
    public AdminDTO createDocumentForSeries(@RequestParam(name = "token", required = false) String token, @PathVariable("seriesId") UUID seriesId) {
        accessService.adminOrThrow(token);
        
        Optional<SeriesModel> seriesOpt =  seriesRepository.findById(seriesId);

        if (!seriesOpt.isPresent()) throw new ResponseStatusException(HttpStatusCode.valueOf(404), "No series");


        AdminDTO dto = new AdminDTO();
        dto.setSeriesName(seriesOpt.get().getName());
        dto.setAccessList(new ArrayList<>());

        return dto;

    }



    /**
     * Odświeżanie wszystkiego
     * @param token
     */
    @PostMapping(value="update")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @ResponseStatus(code = HttpStatus.OK)
    public void update(@RequestParam("token") String token) {
        accessService.adminOrThrow(token);

        DocumentGenerator.removeOldDocuments(documentRepository, fieldValueRepository, historyRepository);
        DocumentGenerator.generateDocuments(documentRepository, templateRepository, seriesRepository);

    }

    /**
     * Odświeżanie dokumentu
     * @param token
     */
    @PostMapping(value="update/{docId}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @ResponseStatus(code = HttpStatus.OK)
    public void updateDocumentContent(@PathVariable("docId") UUID docId, @RequestParam(name = "token", required = false) String token) {
        accessService.adminOrThrow(token);

        var docOpt = documentRepository.findById(docId);

        if (docOpt.isPresent()) {
            docOpt.get().setGeneratedContent(documentService.generateDocumentContent(docOpt.get()));
            documentRepository.save(docOpt.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }

}
