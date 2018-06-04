package com.cs.registre;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.axemble.vdoc.sdk.agent.base.BaseAgent;
import com.axemble.vdoc.sdk.exceptions.PortalModuleException;
import com.axemble.vdoc.sdk.exceptions.SDKException;
import com.axemble.vdoc.sdk.interfaces.IConnectionDefinition;
import com.axemble.vdoc.sdk.interfaces.IContext;
import com.axemble.vdoc.sdk.utils.Logger;
import com.axemble.vdp.ui.framework.Constants;
import com.axemble.vdp.ui.framework.foundation.Navigator;
import com.vdoc.sdk.dm.beans.definitions.Context;
import com.vdoc.sdk.dm.beans.definitions.Folder;
import com.vdoc.sdk.dm.beans.definitions.Version;
import com.vdoc.sdk.dm.beans.definitions.VersionDefinition;
import com.vdoc.sdk.dm.beans.model.IWrap;
import com.vdoc.sdk.dm.services.factory.ServiceFactory;
import com.vdoc.sdk.dm.services.model.IFolderService;
import com.vdoc.sdk.dm.services.model.IVersionAttachmentService;
import com.vdoc.sdk.dm.services.model.IVersionDefinitionService;
import com.vdoc.sdk.dm.services.model.IVersionService;
/**
 * ===================================
 * @author r.sabri
 * @date_création 10.05.2018
 * @version 1.0.0 
 * @plateforme vdoc 14.2
 * ===================================
 */
public class AgentCreationDocToGed extends BaseAgent{

