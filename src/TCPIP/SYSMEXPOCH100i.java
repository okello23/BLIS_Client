/*
 *  ASLM-LIS
 *
 */
package TCPIP;

import BLIS.sampledata;
import configuration.xmlparser;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import log.DisplayMessageType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import system.SampleDataJSON;
import system.settings;
import system.utilities;
import java.text.DecimalFormat;
import ui.MainForm;

/**
 *
 */
public class SYSMEXPOCH100i extends Thread{

  String read;
  boolean first =true;
  DataOutputStream outToEquipment=null;
  ServerSocket welcomeSocket=null;
  Socket connSock = null;
  Iterator list= null;
  static Queue<String> OutQueue=new LinkedList<>();
  private static List<String> testIDs = new ArrayList<>();
  private static Queue<String> PatientTest = new LinkedList<>();
  private static String ASTMMsgs ="";

  boolean stopped = false;
  //Queue<String> InQueue=new LinkedList<>();
  private static final char CARRIAGE_RETURN = 13;
  private static final char STX = 0x02;
  private static final char ACK = 0x06;
  private static final char EOT = 0x04;
  private static final char NAK = 0x15;
  private static final char NUL = 0x00;
  private static final char ENQ = 0x05;
  private static final char ETX = 0x03;
  private static final char CR = 0x0D;
   private static final char LF = 0x0A;

  private static MODE appMode = MODE.IDLE;
  public enum  MSGTYPE
  {
    QUERY(0),
    RESULTS(1),
    ACK_RECEIVED(2),
    ACK(3),
    ENQ(4),
    NAK(5),
    EOT(6),
    STX(7),
    ETX(8),
    UNKNOWN(-1);


    private MSGTYPE(int value)
    {
        this.Value = value;
    }

    private int Value;

  }

   enum MODE
  {
       SENDING_QUERY,
       RECEIVEING_RESULTS,
       IDLE;
  }
  public void Stop()
  {
      try {

          stopped = true;
          if(null != connSock)
              connSock.close();
          if(null !=welcomeSocket)
               welcomeSocket.close();
           log.AddToDisplay.Display("SYSMEX pocH-100i handler stopped", DisplayMessageType.TITLE);
      } catch (IOException ex) {
          log.logger.Logger(ex.getMessage());
      }
  }

  @Override
  public void run() {
        try
        {
          log.AddToDisplay.Display("SYSMEX pocH-100i handler started...", DisplayMessageType.TITLE);
          if(tcpsettings.SERVER_MODE)
          {
              log.AddToDisplay.Display("Starting Server socket on port "+tcpsettings.PORT, DisplayMessageType.INFORMATION);
              welcomeSocket = new ServerSocket(tcpsettings.PORT);
              log.AddToDisplay.Display("Waiting for Equipment connection...", DisplayMessageType.INFORMATION);
              log.AddToDisplay.Display("Listening on port "+ tcpsettings.PORT+"...",DisplayMessageType.INFORMATION);
              connSock = welcomeSocket.accept();
          }
          else
          {
              log.AddToDisplay.Display("Starting Client socket on IP "+tcpsettings.EQUIPMENT_IP +" on port  "+tcpsettings.PORT, DisplayMessageType.INFORMATION);
             connSock = new Socket(tcpsettings.EQUIPMENT_IP, tcpsettings.PORT);
          }
          log.AddToDisplay.Display("SYSMEX pocH-100i is now Connected...",DisplayMessageType.INFORMATION);
          first=false;
          if(!tcpsettings.SERVER_MODE)
          {
              connSock.setKeepAlive(true);
          }
          ClientThread client = new ClientThread(connSock,"SYSMEX pocH-100i");
          client.start();
          String message ;
          outToEquipment= new DataOutputStream(connSock.getOutputStream());
           setTestIDs();
          while(!stopped)
          {
            synchronized(OutQueue)
            {
              while(!OutQueue.isEmpty())
              {
                System.out.println("Message found in sending queue");
                log.AddToDisplay.Display("Message found in sending queue",DisplayMessageType.TITLE);
                //log.logger.Logger("Message found in sending queue");
                message =(String) OutQueue.poll();
                outToEquipment.writeBytes(message);
                //System.out.println(message+ "sent sucessfully");
                log.AddToDisplay.Display(message+ "sent successfully",DisplayMessageType.INFORMATION);
                //log.logger.Logger(message+ "sent sucessfully");
              }
            }
          }
        }
        catch(IOException e)
        {
            if(first)
            {
              if(tcpsettings.SERVER_MODE)
                  log.AddToDisplay.Display("could not listen on port :"+tcpsettings.PORT + " "+e.getMessage(),DisplayMessageType.ERROR);
              else
                  log.AddToDisplay.Display("could not connect to server: "+tcpsettings.EQUIPMENT_IP+" on port :"+tcpsettings.PORT + " "+e.getMessage(),DisplayMessageType.ERROR);
             // log.logger.Logger(e.getMessage());
            }
            else
            {
              log.AddToDisplay.Display("SYSMEX pocH-100i client is now disconnected!",DisplayMessageType.WARNING);
              log.logger.Logger(e.getMessage());
            }
	       }
      }

