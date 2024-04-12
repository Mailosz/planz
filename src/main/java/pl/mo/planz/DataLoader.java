package pl.mo.planz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import pl.mo.planz.controllers.Controller;
import pl.mo.planz.controllers.TemplateController;
import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.IdentityType;
import pl.mo.planz.model.PermissionModel;
import pl.mo.planz.model.ProfileModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.TokenModel;
import pl.mo.planz.model.ValueListItemModel;
import pl.mo.planz.model.ValueListModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.ProfileRepository;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.TokenRepository;
import pl.mo.planz.repositories.ValueListRepository;
import pl.mo.planz.services.AccessService;
import pl.mo.planz.services.DocumentService;
import pl.mo.planz.services.IdentityService;

@RequiredArgsConstructor
@Component
public class DataLoader {

    private final TemplateRepository templateRepository;
    private final DocumentRepository docRepository;
    private final TokenRepository tokenRepository;
    private final IdentityRepository identityRepository;
    private final PermissionRepository permissionRepository;
    private final ValueListRepository listRepository;
    private final Controller controller;
    private final TemplateController templateController;
    private final SeriesRepository seriesRepository;
    private final DocumentService documentService;
    private final ProfileRepository profileRepository;
    private final AccessService accessService;
    private final IdentityService identityService;



    @PostConstruct
    public void postConstruct() {
        LoadData();
    }


    private void LoadData() {

        if (templateRepository.count() > 0 || docRepository.count() > 0) {
            System.out.println("Niepusta baza, skipped");
            return;
        }

        
        PermissionModel viewPermission = createOrGetPermission("view");
        PermissionModel editPermission = createOrGetPermission("edit");
        PermissionModel adminPermission = createOrGetPermission("admin");
        
        loadList(listRepository, "lists/bracia.txt", "bracia", UUID.fromString("c70f096b-2639-40a1-8607-a2d5db8797d3"));
        loadList(listRepository, "lists/wszyscy.txt", "wszyscy", UUID.fromString("dce7a62d-3d5f-4c38-879a-c8abae8371a5"));
        loadList(listRepository, "lists/grupy.txt", "grupy", UUID.fromString("f662628b-b759-48b5-a898-637cfa273062"));

        TemplateModel mainTemplate = loadTemplate(templateRepository, "templates/template.html", "Domyślny", UUID.fromString("5a69f438-f159-4299-8d6e-16792f45bf9e"));
        TemplateModel tylkoNiedziela = loadTemplate(templateRepository, "templates/tylko-niedziela.html", "Tylko niedziela", UUID.fromString("86809da1-a02d-4050-90c4-90a849f4fbe3"));
        TemplateModel tylkoWTygodniu = loadTemplate(templateRepository, "templates/tylko-w-tygodniu.html", "Tylko w tygodniu", UUID.fromString("fafb6f5d-8a2d-41d2-835f-bf6a4cf7efe1"));

        SeriesModel defaultSeries = new SeriesModel();
        defaultSeries.setName("Default series");
        defaultSeries.setGenerationInterval(Period.ofWeeks(1));
        defaultSeries.setDefaultTemplate(mainTemplate);
        seriesRepository.save(defaultSeries);

        SeriesModel secondSeries = new SeriesModel();
        secondSeries.setName("Second series");
        secondSeries.setGenerationInterval(Period.ofWeeks(2));
        secondSeries.setDefaultTemplate(tylkoNiedziela);
        seriesRepository.save(secondSeries);

        
        IdentityModel publicIdentity = createIdentity("public", IdentityType.TOKEN);
        IdentityModel editorIdentity = createIdentity("editor", IdentityType.TOKEN);
        IdentityModel adminIdentity = createIdentity("admin", IdentityType.TOKEN);

        TokenModel showToken = getOrCreateToken("show", publicIdentity, defaultSeries);
        TokenModel testToken = getOrCreateToken("test", editorIdentity, defaultSeries);
        TokenModel adminToken = getOrCreateToken("adm", adminIdentity, defaultSeries);

        TokenModel show2Token = getOrCreateToken("public", publicIdentity, secondSeries);
        TokenModel test2Token = getOrCreateToken("edit", editorIdentity, secondSeries);
        TokenModel admin2Token = getOrCreateToken("admin", adminIdentity, secondSeries);

        ProfileModel publicProfile = createProfile("Public profile", viewPermission);
        ProfileModel editorProfile = createProfile("Editor profile", viewPermission, editPermission);
        ProfileModel adminProfile = createProfile("Admin profile", viewPermission, editPermission, adminPermission);

        identityService.assignProfile(adminProfile, adminIdentity, defaultSeries);
        identityService.assignProfile(editorProfile, editorIdentity, defaultSeries);
        identityService.assignProfile(publicProfile, publicIdentity, defaultSeries);
        identityService.assignProfile(adminProfile, adminIdentity, secondSeries);
        identityService.assignProfile(editorProfile, editorIdentity, secondSeries);
        identityService.assignProfile(publicProfile, publicIdentity, secondSeries);



        documentService.createForSeries(defaultSeries);
        documentService.createForSeries(secondSeries);



    }

