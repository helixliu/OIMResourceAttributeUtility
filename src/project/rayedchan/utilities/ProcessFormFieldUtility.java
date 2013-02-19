package project.rayedchan.utilities;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcAddFieldFailedException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcDeleteNotAllowedException;
import Thor.API.Exceptions.tcFormFieldNotFoundException;
import Thor.API.Exceptions.tcFormNotFoundException;
import Thor.API.Exceptions.tcInvalidAttributeException;
import Thor.API.Operations.tcFormDefinitionOperationsIntf;
import Thor.API.tcResultSet;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import project.rayedchan.custom.objects.ProcessFormField;

/**
 * @author rayedchan
 * A utility to add and delete a form field. Changes are made to the current 
 * process form version which has never been active. A flat file can be used 
 * as a data source. 
 * 
 * Note: Once a process form version has been made active, no changes can ever
 * be made to that form version.
 * 
 * TODO: Auto-increment order field 
 */
public class ProcessFormFieldUtility
{
    //List of possible field attribute names to be specified in file for adding fields
    public static String FIELDLABEL = "field_label";
    public static String VARIANTTYPE = "variant_type";
    public static String FIELDTYPE = "field_type";
    public static String LENGTH = "length";
    public static String ORDER = "order";
    
    //Possible Variant Type of a field
    public static String VT_BYTE = "Byte";
    public static String VT_DOUBLE = "Double";
    public static String VT_INTEGER = "Integer";
    public static String VT_STRING = "String";
    public static String VT_SHORT = "Short";
    public static String VT_LONG = "Long";
    public static String VT_DATE = "Date";
    public static String VT_BOOLEAN = "Boolean";
    public static String VT_BYTEARRAY = "Byte Array";
    
