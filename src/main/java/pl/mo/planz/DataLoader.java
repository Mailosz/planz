package pl.mo.planz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.ProfileModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.TokenModel;
import pl.mo.planz.model.ValueListItemModel;
import pl.mo.planz.model.ValueListModel;


public class DataLoader {

    private TemplateRepository templateRepository;
    private DocumentRepository docRepository;
    private TokenRepository tokenRepository;
    private IdentityRepository identityRepository;
    private ProfileRepository profileRepository;
    private ValueListRepository listRepository;
    private Controller controller;

    @Autowired
    public DataLoader(TemplateRepository tr, TokenRepository tokenRepo, IdentityRepository ir, ProfileRepository pr, Controller controller, DocumentRepository docRepo, ValueListRepository listRepository) {
        this.templateRepository = tr;
        this.tokenRepository = tokenRepo;
        this.identityRepository = ir;
        this.profileRepository = pr;
        this.controller = controller;
        this.docRepository = docRepo;
        this.listRepository = listRepository;
        LoadData();
    }

    private void LoadData() {

        ProfileModel edtProfile = new ProfileModel();
        edtProfile.setName("edit");
        profileRepository.save(edtProfile);

        ProfileModel admProfile = new ProfileModel();
        admProfile.setName("admin");
        profileRepository.save(admProfile);

        ProfileModel profile3 = new ProfileModel();
        profile3.setName("edit3");
        profileRepository.save(profile3);


        
        IdentityModel publicIdentity = new IdentityModel();
        identityRepository.save(publicIdentity);

        TokenModel showToken = new TokenModel();
        showToken.setValue("show");
        showToken.setIdentity(publicIdentity);
        tokenRepository.save(showToken);


        IdentityModel testIdentity = new IdentityModel();
        testIdentity.setProfiles(new HashSet<ProfileModel>());
        testIdentity.getProfiles().add(edtProfile);
        identityRepository.save(testIdentity);

        TokenModel testToken = new TokenModel();
        testToken.setValue("test");
        testToken.setIdentity(testIdentity);
        tokenRepository.save(testToken);


        IdentityModel admIdentity = new IdentityModel();
        admIdentity.setProfiles(new HashSet<ProfileModel>());
        admIdentity.getProfiles().add(admProfile);
        identityRepository.save(admIdentity);

        TokenModel admToken = new TokenModel();
        admToken.setValue("adm");
        admToken.setIdentity(admIdentity);
        tokenRepository.save(admToken);




        loadList(listRepository, "lists/bracia.txt", "bracia");
        loadList(listRepository, "lists/wszyscy.txt", "wszyscy");
        loadList(listRepository, "lists/grupy.txt", "grupy");

            


        InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream("templates/template.html");
        BufferedReader reader = new BufferedReader(new InputStreamReader(templateStream, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(10000);
        try {
            while (true) {
                
                    int c = reader.read();
                    if (c < 0) {
                        break;
                    }
                    sb.append((char)c);
            } 
            String templateContent = sb.toString();

            TemplateModel mainTemplate = new TemplateModel();
            mainTemplate.setName("Main template");
            controller.parseTemplateAndSave(templateContent, mainTemplate);

            DocumentModel document1 = new DocumentModel();
            document1.setTemplate(mainTemplate);
            document1.setWeek(LocalDate.of(2023, Month.APRIL, 3));
            docRepository.save(document1);

            DocumentModel document2 = new DocumentModel();
            document2.setTemplate(mainTemplate);
            document2.setWeek(LocalDate.of(2023, Month.APRIL, 10));
            docRepository.save(document2);

            DocumentModel document3 = new DocumentModel();
            document3.setTemplate(mainTemplate);
            document3.setWeek(LocalDate.of(2023, Month.APRIL, 17));
            docRepository.save(document3);

            DocumentModel document4 = new DocumentModel();
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
    }

    private void loadList(ValueListRepository listRepo, String resName, String name) {
        try {
            InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream(resName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(templateStream, StandardCharsets.UTF_8));

            ValueListModel list = new ValueListModel();
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
