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
import pl.mo.planz.model.ProfileModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.TokenModel;
import pl.mo.planz.model.ValueListItemModel;
import pl.mo.planz.model.ValueListModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.ProfileRepository;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.TokenRepository;
import pl.mo.planz.repositories.ValueListRepository;

@RequiredArgsConstructor
@Component
public class DataLoader {

    private final TemplateRepository templateRepository;
    private final DocumentRepository docRepository;
    private final TokenRepository tokenRepository;
    private final IdentityRepository identityRepository;
    private final ProfileRepository profileRepository;
    private final ValueListRepository listRepository;
    private final Controller controller;
    private final TemplateController templateController;
    private final SeriesRepository seriesRepository;



    @PostConstruct
    public void postConstruct() {
        LoadData();
    }


    private void LoadData() {

        if (templateRepository.count() > 0 || docRepository.count() > 0) {
            System.out.println("Niepusta baza, skipped");
            return;
        }

        
        ProfileModel edtProfile;
        ProfileModel admProfile;
        ProfileModel viewProfile;
        
        Optional<ProfileModel> edtOpt =  profileRepository.findByName("edit");
        if (edtOpt.isPresent()) {
            edtProfile = edtOpt.get();
        } else {
            edtProfile = new ProfileModel();
            //edtProfile.setId(UUID.fromString("f36ed828-f7c1-40ec-9bf5-6adf69646f3a"));
            edtProfile.setName("edit");
            profileRepository.save(edtProfile);
        }

        Optional<ProfileModel> admOpt =  profileRepository.findByName("admin");
        if (admOpt.isPresent()) {
            admProfile = admOpt.get();
        } else {
            admProfile = new ProfileModel();
            //admProfile.setId(UUID.fromString("a91aaae3-7a4d-4c7c-b658-3846268e7df9"));
            admProfile.setName("admin");
            profileRepository.save(admProfile);
        }

        Optional<ProfileModel> viewOpt =  profileRepository.findByName("view");
        if (viewOpt.isPresent()) {
            viewProfile = viewOpt.get();
        } else {
            viewProfile = new ProfileModel();
            //tstProfile.setId(UUID.fromString("90d48d10-8b00-410b-938e-25c5d3f9c6a7"));
            viewProfile.setName("view");
            profileRepository.save(viewProfile);
        }


        loadList(listRepository, "lists/bracia.txt", "bracia", UUID.fromString("c70f096b-2639-40a1-8607-a2d5db8797d3"));
        loadList(listRepository, "lists/wszyscy.txt", "wszyscy", UUID.fromString("dce7a62d-3d5f-4c38-879a-c8abae8371a5"));
        loadList(listRepository, "lists/grupy.txt", "grupy", UUID.fromString("f662628b-b759-48b5-a898-637cfa273062"));

        TemplateModel mainTemplate = loadTemplate(templateRepository, "templates/template.html", "Domyślny", UUID.fromString("5a69f438-f159-4299-8d6e-16792f45bf9e"));
        loadTemplate(templateRepository, "templates/tylko-niedziela.html", "Tylko niedziela", UUID.fromString("86809da1-a02d-4050-90c4-90a849f4fbe3"));
        loadTemplate(templateRepository, "templates/tylko-w-tygodniu.html", "Tylko w tygodniu", UUID.fromString("fafb6f5d-8a2d-41d2-835f-bf6a4cf7efe1"));

        SeriesModel defaultSeries = new SeriesModel();
        // defaultSeries.setId(UUID.fromString("fa451ce2-5528-4b15-b93f-2d6f932bf3c4"));
        defaultSeries.setName("Default series");
        defaultSeries.setGenerationInterval(Period.ofWeeks(1));
        defaultSeries.setDefaultTemplate(mainTemplate);
        seriesRepository.save(defaultSeries);

        
        IdentityModel publicIdentity = new IdentityModel();
        //publicIdentity.setId(UUID.fromString("bc463ce2-5528-4b15-b93f-2d6f932bf3c4"));
        publicIdentity.setProfiles(new HashSet<ProfileModel>());
        publicIdentity.getProfiles().add(viewProfile);
        identityRepository.save(publicIdentity);

        TokenModel showToken = new TokenModel();
        showToken.setValue("show");
        showToken.setIdentity(publicIdentity);
        showToken.setSeries(defaultSeries);
        tokenRepository.save(showToken);


        IdentityModel testIdentity = new IdentityModel();
        //testIdentity.setId(UUID.fromString("f310f0d9-68c8-45eb-a9be-bd0cc20665f6"));
        testIdentity.setProfiles(new HashSet<ProfileModel>());
        testIdentity.setProfiles(new HashSet<ProfileModel>());
        testIdentity.getProfiles().add(edtProfile);
        identityRepository.save(testIdentity);

        TokenModel testToken = new TokenModel();
        testToken.setValue("test");
        testToken.setIdentity(testIdentity);
        testToken.setSeries(defaultSeries);
        tokenRepository.save(testToken);


        IdentityModel admIdentity = new IdentityModel();
        //admIdentity.setId(UUID.fromString("41a50ee8-4e49-4569-ab62-de4a7f2e3346"));
        admIdentity.setProfiles(new HashSet<ProfileModel>());
        admIdentity.setProfiles(new HashSet<ProfileModel>());
        admIdentity.getProfiles().add(admProfile);
        identityRepository.save(admIdentity);

        TokenModel admToken = new TokenModel();
        admToken.setValue("adm");
        admToken.setIdentity(admIdentity);
        admToken.setSeries(defaultSeries);
        tokenRepository.save(admToken);


        DocumentModel document1 = new DocumentModel();
        //document1.setId(UUID.fromString("7e3ef958-0db6-4b5b-bc4e-fd4768a1ade2"));
        document1.setTemplate(mainTemplate);
        document1.setWeek(LocalDate.of(2023, Month.APRIL, 3));
        docRepository.save(document1);

        DocumentModel document2 = new DocumentModel();
        //document2.setId(UUID.fromString("b956736d-0fd0-46b9-ae07-7561ddaa714b"));
        document2.setTemplate(mainTemplate);
        document2.setWeek(LocalDate.of(2023, Month.APRIL, 10));
        docRepository.save(document2);

        DocumentModel document3 = new DocumentModel();
        //document3.setId(UUID.fromString("b7350128-84cb-43e4-bd4f-8d78abb95cde"));
        document3.setTemplate(mainTemplate);
        document3.setWeek(LocalDate.of(2023, Month.APRIL, 17));
        docRepository.save(document3);

        DocumentModel document4 = new DocumentModel();
        //document4.setId(UUID.fromString("49115c0b-ebeb-40c9-a232-0443886ea5d0"));
        document4.setTemplate(mainTemplate);
        document4.setWeek(LocalDate.of(2023, Month.APRIL, 24));
        docRepository.save(document4);

        document1.setNext(document2);
        document2.setPrev(document1);
        document2.setNext(document3);
        document3.setPrev(document2);
        document3.setNext(document4);
        document4.setPrev(document3);

        docRepository.save(document1);
        docRepository.save(document2);
        docRepository.save(document3);
        docRepository.save(document4);

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