    //Possible Field Type of a Field
    public static String FT_TEXTFIELD = "TextField";
    public static String FT_RADIOBUTTON = "RadioButton";
    public static String FT_DATEFIELDWITHDIALOG = "DateFieldDlg";
    public static String FT_DISPLAYONLYFIELD = "DOField";
    public static String FT_PASSWORDFIELD = "PasswordField";
    public static String FT_CHECKBOX = "CheckBox";
    public static String FT_DATACOMBOBOX = "ComboBox";
    public static String FT_TEXTAREA = "TextArea";
    public static String FT_ITRESOURCELOOKUPFIELD = "ITResourceLookupField";
    public static String FT_LOOKUPFIELD = "LookupField";

    
    /*
     *Add fields to the lastest process form version. Data source is a flat file. File must follow 
     *a specific format. The column name of the field is determined by the name of the field label.
     *If the column name of the field exist, then the column name will be appended a number starting at 1.
     *Only form versions that has never been made active can have fields added, deleted, or updated.
     * 
     * Sanity Check: Check file format before adding fields to process form.
     * Do not add existing field label names. [My Check]
     * Make sure field attributes are the right type.
     * Check if the latest form version is locked.
     * 
     * File Format
     * <Process Form Table Name>
     * <Field attributes tab delimited [Field_Label	Variant_Type	Field_Type	Length	Order]>
     * <field record 1>
     * <field record 2>
     * 
     * For the line that contains the field attribute names, "Field Label" is required.
     * The other field attribute will have these default values if not specified.
     * VARIANTTYPE = "String";
     * FIELDTYPE = "TextField"
     * LENGTH = "100"
     * ORDER = "50"
     * 
     * 
     * @params - 
     *          formDefOps - tcFormDefinitionOperationsIntf service object
     *          fileName - name of the file 
     * 
     * @return - boolean value to indicate success or failure
     * 
     */
    public static boolean addFieldsToProcessFormDSFF(tcFormDefinitionOperationsIntf formDefOps, String fileName) throws tcAPIException, tcColumnNotFoundException, tcFormNotFoundException
    {    
        FileInputStream fstream = null;
        DataInputStream in = null;
        BufferedReader br = null;
            
        try 
        {    
            fstream = new FileInputStream(fileName); //Open File
            in = new DataInputStream(fstream); //Get the object of DataInputStream
            br = new BufferedReader(new InputStreamReader(in));
            
            String strLine; //var to store a line of a file
            ArrayList<String> pf_fieldAttributeNameArray = new ArrayList<String>(); //store the name of the field attributes 
            ArrayList<ProcessFormField> pf_fieldRecordArray = new ArrayList<ProcessFormField>(); //store all process form fields to be added
            
            //First line of the file should be the name of the process form
            String processFormName = br.readLine();
            if(doesProcessFormExist(formDefOps, processFormName) == false)
            {
                System.out.println("[Error]: Process form name "+ processFormName + " does not exist.");
                return false;
            }
            
            tcResultSet formResultSet = getProcessFormData(formDefOps, processFormName); //get all data of a process form
            long processFormKey = Long.parseLong(formResultSet.getStringValue("Structure Utility.Key"));
            int processFormLatestVersion = Integer.parseInt(formResultSet.getStringValue("Structure Utility.Latest Version"));

            //determine if the form version is locked
            if(isFormVersionLocked(formDefOps, processFormKey, processFormLatestVersion))
            {
                System.out.println("[Error]: Process form version "+ processFormLatestVersion + " is locked.");
                return false;
            }
            
            //Second line contains the attributes of a process form field
            String fieldAttributes = br.readLine();
            StringTokenizer attributeNameToken = new StringTokenizer(fieldAttributes, "\t"); 
            
            while(attributeNameToken.hasMoreTokens())
            {
                //System.out.println();
                String fieldAttributeName = attributeNameToken.nextToken().toLowerCase();
                
                //Check if the name of the attribute is valid
                if(fieldAttributeName.equalsIgnoreCase(FIELDLABEL))
                {
                    pf_fieldAttributeNameArray.add(FIELDLABEL);
                }
                
                else if(fieldAttributeName.equalsIgnoreCase(VARIANTTYPE))
                {
                    pf_fieldAttributeNameArray.add(VARIANTTYPE );
                }
                
                else if(fieldAttributeName.equalsIgnoreCase(FIELDTYPE))
                {
                    pf_fieldAttributeNameArray.add(FIELDTYPE);
                }
                          
                else if(fieldAttributeName.equalsIgnoreCase(LENGTH))
                {
                    pf_fieldAttributeNameArray.add(LENGTH);
                }
                
                else if(fieldAttributeName.equalsIgnoreCase(ORDER))
                {
                    pf_fieldAttributeNameArray.add(ORDER);
                }
                
                else
                {
                    System.out.println("Field attribute name " + fieldAttributeName + "is invalid."
                    + "Here are all the possible attribute names:\n "
                    + FIELDLABEL + "\n" +
                    VARIANTTYPE + "\n" +
                    FIELDTYPE + "\n" +
                    LENGTH + "\n" +
                    ORDER );
                    return false;
                }
   
            }
            
            //Validate that the "field_label" attribute name is specified the file
            if(!pf_fieldAttributeNameArray.contains(FIELDLABEL))
            {
                System.out.println("'"+ FIELDLABEL + "' is a required attribute to be specified in file");
                return false;
            }
            
            //Read each process form field from file
            while ((strLine = br.readLine()) != null)  
            {
                //System.out.println(strLine);
                StringTokenizer fieldAttributeValueToken = new StringTokenizer(strLine, "\t");
                int numFieldAttributeNames = pf_fieldAttributeNameArray.size();
                int numTokens = fieldAttributeValueToken.countTokens();
                ProcessFormField processFormFieldObj = new ProcessFormField(processFormKey, processFormLatestVersion);
                
                if(numFieldAttributeNames != numTokens)
                {
                    System.out.println("[Warning]: Size of row is invalid. Field will not be added:\n" + strLine);
                    continue;
                }
                
                boolean isFieldRecordFromFileValid = true;
                
                for(int i = 0; i < numFieldAttributeNames; i++)
                {
                    String fieldAttributeName = pf_fieldAttributeNameArray.get(i);
                    
                    if(fieldAttributeName.equalsIgnoreCase(FIELDLABEL))
                    {
                        String fieldName = fieldAttributeValueToken.nextToken();
                         
                        //Check if the field label exist
                        if(doesFormFieldLabelExists(formDefOps, processFormKey, processFormLatestVersion, fieldName))
                        {
                            System.out.println("[Warning]: Field label '" + fieldName + "' exists. Field will not be added:\n" + strLine);
                            isFieldRecordFromFileValid = false;
                            break;
                        }
                        
                        processFormFieldObj.setFieldName(fieldName);
                    }

                    else if(fieldAttributeName.equalsIgnoreCase(VARIANTTYPE))
                    {
                        String variantType = fieldAttributeValueToken.nextToken();
                        
                         //check if the variant type is valid
                        if(!isFieldVariantTypeValid(variantType))
                        {
                            System.out.println("[Warning]: Variant type '" + variantType + "' is not valid. Field will not be added:\n" + strLine);
                            isFieldRecordFromFileValid = false;
                            break; 
                        }
                        
                        processFormFieldObj.setVariantType(variantType);
                    }

                    else if(fieldAttributeName.equalsIgnoreCase(FIELDTYPE))
                    {
                        
                        String fieldType = fieldAttributeValueToken.nextToken();
                        
                        //check if the field type is valid
                        if(!isFieldTypeValid(fieldType))
                        {
                            System.out.println("[Warning]: Field type '" + fieldType + "' is not valid. Field will not be added:\n" + strLine);
                            isFieldRecordFromFileValid = false;
                            break; 
                        }
                        
                        processFormFieldObj.setFieldType(fieldType);
                    }

                    else if(fieldAttributeName.equalsIgnoreCase(LENGTH))
                    {
                        String length = fieldAttributeValueToken.nextToken();
                        
                        //Check if length is an int type
                        if(!HelperUtility.isInteger(length))
                        {
                            System.out.println("[Warning]: Length '" + length + "' is not valid. Field will not be added:\n" + strLine);
                            isFieldRecordFromFileValid = false;
                            break; 
                        }
                        
                        int numLength = Integer.parseInt(length);
                        processFormFieldObj.setLength(numLength);
                    }

                    else if(fieldAttributeName.equalsIgnoreCase(ORDER))
                    {
                        String order = fieldAttributeValueToken.nextToken();
                        
                         //Check if order is an integer
                        if(!HelperUtility.isInteger(order))
                        {
                            System.out.println("[Warning]: Order '" + order+ "' is not valid. Field will not be added:\n" + strLine);
                            isFieldRecordFromFileValid = false;
                            break; 
                        }
                        
                        int numOrder = Integer.parseInt(order);
                        processFormFieldObj.setOrder(numOrder);
                    }
                }
                
                //add form field object if field record in file is valid
                if(isFieldRecordFromFileValid)
                {
                    pf_fieldRecordArray.add( processFormFieldObj); 
                }
                
            }
            
            System.out.println(pf_fieldRecordArray.toString());
            
            //Add fields to the process form
            for(ProcessFormField obj: pf_fieldRecordArray)
            {
                try 
                {
                    addFieldToProcessForm(formDefOps, obj);
                } 
                
                catch (tcInvalidAttributeException ex) 
                {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                } 
                
                catch (tcAddFieldFailedException ex)
                {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                catch (tcAPIException ex)
                {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                catch (tcFormNotFoundException ex) 
                {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
            
            return true;
        } 
        
        catch (FileNotFoundException ex) 
        {
            Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        catch (IOException ex) 
        { 
            Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         finally
        {
            if(br != null)
            {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            if(in != null)
            {
                try {
                    in.close(); //Close the input stream
                } catch (IOException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            if(fstream != null)
            {
                try {
                    fstream.close();
                } catch (IOException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }      
        
        return false;
            
    }
    
    /*
     * Uses a flat file as a datasource to remove fields from the latest version of a process form.
     * File must be in a specific format.
     * 
     * Sanity - 
     * Check to make sure field label exists. 
     * Check for duplication in file are not process.
     * 
     * File Format
     * <Name of Process Form>
     * <Field Label1>
     * <Field Label2>
     * 
     */
    public static boolean removeFieldsFromProcessFormDSFF(tcFormDefinitionOperationsIntf formDefOps, String fileName) throws tcAPIException, tcColumnNotFoundException, tcFormNotFoundException
    {    
        FileInputStream fstream = null;
        DataInputStream in = null;
        BufferedReader br = null;
            
        try 
        {    
            fstream = new FileInputStream(fileName); //Open File
            in = new DataInputStream(fstream); //Get the object of DataInputStream
            br = new BufferedReader(new InputStreamReader(in));
            
            String strLine; //var to store a line of a file
            HashMap<Long, Object> fieldKeys = new HashMap<Long, Object>();

            //First line of the file should be the name of the process form
            String processFormName = br.readLine();
            if(doesProcessFormExist(formDefOps, processFormName) == false)
            {
                System.out.println("[Error]: Process form name "+ processFormName + " does not exist.");
                return false;
            }
            
            tcResultSet formResultSet = getProcessFormData(formDefOps, processFormName); //get all data of a process form
            long processFormKey = Long.parseLong(formResultSet.getStringValue("Structure Utility.Key"));
            int processFormLatestVersion = Integer.parseInt(formResultSet.getStringValue("Structure Utility.Latest Version"));

            //determine if the form version is locked
            if(isFormVersionLocked(formDefOps, processFormKey, processFormLatestVersion))
            {
                System.out.println("[Error]: Process form version "+ processFormLatestVersion + " is locked.");
                return false;
            }
            
            //Read each process form field from file
            while ((strLine = br.readLine()) != null)  
            {   
                String fieldLabel = strLine;
                         
                //Check if the field label exist
                if(!doesFormFieldLabelExists(formDefOps, processFormKey, processFormLatestVersion, fieldLabel))
                {
                    System.out.println("[Warning]: Field label '" + fieldLabel + "' does not exists.");
                    continue;
                }
                
                //get form field key
                Long fieldKey = getFieldKeyByFieldLabel(formDefOps, processFormKey, processFormLatestVersion, fieldLabel);
                
                if(fieldKey != null)
                {
                    //check if the field has been added to map
                    if(fieldKeys.containsKey(fieldKey))
                    {
                        System.out.println("[Warning]: Duplication of field label '" + fieldLabel + "' does not exists.");
                        continue;
                    }
                    //add form key to hash map
                    fieldKeys.put(fieldKey, "");
                }
            }

            System.out.println(fieldKeys.toString());
            
            //Remove fields from the process form 
            Iterator it = fieldKeys.entrySet().iterator();
    
            while (it.hasNext()) 
            {   
                Map.Entry pairs = (Map.Entry)it.next();
                Long fieldKey = (Long) pairs.getKey();
                try {
                    removeFormField(formDefOps, fieldKey);
                } catch (tcFormFieldNotFoundException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                } catch (tcDeleteNotAllowedException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                } catch (tcAPIException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
                it.remove(); // avoids a ConcurrentModificationException
            }

            return true;
        } 
        
        catch (FileNotFoundException ex) 
        {
            Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        catch (IOException ex) 
        { 
            Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         finally
        {
            if(br != null)
            {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            if(in != null)
            {
                try {
                    in.close(); //Close the input stream
                } catch (IOException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            if(fstream != null)
            {
                try {
                    fstream.close();
                } catch (IOException ex) {
                    Logger.getLogger(ProcessFormFieldUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }      
        
        return false;
            
    }
    
    /*
     * Print all the column names assiocated with a process form.
     * @params - 
     *          formDefOps - tcFormDefinitionOperationsIntf service object

     * Table References -
     *      SDK - information on process form
     * 
     * Structure Utility.Key  [SDK_KEY]
     * Structure Utility.Child Tables.Parent Key [SDK_KEY]
     * Structure Utility.Table Name [SDK_NAME]
     * Structure Utility.Description
     * Structure Utility.Request Table
     * Structure Utility.Form Description
     * Structure Utility.Schema [SDK_SCHEMA]
     * Structure Utility.Note
     * Form Information.key [WIN_KEY]
     * Structure Utility.Form Type [SDK_TYPE]
     * Data Object Manager.key
     * Structure Utility.Row Version
     * Structure Utility.Latest Version
     * Structure Utility.Structure Utility Version Label.Latest Version Label
     * Structure Utility.Active Version
     * Structure Utility.Structure Utility Version Label.Active Version Label
     * SDK_CURRENT_VERSION
     * CURRENT_SDL_LABEL
     */
    public static void printProcessFormColumnNames(tcFormDefinitionOperationsIntf formDefOps) throws tcAPIException
    {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Structure Utility.Table Name", "UD_*");
        tcResultSet processFormResultSet = formDefOps.findForms(map);
        String[] columnNames = processFormResultSet.getColumnNames();
        
        for(String columnName: columnNames)
        {
            System.out.println(columnName);
        }
    }
    
    /*
     * Print all the important data of each process form.
     * @params - 
     *          formDefOps - tcFormDefinitionOperationsIntf service object
     */
    public static void printAllProcessFormInfo(tcFormDefinitionOperationsIntf formDefOps) throws tcAPIException, tcColumnNotFoundException
    {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Structure Utility.Table Name", "UD_*");
        tcResultSet processFormResultSet = formDefOps.findForms(map);
        int numRows = processFormResultSet.getTotalRowCount();
        
        System.out.printf("%-30s%-30s%-30s%-30s\n","Process Form Key", "Process Form Table Name", "Active Version", "Latest Version");
        for(int i = 0; i < numRows; i++)
        {
            processFormResultSet.goToRow(i);
            String processFormKey = processFormResultSet.getStringValue("Structure Utility.Key");
            String processFormTableName = processFormResultSet.getStringValue("Structure Utility.Table Name");
            String processFormActiveVersion = processFormResultSet.getStringValue("Structure Utility.Active Version");
            String processFormLatestVersion = processFormResultSet.getStringValue("Structure Utility.Latest Version");
            
            System.out.printf("%-30s%-30s%-30s%-30s\n",processFormKey, processFormTableName, processFormActiveVersion,processFormLatestVersion);
        }
    }
    
    /*
     * Print all the columm names of a process form field.
     * @params - 
     *          formDefOps - tcFormDefinitionOperationsIntf service object
     * 
     * Table References
     *      SDC -information on process form fields
     * 
     * Structure Utility.Additional Columns.Key
     * Structure Utility.Key
     * Structure Utility.Additional Columns.Name
     * Structure Utility.Additional Columns.Variant Type
     * Structure Utility.Additional Columns.Length
     * Structure Utility.Additional Columns.Field Label
     * Structure Utility.Additional Columns.Field Type
     * Structure Utility.Additional Columns.Default Value
     * Structure Utility.Additional Columns.Order
     * Structure Utility.Additional Columns.Profile Enabled
     * Structure Utility.Additional Columns.Encrypted
     * Structure Utility.Additional Columns.Row Version
     * Structure Utility.Additional Columns.Version
     * EDITABLE
     * OPTIONAL
     * DISCRIMINABLE
     * VISIBLE
     * LOOKUPCODE
     */
    public static void printProcessFormFieldColumnNames(tcFormDefinitionOperationsIntf formDefOps) throws tcAPIException, tcColumnNotFoundException, tcFormNotFoundException
    {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Structure Utility.Table Name", "UD_*");
        tcResultSet processFormResultSet = formDefOps.findForms(map);
        Long processFormKey = null;
        Integer processFormLatestVersion = null;
        int totalProcessFormRecords = processFormResultSet.getTotalRowCount();
        
        if(totalProcessFormRecords == 0)
        {
            System.out.println("Cannot get information. No process form exist.");
            return;
        }
        
        for(int i = 0; i < totalProcessFormRecords; i++)
        {
            processFormResultSet.goToRow(i);
            processFormKey = Long.parseLong(processFormResultSet.getStringValue("Structure Utility.Key"));
            processFormLatestVersion = Integer.parseInt(processFormResultSet.getStringValue("Structure Utility.Latest Version"));
            break;
        }
        
        tcResultSet processFormFieldResultSet = formDefOps.getFormFields(processFormKey, processFormLatestVersion); 
        String[] columnNames = processFormFieldResultSet.getColumnNames();
        
        for(String columnName : columnNames)
        {
            System.out.println(columnName);
        }
    }
    
    /*
     * Print all the fields of a process form in proper file format for this utility.
     * @params 
     *       formDefOps - tcFormDefinitionOperationsIntf service object
     *       processFormName - Name of the process form table
     */
    public static void printProcessFormFieldsFileFormatAdd(tcFormDefinitionOperationsIntf formDefOps, String processFormName) throws tcAPIException, tcFormNotFoundException, tcColumnNotFoundException
    {
        tcResultSet formResultSet = getProcessFormData(formDefOps, processFormName);
        tcResultSet processFieldResultSet = formDefOps.getFormFields(Long.parseLong(formResultSet.getStringValue("Structure Utility.Key")), Integer.parseInt(formResultSet.getStringValue("Structure Utility.Latest Version"))); 
        int numRows = processFieldResultSet.getTotalRowCount();
        
        System.out.println(processFormName);
        System.out.printf("%s\t%s\t%s\t%s\t%s\n", "Field_Label", "Variant_Type", "Field_Type" ,"Length", "Order");
        for(int i = 0; i < numRows; i++)
        {
            processFieldResultSet.goToRow(i);
            String processFormFieldLabel = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Field Label");
            String processFormFieldVariantType = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Variant Type");
            String processFormFieldType = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Field Type");
            String processFormFieldLength = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Length");
            String processFormFieldOrder = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Order");
            
            
            System.out.printf("%s\t%s\t%s\t%s\t%s\n",processFormFieldLabel, processFormFieldVariantType, processFormFieldType ,processFormFieldLength, processFormFieldOrder);
        }
    }
    
    
    /*
     * Print all the fields of a process form.
     * @params 
     *       formDefOps - tcFormDefinitionOperationsIntf service object
     *       processFormKey - SDK_KEY
     *       processFormVersion - SDK_LATEST_VERSION, SDK_ACTIVE_VERSION, or any sdk version; Do not use the version label.
     */
    public static void printProcessFormFields(tcFormDefinitionOperationsIntf formDefOps, long processFormKey, int processFormVersion) throws tcAPIException, tcFormNotFoundException, tcColumnNotFoundException
    {
        tcResultSet processFieldResultSet = formDefOps.getFormFields(processFormKey, processFormVersion); 
        int numRows = processFieldResultSet.getTotalRowCount();
        
        System.out.printf("%-10s%-12s%-30s%-15s%-10s%-25s%-7s\n","Form Key" ,"Field Key", "Field Column Name", "Variant Type", "Length", "Field Label", "Order");
        for(int i = 0; i < numRows; i++)
        {
            processFieldResultSet.goToRow(i);
            String processFormKey_SDK_KEY = processFieldResultSet.getStringValue("Structure Utility.Key"); 
            String processFormFieldKey = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Key");
            String processFormFieldColumnName = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Name");
            String processFormFieldVariantType = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Variant Type");
            String processFormFieldLength = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Length");
            String processFormFieldLabel = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Field Label");
            String processFormFieldOrder = processFieldResultSet.getStringValue("Structure Utility.Additional Columns.Order");
            
            System.out.printf("%-10s%-12s%-30s%-15s%-10s%-25s%-7s\n", processFormKey_SDK_KEY ,processFormFieldKey, processFormFieldColumnName,processFormFieldVariantType, processFormFieldLength, processFormFieldLabel,processFormFieldOrder);
        }
    }
    
    /*
     * Add a field to a process form.
     * @params 
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormFieldObject - process form field object
     * 
     * NOTE: The API appends a number to column name if the column name already exist.
     * In Design Console you have the ability the to decide a field's column name. But
     * through the API, you do not. The column name is determined by the field label for the API.  
     *     
     */
    public static void addFieldToProcessForm(tcFormDefinitionOperationsIntf formDefOps, ProcessFormField processFormFieldObj) throws tcAPIException, tcFormNotFoundException, tcInvalidAttributeException, tcAddFieldFailedException
    {
        long processFormKey = processFormFieldObj.getProcessFormKey() ;
        int processFormVersion = processFormFieldObj.getProcessFormVersion();
        String fieldName = processFormFieldObj.getFieldName();
        String fieldType = processFormFieldObj.getFieldType();
        String variantType = processFormFieldObj.getVariantType();
        int length = processFormFieldObj.getLength();
        int order = processFormFieldObj.getOrder();
        String defaultValue = processFormFieldObj.getDefaultValue();
        String profileEnabled = processFormFieldObj.getProfileEnabled();
        boolean secure = processFormFieldObj.getSecure();
       
        formDefOps.addFormField(processFormKey, processFormVersion,  fieldName, fieldType, variantType, length, order, defaultValue, profileEnabled, secure); 
    }
    
    /*
     * Determines if a process form exists.
     * @params 
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormName - table name of the process form
     */
    public static boolean doesProcessFormExist(tcFormDefinitionOperationsIntf formDefOps, String processFormName) throws tcAPIException, tcColumnNotFoundException
    {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Structure Utility.Table Name", processFormName);
        tcResultSet processFormResultSet = formDefOps.findForms(map);
        
        if(processFormResultSet.isEmpty())
        {
            return false;
        }
        
        return true;
    }
    
    /*
     * Get the process form name by process form key.
     * @params 
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormKey - process form key (SDK.SDK_KEY)
     */
    public static String getProcessFormNameByFormKey(tcFormDefinitionOperationsIntf formDefOps, Long processFormKey) throws tcAPIException, tcColumnNotFoundException
    {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Structure Utility.Key", processFormKey.toString());
        tcResultSet processFormResultSet = formDefOps.findForms(map);
        int numRows = processFormResultSet.getTotalRowCount();
        
        
        for(int i = 0; i < numRows; i++)
        {
            processFormResultSet.goToRow(i);
            String processFormName = processFormResultSet.getStringValue("Structure Utility.Table Name");
            return processFormName;
        }
        
        return null;
    }
    
    /*
     * Get all the data of a process form.
     * @params 
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormName - table name of the process form
     * 
     * @return - tcResultSet pointing to the process form record
     */
    public static tcResultSet getProcessFormData(tcFormDefinitionOperationsIntf formDefOps, String processFormName) throws tcAPIException, tcColumnNotFoundException
    {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Structure Utility.Table Name", processFormName);
        tcResultSet processFormResultSet = formDefOps.findForms(map);
        int numRows = processFormResultSet.getTotalRowCount();
        
        for(int i = 0; i < numRows; i++)
        {
            processFormResultSet.goToRow(i);
            return processFormResultSet;
        }
        
        return null;
    }
    
    /*
     * Get the process form name by process form key.
     * @params 
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormKey - process form key (SDK.SDK_KEY)
     * 
     * @return - tcResultSet pointing to the process form record
     */
    public static tcResultSet getProcessFormData(tcFormDefinitionOperationsIntf formDefOps, Long processFormKey) throws tcAPIException, tcColumnNotFoundException
    {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Structure Utility.Key", processFormKey.toString());
        tcResultSet processFormResultSet = formDefOps.findForms(map);
        int numRows = processFormResultSet.getTotalRowCount();
        
        for(int i = 0; i < numRows; i++)
        {
            processFormResultSet.goToRow(i);
            return processFormResultSet;
        }
        
        return null;
    }
    
    /*
     * Get all the fields of a process form
     * params 
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormKey - SDK_KEY
     *      processFormVersion - SDK_LATEST_VERSION, SDK_ACTIVE_VERSION, or any sdk version; Do not use the version label.
     * 
     */
    public static tcResultSet getAllProcessFormFields(tcFormDefinitionOperationsIntf formDefOps, long processFormKey, int processFormVersion) throws tcAPIException, tcFormNotFoundException
    {
        return formDefOps.getFormFields(processFormKey, processFormVersion); 
    }
    
    /*
     * Get the form field key by field label.
     * @params
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormKey - SDK_KEY
     *      processFormVersion - SDK_LATEST_VERSION, SDK_ACTIVE_VERSION, or any sdk version; Do not use the version label.
     *      fieldLabelCheck - field label to be checked
     * 
     * @return - field key [Structure Utility.Additional Columns.Key] SDC_KEY
     */
    public static Long getFieldKeyByFieldLabel(tcFormDefinitionOperationsIntf formDefOps, long processFormKey, int processFormVersion, String fieldLabelCheck) throws tcAPIException, tcFormNotFoundException, tcColumnNotFoundException
    {
        tcResultSet fieldResultSet = getAllProcessFormFields(formDefOps, processFormKey, processFormVersion);
        int numRows = fieldResultSet.getTotalRowCount();
        
        for(int i = 0; i < numRows; i++)
        {
            fieldResultSet.goToRow(i);
            String fieldLabel = fieldResultSet.getStringValue("Structure Utility.Additional Columns.Field Label");
            
            if(fieldLabel.equals(fieldLabelCheck))
            {
                return Long.parseLong(fieldResultSet.getStringValue("Structure Utility.Additional Columns.Key"));
            }
        }

        return null;
    }
    
    /*
     * Determines if a form field label exists.
     * TODO: Optimize with direct SQL Query
     * @params
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormKey - the form key (SDK_KEY)
     *      processFormVersion - SDK_LATEST_VERSION, SDK_ACTIVE_VERSION, or any sdk version; Do not use the version label.
     *      fieldLabelCheck - field label to be checked
     * 
     * @return - boolean value to indicate if form label exists in the given process form
     */
    public static boolean doesFormFieldLabelExists(tcFormDefinitionOperationsIntf formDefOps, long processFormKey, int processFormVersion, String fieldLabelCheck) throws tcAPIException, tcFormNotFoundException, tcColumnNotFoundException
    {
        tcResultSet fieldResultSet = getAllProcessFormFields(formDefOps, processFormKey, processFormVersion);
        int numRows = fieldResultSet.getTotalRowCount();
        
        for(int i = 0; i < numRows; i++)
        {
            fieldResultSet.goToRow(i);
            String fieldLabel = fieldResultSet.getStringValue("Structure Utility.Additional Columns.Field Label");
            
            if(fieldLabel.equals(fieldLabelCheck))
            {
                return true;
            }
        }

        return false;
    }
    
    /*
     * Determine if the variant type of a process form field is valid.
     * @param 
     *      variantType - name of variant type
     * 
     * @return - boolean value to indicate if a variant type is valid
     */
    public static boolean isFieldVariantTypeValid(String variantType)
    {
        return variantType.equalsIgnoreCase(VT_BYTE) || variantType.equalsIgnoreCase(VT_DOUBLE)  
                || variantType.equalsIgnoreCase(VT_INTEGER) || variantType.equalsIgnoreCase(VT_STRING) 
                || variantType.equalsIgnoreCase(VT_SHORT) || variantType.equalsIgnoreCase(VT_LONG)
                || variantType.equalsIgnoreCase(VT_DATE) || variantType.equalsIgnoreCase(VT_BOOLEAN)
                || variantType.equalsIgnoreCase(VT_BYTEARRAY);
    }
    
    /*
     * Determine if the field type of a process form field is valid.
     * @param
     *      fieldType - name of field type
     * 
     * @return - boolean value to indicate if a variant type is valid
     */
    public static boolean isFieldTypeValid(String fieldType)
    {
        return fieldType.equalsIgnoreCase(FT_TEXTFIELD) || fieldType.equalsIgnoreCase(FT_RADIOBUTTON)  
        || fieldType.equalsIgnoreCase(FT_DATEFIELDWITHDIALOG) || fieldType.equalsIgnoreCase(FT_DISPLAYONLYFIELD) 
        || fieldType.equalsIgnoreCase(FT_PASSWORDFIELD) || fieldType.equalsIgnoreCase(FT_CHECKBOX)
        || fieldType.equalsIgnoreCase(FT_DATACOMBOBOX) || fieldType.equalsIgnoreCase(FT_TEXTAREA)
        || fieldType.equalsIgnoreCase(FT_ITRESOURCELOOKUPFIELD) || fieldType.equalsIgnoreCase(FT_LOOKUPFIELD);
    }
            
    /*
     * Determine if a process form version is locked. Locked means the form has
     * been active before and no create, delete, or update operations can be 
     * performed on the form. Assumes a correct form version is passed in.
     * 
     * Table Reference - 
     *      SDL - contains records with process form versions
     * 
     * Structure Utility.Structure Utility Version Label.Key
     * Structure Utility.Structure Utility Version Label.Parent Label
     * SDL_CURRENT_VERSION
     * Structure Utility.Structure Utility Version Label.Version Label
     * SDL_LOCK
     * Structure Utility.Active Version
     * Structure Utility.Latest Version
     * 
     * @params -
     *      formDefOps - tcFormDefinitionOperationsIntf service object
     *      processFormKey - process form key (SDK.SDK_KEY)
     *      processFormVersion - version to be checked against SDL_CURRENT_VERSION
     * 
     * @return - boolean value to indicate if the given form version is locked.
     *      
     */
    public static boolean isFormVersionLocked(tcFormDefinitionOperationsIntf formDefOps, long processFormKey, int processFormVersion) throws tcAPIException, tcFormNotFoundException, tcColumnNotFoundException
    {
        tcResultSet formVersionInfoResultSet = formDefOps.getFormVersions(processFormKey);
        int numRows = formVersionInfoResultSet.getRowCount();
       
        for(int i = 0; i < numRows; i++)
        {
            formVersionInfoResultSet.goToRow(i);
            int formVersionCompare = Integer.parseInt(formVersionInfoResultSet.getStringValue("SDL_CURRENT_VERSION"));
            //System.out.println(formVersionCompare);
            
            if(formVersionCompare == processFormVersion)
            {
                String isLocked = formVersionInfoResultSet.getStringValue("SDL_LOCK");
                //System.out.println(isLocked);
                
                if(isLocked == null || isLocked.isEmpty() || Integer.parseInt(isLocked) == 0)
                {
                    return false;
                }
                
                else
                {
                    return true;
                }
            }

        }
        
        System.out.println("form version does not exist");
        return true;
    }
    
    /*
     * Removes a field from a process form.
     * @param -
     *          formDefOps - tcFormDefinitionOperationsIntf service object
     *          formFieldKey - key of the form field (SDC.SDC_KEY)
     *      
     * Note: Form Fields with a mapping to a reconciliation field can only be deleted when 
     * the mapping is removed.
     */
    public static void removeFormField(tcFormDefinitionOperationsIntf formDefOps, Long formFieldKey) throws tcAPIException, tcFormFieldNotFoundException, tcDeleteNotAllowedException
    {      
        formDefOps.removeFormField(formFieldKey);
    }
}