    protected static final Logger log = Logger.getLogger(AgentCreationDocToGed.class);
    protected IWrap<Context> contextGED = null;
    protected ServiceFactory serviceFactory = null;
    protected IVersionService versionService = null;
    protected IVersionAttachmentService versionAttachmentService = null;
    protected IFolderService folderService = null;
    protected IVersionDefinitionService versionDefinitionService = null;
    public IContext context;
    Connection connection = null;
    PreparedStatement st = null;
    ResultSet result = null;
    String query = null;
    String NatReg = null;
    // Dépôt des fichiers
    public String path="C:\\registre";
    // ------------------------------------------------------------------
    // Execution de l'agent 
    // ------------------------------------------------------------------
    @Override
    protected void execute() {

        if(!IsEmptyDirctory())
        {       
            Save();
        }else
        {
            printInfo("Le repertoire est vider");
            this.printLine();
        }
    }
    // ------------------------------------------------------------------
    // Vérification de dépôt des fichiers scannés
    // ------------------------------------------------------------------   
    public boolean IsEmptyDirctory()
    {
        try{
            File file = new File(path);
            File[] files = file.listFiles();
            if (files.length==0) {
                return true;
            }else
            {
                return false;
            }
        }catch(Exception e) {
            log.error("CS:Error in IsEmptyDirctory  method : " + e.getClass() + " - " + e.getMessage());
            Navigator.getNavigator().processErrors(e);
            return true;
        }
    }
    // ------------------------------------------------------------------
    // Méthode d'enregistrement des document au niveau GED
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void Save()
    {
        context = getWorkflowModule().getContextByLogin("sysadmin");
        String folderName = "";
        @SuppressWarnings("unused")
        String tempFolder = "";
        String sys_Reference = null;
        Map<String, Map<String, Object>> mappingMap = new HashMap<String, Map<String, Object>>();
        // Message de démarrage
        this.printSeparator();
        printInfo("Agent de démarrage!");
        this.printSeparator();
        this.printLine();
        serviceFactory = null;
        try
        {
            this.printInfo( "Traitement démarrer..." );
            this.printSeparator();
            this.printLine();
            //appelle la methode de connection
            IConnectionDefinition<java.sql.Connection> connectionDefinition =(IConnectionDefinition<Connection>) getPortalModule().getConnectionDefinition(context,"Ref_VDocDm");
            connection=connectionDefinition.getConnection();
            //Server 192.168.1.33 param
            query = "SELECT PROTOCLE,PORT, HOST, NOMDELABASEDEDONNEESGED, DOSSIERRACINEGED, DOSSIERTEMPORAIREDELAGED, NOMSYSTEMEPJ, NOMSYSTEMETYPEDOC FROM r_stoparametres where id=1";
            // Server 192.168.1.123 param
            //query = "SELECT PROTOCLE,PORT, HOST, NOMDELABASEDEDONNEESGED, DOSSIERRACINEGED, DOSSIERTEMPORAIREDELAGED, NOMSYSTEMEPJ, NOMSYSTEMETYPEDOC FROM r_stoparametres where id=1";
            st = connection.prepareStatement(query);
            result = st.executeQuery();
            if (result.next())
            {
                serviceFactory = ServiceFactory.getServiceFactory(result.getString("HOST"), result.getString("NOMDELABASEDEDONNEESGED"), new File(Constants.DEFAULT_STORAGE
                        + "../../../resources/groovy/documentmanagement"), null);

                // other parameter
                folderName = result.getString("DOSSIERRACINEGED");
                tempFolder = result.getString("DOSSIERTEMPORAIREDELAGED");
            }
            this.printInfo( "IWrap <Folder>..." );
            this.printSeparator();
            this.printLine();
            IWrap<Folder> folder = initializeServiceEnvironment(folderName,serviceFactory);

            int appartmentSize = 100;
            int appartmentSizeCounter = 0;


            if (appartmentSizeCounter > appartmentSize)
            {
                ServiceFactory.release(serviceFactory);

                folder = initializeServiceEnvironment(folderName,serviceFactory);
                appartmentSizeCounter = 0;
            }
            
            listDirectory(path,contextGED, folder, mappingMap);
            this.printInfo("OK");
            this.printSeparator();
            this.printInfo( "Opération terminée avec succès" );
            this.printSeparator();
        }
        catch (Exception e)
        {
            LOGGER.error(sys_Reference, e);
            throw new SDKException("", e);
        }
        finally
        {
            try
            {
                st.close();
                result.close();
                connection.close();
                // Release factory and memory
                ServiceFactory.release(serviceFactory);
            }
            catch (Exception e)
            {
            }
            this.printSeparator();
            this.printInfo( "Terminer " );
        }
    }
    // ------------------------------------------------------------------
    // Paramettre de connexion au serveur GED
    // ------------------------------------------------------------------
    protected IWrap<Folder> initializeServiceEnvironment(String folderName, ServiceFactory serviceFactory)
    {
        versionService = serviceFactory.getVersionService();
        versionDefinitionService = serviceFactory.getVersionDefinitionService();
        versionAttachmentService = serviceFactory.getVersionAttachmentService();
        folderService = serviceFactory.getFolderService();
        this.printInfo( "Connexion GED..." );
        this.printSeparator();
        contextGED = versionService.getContextByAuthentication("adm","manager"); // Server 192.168.1.33
        this.printInfo( "Connexion OK..." );
        this.printSeparator();
        //contextGED = versionService.getContextByAuthentication("adm","manager");
        IWrap<Folder> folder = folderService.getFolder(contextGED, folderService.getRootFolder(contextGED), folderName);
        return folder;
    }
    // ------------------------------------------------------------------
    // Traitement des fichiers stockés au dépots
    // ------------------------------------------------------------------
    public void listDirectory(String dir,IWrap<Context> context,IWrap<Folder> folder,
            Map<String, Map<String, Object>> mappingMap) throws Exception {

        try{
            File file = new File(dir);
            //lister la repertoire
            File[] files = file.listFiles();
            if (files.length != 0) {

                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory() == false) {
                        createFile(context,folder,mappingMap,files[i].getAbsolutePath(),files[i].getName());
                    } else {
                        //
                    }
                }
            }
        }catch(Exception e)
        {
            log.error("CS :Erreur dans la  methode listDirectory :" + e.getClass() + " - " + e.getMessage());
        }
    }
    // ------------------------------------------------------------------
    // Methode d'archivage au serveur GED
    // ------------------------------------------------------------------
    public void createFile(IWrap<Context> context,IWrap<Folder> folder,
            Map<String, Map<String, Object>> mappingMap,String Path_X,String NomDocument) throws Exception
    {
        ArrayList<String> list =new ArrayList<String>();
        String Nat1 = null;
        String Nat2 = null;
        String documentTypeName=null;
        try
        {
            list=DiviserNomDocument(NomDocument);
            //verifier si la liste est different à null
            if(list!=null && !list.contains(null))
            {
                Nat1=NatureRegistre(list.get(1));
                Nat2=NatureRegistre(list.get(2));
                documentTypeName="Avocat_TD";
                // On récupère le dossier racine de la GED et le type de document ( on a un seul type de document)
                IWrap<VersionDefinition> versionDefinition = versionDefinitionService.getVersionDefinition(contextGED, documentTypeName);
                // We create a new version with iDocumentManagementModule
                IWrap<Version> newVersion = serviceFactory.getBeanFactory().getEmptyBean(Version.class);
                // Version N°
                newVersion.setValue(Version.VersionNumber, "1.0");
                // Version Title
                // newVersion.setValue(Version.Title, cAttachment.getName());
                newVersion.setValue(Version.Title, "DOC");
                for (Entry<String, Map<String, Object>> entry : mappingMap.entrySet())
                {
                    Map<String, Object> dataMap = entry.getValue();
                    for (Entry<String, Object> dataEntry : dataMap.entrySet())
                    {
                        if (dataEntry.getValue() != null)
                        {
                            newVersion.setValue(dataEntry.getKey(), dataEntry.getValue());
                        }
                    }
                }
                newVersion.setValue("VDOC_FREE_NUMDOSSIER",list.get(0));  
                newVersion.setValue("VDOC_FREE_TYPEREG",Nat1);
                newVersion.setValue("VDOC_FREE_REG",Nat2);
                // Save version
                newVersion = versionService.createVersion(contextGED, versionDefinition, folder, newVersion);
                newVersion.save(contextGED);
                versionAttachmentService.setAttachment(contextGED, newVersion, Path_X);
                // LOG new Version ID
                LOGGER.info(Long.toString(newVersion.getId()));
                //supprimer le fichier creer dans la Ged
                File test=new File(Path_X);
                test.delete();
            }
        }
        catch (Exception e)
        {
            String message = e.getMessage();
            if (message == null)
            {
                message = "";
            }
            log.error("CS : Erreur dans la methode createFile  : " + e.getClass() + " - " + e.getMessage()+" _ "+e.getStackTrace()[0].getLineNumber());
            Navigator.getNavigator().processErrors(e);
        }
    }
    // ------------------------------------------------------------------
    // Traitement des documents pdf
    // ------------------------------------------------------------------
    public static ArrayList<String> DiviserNomDocument(String NomDocument)
    {
        ArrayList<String> list =new ArrayList<String>();

        try{
            String Docname=null;
            String NumDossier = null;
            String TypeRegistre = null;
            String Registre = null;
            String[] List=null;

            //change le point par le tire  EX  4500_20_21.pdf ===> 4500_20_21_pdf 
            if(NomDocument.contains("."))
            {
                Docname=NomDocument.replace(".", "_");
            }

            // Le principe de split est de diviser un string en un tableau se string selaon un caractere ou un string ici le caractere est 
            if (Docname.contains("_"))
            {
                List=Docname.split("_");
                if(List.length==4)
                {
                    NumDossier=List[0];                                         
                    TypeRegistre=List[1];
                    Registre=List[2];
                }
                else
                {
                    System.out.println("Avertissement 9001. Ce Document : '"+NomDocument+"' ne respecte pas la norme d'arborescence.");
                    return null;
                }

            }else
            {
                System.out.println("Avertissement 9002. Ce Document '"+NomDocument+"' ne respecte pas la norme d'arborescence.");
                return null;
            }
            // Remplacer le char '-' par '/'
            if (NumDossier.contains("-"))
            {
                NumDossier = NumDossier.replace('-', '/');
            }

            // Numéro de Dossier
            list.add(NumDossier);

            // Type de registre
            list.add(TypeRegistre);

            // Registre
            list.add(Registre);

            return list;

        }catch(Exception e)
        {
            System.out.println("CS : Erreur dans la DiviserNomDocument  :" + e.getClass() + " - " + e.getMessage()+" _ "+e.getStackTrace()[0].getLineNumber());
            Navigator.getNavigator().processErrors(e);
            return null;
        }
    }
    // ------------------------------------------------------------------
    // Affectation des natures de registre
    // ------------------------------------------------------------------
    public String NatureRegistre(String indice)
    {
        try{
            connection=ConnectionDefinition("Ref_Avocat").getConnection();
            query="SELECT REGISTRE_TRADUCTION FROM REGISTRE_NATURE WHERE INDICE='"+indice+"'";
            st = connection.prepareStatement(query);
            result=st.executeQuery();
            while(result.next())
            {
                NatReg=result.getString(1);
            }
        }catch(Exception e)
        {
            log.error("CS: Erreur dans la methode NatureRegistre : " + e.getClass() + " - " + e.getMessage());
            Navigator.getNavigator().processErrors(e);
        }       finally
        {
            try
            {
                st.close();
                result.close();
                connection.close();
            }
            catch (Exception e)
            {
            }
        }
        return NatReg;
    }
    // ------------------------------------------------------------------
    // Connexion au base de données Gavocat
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public IConnectionDefinition<java.sql.Connection> ConnectionDefinition (String Ref_externe) throws PortalModuleException
    {
        context = getWorkflowModule().getContextByLogin("sysadmin");
        return (IConnectionDefinition<Connection>) getPortalModule().getConnectionDefinition(context, Ref_externe);
    }
}
