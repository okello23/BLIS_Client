package RS232;

import configuration.xmlparser;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.DisplayMessageType;
import system.settings;
import system.utilities;

public class HumaCount60TS extends Thread {

    private static List<String> testIDs = new ArrayList<String>();

    private static final char ETX = 0x03;
    public static final char EOT = 0x04;
    public static final char TAB = 0x09;
    private static String ASTMMsgs ="";
     
    private static StringBuilder datarecieved = new StringBuilder();
    
      
  @Override
    public void run() {
        log.AddToDisplay.Display("HumaCount 60TS handler started...", DisplayMessageType.TITLE);
        log.AddToDisplay.Display("Checking available ports on this system...", DisplayMessageType.INFORMATION);
        String[] ports = Manager.getSerialPorts();
        log.AddToDisplay.Display("Avaliable ports:", DisplayMessageType.TITLE);
       for(int i = 0; i < ports.length; i++){           
           log.AddToDisplay.Display(ports[i],log.DisplayMessageType.INFORMATION);
        }
       log.AddToDisplay.Display("Now connecting to port "+RS232Settings.COMPORT , DisplayMessageType.TITLE);
       if(Manager.openPortforData("HumaCount 60TS"))
       {
           log.AddToDisplay.Display("Connected sucessfully",DisplayMessageType.INFORMATION);
           setTestIDs();
       }
    }
    
    public static void HandleDataInput(String data)
    {       
        try
        {
            ASTMMsgs="";
            ASTMMsgs=data;
            ASTMMsgs = ASTMMsgs.replaceAll(String.valueOf(ETX), "");
            // new line of simple texts split using "\n"
            String[] msgParts = ASTMMsgs.trim().split("\n");
            
            int mID=0;
            float value = 0;
            boolean flag = false;
            String PatientID =  msgParts[11].split(String.valueOf(TAB))[1].trim();

            // restrict string manupulation to actual results only
            for(int i=21;i<43;i++){
                // measure id of the instrument, now get mmeasure id of LIS
                mID = getMeasureID(Integer.toString(i));
                if(mID > 0){

                    String rawResult = "";
                    // actual result
                    rawResult = msgParts[i].split(String.valueOf(TAB))[2];
                    String result = rawResult.trim();

                    try
                    {
                        value = Float.parseFloat(result);
                    }catch(NumberFormatException e){
                        //
                    }
                    if(SaveResults(PatientID, mID,value))
                    {
                        flag = true;
                    }
                }
            }
            // when is this flag applicable
            if(flag)
            {
                log.AddToDisplay.Display("\nResults with Code: "+PatientID +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
            }
            else
            {
                log.AddToDisplay.Display("\nSpecimen with Code: "+PatientID +" not Found on BLIS",DisplayMessageType.WARNING);
            }
        }catch(Exception ex)
        {
            log.AddToDisplay.Display("Error:"+ex.getMessage(),DisplayMessageType.ERROR);
        }
    }
    
    public void Stop()
    {
        if(Manager.closeOpenedPort())
        {
            log.AddToDisplay.Display("Port Closed sucessfully", log.DisplayMessageType.INFORMATION);
        }
    }
    
    
    private void setTestIDs()
     {
         String equipmentid = getSpecimenFilter(3);
         String blismeasureid = getSpecimenFilter(4);
        
         String[] equipmentids = equipmentid.split(",");
         String[] blismeasureids = blismeasureid.split(",");
         for(int i=0;i<equipmentids.length;i++)
         {
             testIDs.add(equipmentids[i]+";"+blismeasureids[i]);             
         }
        
     }
    
    private static String getSpecimenFilter(int whichdata)
    {
        String data = "";
        xmlparser p = new xmlparser("configs/HumaCount/humacount60ts.xml");
        try {
            data = p.getMicros60Filter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(ABXPentra80.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return data;        
    }
    
     private static int getMeasureID(String equipmentID)
     {
         int measureid = 0;
         for(int i=0;i<testIDs.size();i++)
         {
             if(testIDs.get(i).split(";")[0].equalsIgnoreCase(equipmentID))
             {
                 measureid = Integer.parseInt(testIDs.get(i).split(";")[1]);
                 break;
             }
         }
         
         return measureid;
     }
     private static String getEquipmentID(String measureID)
     {
         String equipmentID = "";
         for(int i=0;i<testIDs.size();i++)
         {
             if(testIDs.get(i).split(";")[1].equalsIgnoreCase(measureID))
             {
                 equipmentID = testIDs.get(i).split(";")[0];
                 break;
             }
         }
         
         return equipmentID;
     }
    private static boolean SaveResults(String barcode,int MeasureID, float value)
     {
          boolean flag = false;
          String testtypeid = getSpecimenFilter(1);
          if("1".equals(BLIS.blis.saveResults(barcode,MeasureID,value,testtypeid,"HumaCount60TS")))
            {
              flag = true;
            }
         return flag;
     }      
}