    private static void resetCon()
    {
      //
    }

  private static void AddtoQueue(char value)
  {
    synchronized(OutQueue)
    {
      OutQueue.add(String.valueOf(value));
    }
  }

    private static void AddtoQueue(String data)
    {
      synchronized(OutQueue)
      {
        OutQueue.add(data);
        log.logger.Logger("New message added to sending queue\n"+data);
      }
    }

    public static void handleMessage(String message)
    {
      synchronized(MainForm.set)
        {
          MainForm.set = MainForm.RESET.WAIT;
        }
        try
        {
          //String[] msgParts = message.split("\r");
          MSGTYPE type =getMessageType(message);
          if(type == MSGTYPE.ENQ)
          {
            AddtoQueue(ACK);
            ASTMMsgs="";
            appMode = MODE.RECEIVEING_RESULTS;
          }
          else if(type == MSGTYPE.STX)
          {
              AddtoQueue(ACK);
             ASTMMsgs = ASTMMsgs + "\r"+message;
             appMode = MODE.RECEIVEING_RESULTS;

          }
          else if (type == MSGTYPE.EOT)
          {
            ASTMMsgs = ASTMMsgs.replaceAll(String.valueOf(STX)+"[\\d]", "");
            ASTMMsgs = ASTMMsgs.replaceAll(String.valueOf(ETX), "");
            String[] msgParts = ASTMMsgs.trim().split("\r");
            int mID=0;
            float value = 0;
            boolean flag = false;
            // patient Id
            String PatientID =  msgParts[0].substring(52,73).trim();
log.AddToDisplay.Display(msgParts[0],DisplayMessageType.INFORMATION);
            // Results Data
            String[] DataParts = normalizeData(msgParts[0].substring(78,172));

            for(int i=0;i<DataParts.length;i++)
            {
                mID = getMeasureID(i);
                if(mID > 0)
                {
                    try
                    {
                        value = Float.parseFloat(DataParts[i].trim());
                    }catch(NumberFormatException e){
                        try{
                            value = Float.parseFloat(DataParts[i].trim());
                        }catch(NumberFormatException ex){}
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
            appMode = MODE.IDLE;
            synchronized(MainForm.set)
            {
              MainForm.set = MainForm.RESET.NOW;
            }

          }
          else if (type == MSGTYPE.NAK)
          {
            log.AddToDisplay.Display("NAK Response from Analyzer",DisplayMessageType.ERROR);
          }
          else if(type == MSGTYPE.ACK)
          {
            if(appMode == MODE.SENDING_QUERY)
            {
                if(PatientTest.size()>0)
                {
                  AddtoQueue(PatientTest.poll());
                }
                else
                {
                  AddtoQueue(EOT);
                  Thread.sleep(500);
                  appMode = MODE.IDLE;
                  synchronized(MainForm.set)
                  {
                    MainForm.set = MainForm.RESET.NOW;
                  }
                }
            }
          }
      }catch(Exception ex)
      {
          log.AddToDisplay.Display("Processing Error Occured!",DisplayMessageType.ERROR);
          log.AddToDisplay.Display("Data format of Details received from Analyzer UNKNOWN",DisplayMessageType.ERROR);
          log.logger.PrintStackTrace(ex);
      }
    }

    private static MSGTYPE getMessageType(String msg)
    {
        MSGTYPE type = null;
        switch (msg.charAt(0))
        {
            case ACK:
                type = MSGTYPE.ACK;
                break;
            case ENQ:
                type = MSGTYPE.ENQ;
                break;
            case EOT:
                type =MSGTYPE.EOT;
                break;
            case NAK:
                type = MSGTYPE.NAK;
                break;
            case STX:
                type = MSGTYPE.STX;
                break;
            case ETX:
                type = MSGTYPE.ETX;
                break;
            default:
                String[] parts = msg.split("\r");
                // todo: dont understand the functions of this
                if(parts.length > 1 )
                {
                    if(parts[1].startsWith("Q|"))
                    {
                        type= MSGTYPE.QUERY;
                    }
                    else if (parts[1].startsWith("P|"))
                    {
                        type = MSGTYPE.RESULTS;
                    }
                    else
                    {
                        type =MSGTYPE.UNKNOWN;
                    }
                }
        }
        return type;
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
        xmlparser p = new xmlparser("configs/SYSMEX/SYSMEXPOCH100i.xml");
        try {
            data = p.getMicros60Filter(whichdata);
        } catch (Exception ex) {
            Logger.getLogger(SYSMEXPOCH100i.class.getName()).log(Level.SEVERE, null, ex);
            log.logger.PrintStackTrace(ex);
            log.AddToDisplay.Display(ex.getMessage(), log.DisplayMessageType.ERROR);
        }
        return data;
    }

    private static String[] normalizeData(String data)
    {
      // number characters from the string, that make the results
      int[] normkeys = {5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5};
      // formating of the results
      String[] normformat ={"###.##","###.##","###.##","###.##",
          "###.##","###.##","###.##","###.##","###.##",
          "###.##","###.##","###.##","###.##","###.##","###.##","###.##","###.##","###.##","###.##"};
      DecimalFormat myFormatter;
      // 95 is the size of the string array
      String[] norm = new String[95];
      // start is index of 1st content; indstart is index of ??? content; 
      for(int i=0,start=0,indstart =0;i<normkeys.length;i++,start++)
      {           
          norm[start] = customFormat(data.substring(indstart, indstart+normkeys[i]),normformat[i]);
          indstart = indstart + normkeys[i];
          
          System.out.println("i:"+i+" indstart:"+indstart+"normkeys[i]:"+normkeys[i]);
      }
      return norm;
    }

    private static String customFormat(String value, String pattern)
    {
        String formated ="";
        int ind = 0;
        try
        {
            formated = String.valueOf(Integer.parseInt(value));
           for(int i = pattern.length()-1,in=0;i>=0;i--,in++)
            {
                if(pattern.charAt(i) == '.')
                {
                   ind = in;
                   break;
                }
            }

            if (ind > 0)
            {
                for(int i = value.length()-1,in=0;i>=0;i--,in++)
                {
                   if(in == ind)
                   {
                       formated = value.substring(0,i+1)+"."+value.substring(i+1);
                       formated =String.valueOf(Float.parseFloat(formated));
                        break;
                    }
                }
            }
        }catch(NumberFormatException ex){ formated = "0";}
        return formated;
    }

    private static int getMeasureID(int equipmentID)
    {
        int measureid = 0;
        for(int i=0;i<testIDs.size();i++)
        {
            if(testIDs.get(i).split(";")[0].equalsIgnoreCase(equipmentID+""))
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
    if("1".equals(BLIS.blis.saveResults(barcode,MeasureID,value,testtypeid,"sysmexPOCH-100i")))
    {
      flag = true;
    }
   return flag;
   }
}