    private PermissionModel createOrGetPermission(String name) {
        Optional<PermissionModel> edtOpt =  permissionRepository.findByName(name);
        if (edtOpt.isPresent()) {
            return edtOpt.get();
        } else {
            PermissionModel permissionModel = new PermissionModel();
            permissionModel.setName(name);
            
            permissionRepository.save(permissionModel);

            return permissionModel;
        }
    }

    private IdentityModel createIdentity(String name, IdentityType type) {
        IdentityModel identityModel = new IdentityModel();
        identityModel.setName(name);
        identityModel.setType(type);
        
        identityRepository.save(identityModel);

        return identityModel;
    }

    private ProfileModel createProfile(String name, PermissionModel... permissions) {
        ProfileModel profileModel = new ProfileModel();
        profileModel.setName(name);
        profileModel.setPermissions(new HashSet<>(Arrays.asList(permissions)));
        
        profileRepository.save(profileModel);

        return profileModel;
    }


    private TokenModel getOrCreateToken(String token, IdentityModel identity, SeriesModel series) {
        Optional<TokenModel> tokenOpt =  tokenRepository.findByValue(token);
        if (tokenOpt.isPresent()) {
            return tokenOpt.get();
        } else {
            TokenModel tokenModel = new TokenModel();
            tokenModel.setValue(token);
            tokenModel.setIdentity(identity);
            tokenModel.setSeries(series);
            
            tokenRepository.save(tokenModel);
    
            return tokenModel;
        }
    }


    private TemplateModel loadTemplate(TemplateRepository templateRepo, String resName, String name, UUID uuid) {

        InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream(resName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(templateStream, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(100000);
        try {
            while (true) {
                
                    int c = reader.read();
                    if (c < 0) {
                        break;
                    }
                    sb.append((char)c);
            } 
            String templateContent = sb.toString();

            TemplateModel templateModel = new TemplateModel();
            //templateModel.setId(uuid);
            templateModel.setName(name);
            templateController.parseTemplateAndSave(templateContent, templateModel);

            return templateModel;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TemplateParsingException e) {
            System.out.println("-------------");
            System.out.println();
            System.out.println("Parse error: " + e.getDesc());
            System.out.println();
            System.out.println("-------------");
        }

        return null;
    }

    private void loadList(ValueListRepository listRepo, String resName, String name, UUID uuid) {

        // if (listRepository.findById(uuid).isPresent()) {
        //     System.out.println("List " + name + " already exists, skipped");
        //     return;
        // }

        try {
            InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream(resName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(templateStream, StandardCharsets.UTF_8));

            ValueListModel list = new ValueListModel();
            //list.setId(uuid);
            list.setName(name);
            list.setValues(new ArrayList<ValueListItemModel>());
            String line;
            
                line = reader.readLine();

            while (line!=null){
                list.addValue(line);
                line= reader.readLine();
            }
            listRepo.save(list);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
