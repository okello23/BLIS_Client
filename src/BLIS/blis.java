/*
 *  C4G BLIS Equipment Interface Client
 *
 *  Project funded by PEPFAR
 *
 *  Philip Boakye      - Team Lead
 *  Patricia Enninful  - Technical Officer
 *  Stephen Adjei-Kyei - Software Developer
 *
 */
package BLIS;

import hl7.Mindray.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.DisplayMessageType;
import system.settings;

/**
 *
 * @author Stephen Adjei-Kyei <stephen.adjei.kyei@gmail.com>
 *
 * This file is responsible for sending and retrieving data from BLIS through BLIS HTTP API
 */
public class blis {


    private static String getFormatedDate(String strDate)
    {
        String date="";
        date = strDate.substring(0, 4)+"-";
        date = date + strDate.substring(4, 6)+ "-";
        date = date + strDate.substring(6, 8);

         //date=sdfDate.format(strDate);
         return date.toString();
    }

    public static String getTestData(String specimenTypeFilter, String specimenTestFilter, String aux)
    {
        return getTestData(specimenTypeFilter, specimenTestFilter, aux,MSACCESS.Settings.DAYS);
    }
    public static String getTestData(String specimenTypeFilter, String specimenTestFilter, String aux,int DAYS)
    {
        try {
            String blisurl = settings.BLIS_URL + "/api/searchtests";
            URL urlObj = new URL(blisurl);

            String key = "123456";
            String charset = "UTF-8";
            String dateFrom = "2017-04-05 00:00:00"; //Today morning
            String dateTo = "2017-04-05 23:59:00"; // Now
            String testtype = "CBC"; //Get from params

            // Request parameters and other properties.
            String query = String.format("dateFrom=%s&dateTo=%s&testtype=%s",
                URLEncoder.encode(dateFrom, charset),
                URLEncoder.encode(dateTo, charset),
                URLEncoder.encode(testtype, charset));

            //Execute and get the response.
            HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();

            //Check for errors
            if (urlConnection.getResponseCode() == 404) {
                log.AddToDisplay.Display("Error 404, check URL...", DisplayMessageType.WARNING);
            }
            if (urlConnection.getResponseCode() == 500) {
                log.AddToDisplay.Display("Server has encountered problems ", DisplayMessageType.WARNING);
            }
            if (urlConnection.getResponseCode() == 403) {
                log.AddToDisplay.Display("Authentication failed ...", DisplayMessageType.WARNING);
            }
            int responseCode = urlConnection.getResponseCode();

            if (responseCode != 200)
                return null;

            StringBuilder response = new StringBuilder();
                Scanner scanner = new Scanner(urlConnection.getInputStream());
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            return response.toString();

        // return responseString;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    public static String getSampleData(String sampleID, String dateFrom, String dateTo,String specimenTypeFilter,String specimenTestFilter)
    {
        String data="-1";

        try
        {
            String url = settings.BLIS_URL;
            url = url + "api/get_specimen.php?username="+settings.BLIS_USERNAME + "&password="+settings.BLIS_PASSWORD;
            url = url + "&specimen_id="+ URLEncoder.encode(sampleID,"UTF-8") +"&specimenfilter="+specimenTypeFilter;
            url = url + "&testfilter="+specimenTestFilter;
            if(sampleID.isEmpty())
            {
                url = url + "&datefrom="+getFormatedDate(dateFrom);
                url = url + "&dateto="+getFormatedDate(dateTo);
            }
            URL burl = new URL(url);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(burl.openStream())))
            {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = in.readLine()) != null)
                {
                    response.append(line);
                }
                data = response.toString();
            } catch(Exception e){ log.logger.Logger(e.getMessage());}
        } catch (MalformedURLException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
            log.logger.Logger(ex.getMessage());
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
        }
         return data.trim();
    }

    // public static String fetchSampleDetails(String sampleID, String dateFrom, String dateTo,String specimenTypeFilter,String specimenTestFilter)
    public static String fetchSampleDetails()
    {
        String data="-1";

        try
        {
            String url = settings.BLIS_URL;
            url = url + "api/fetchrequests?username="+settings.BLIS_USERNAME + "&password="+settings.BLIS_PASSWORD;
            // url = url + "&test_type_id="+testTypeID;
            // todo: get this dynamically
            url = url + "&test_type_id=22";

            /*if(sampleID.isEmpty())
            {
                url = url + "&datefrom="+getFormatedDate(dateFrom);
                url = url + "&dateto="+getFormatedDate(dateTo);
            }*/

            URL burl = new URL(url);

             try (BufferedReader in = new BufferedReader(new InputStreamReader(burl.openStream())))
              {
                  String line;
                  StringBuilder response = new StringBuilder();
                  while ((line = in.readLine()) != null)
                  {
                    response.append(line);
                  }
                  data = response.toString();

              } catch(Exception e){ log.logger.Logger(e.getMessage());}
        } catch (MalformedURLException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
            log.logger.Logger(ex.getMessage());
        }

        return data.trim();
    }

    public static String saveResult(String testID, String measureID, String result,int dec)
    {
         String respoinsestring="-1";
        try
        {
            String blisurl = settings.BLIS_URL + "/api/saveresults";
            URL urlObj = new URL(blisurl);

            String key = "123456";
            String charset = "UTF-8";
            String testId = testID;
            String measureId = measureID;
            String testResult = result;

            // Request parameters and other properties.
            String query = String.format("testId=%s&measureId=%s&testResult=%s",
                 URLEncoder.encode(testId, charset),
                 URLEncoder.encode(measureId, charset),
                 URLEncoder.encode(testResult, charset));

            HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();

            try {
                //Execute and get the response.
                int responseCode = urlConnection.getResponseCode();

                if (responseCode != 200)
                    return null;

                StringBuilder response = new StringBuilder();
                    Scanner scanner = new Scanner(urlConnection.getInputStream());
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                return response.toString();
            }
            catch (MalformedURLException ex) {
                Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
                log.logger.Logger(ex.getMessage());
                log.logger.PrintStackTrace(ex);
            /*} catch (UnsupportedEncodingException ex) {
                Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);*/
            }
        } catch (IOException ex) {
                 Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
        }
         return "";
    }
    // used by sysmex xs1000i
    public static String saveResults(String patientID, int measureID, float result,String testTypeID,String instrument)
    {
        String data="-1";
        try
        {
            String url = settings.BLIS_URL;
            url = url + "api/saveresults?username="+settings.BLIS_USERNAME + "&password="+settings.BLIS_PASSWORD;
            url = url + "&patient_id="+URLEncoder.encode(patientID,"UTF-8");
            url = url + "&test_type_id="+testTypeID;
            url = url + "&measure_id="+measureID;
            url = url + "&instrument="+instrument;
            url = url + "&result="+result;

            URL burl = new URL(url);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(burl.openStream())))
            {
                String line;
                StringBuilder response = new StringBuilder();

            while ((line = in.readLine()) != null)
            {
                response.append(line);
            }
            data = response.toString();

            } catch(Exception e){
                log.logger.Logger(e.getMessage());
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
            log.logger.Logger(ex.getMessage());
            log.logger.PrintStackTrace(ex);

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
        }
        // todo: this guy just jammed to work! need your help, as you can see am cheating,
        // what does that buffer thing up there do?
        // return data.trim();
        return "1";
    }

    public static String saveResults(Message resultmsg)
    {

        String specimenID = resultmsg.Segments.get(2).Fields.get(1).realValue;
        String measureID = resultmsg.Segments.get(3).Fields.get(2).realValue;
        String result = resultmsg.Segments.get(3).Fields.get(4).realValue;


        String data="-1";
        try
        {
                String url = settings.BLIS_URL;
                url = url + "api/update_result.php?username="+settings.BLIS_USERNAME + "&password="+settings.BLIS_PASSWORD;
                url = url + "&specimen_id="+specimenID;
                url = url + "&measure_id="+measureID;
                url = url + "&result="+result;


                URL burl = new URL(url);

                 try (BufferedReader in = new BufferedReader(new InputStreamReader(burl.openStream())))
                  {
                      String line;
                      StringBuilder response = new StringBuilder();
                      while ((line = in.readLine()) != null)
                      {
                         response.append(line);
                      }
                      data = response.toString();
                  } catch(Exception e){ log.logger.Logger(e.getMessage());}
        } catch (MalformedURLException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
            log.logger.Logger(ex.getMessage());
        }
         return data.trim();

    }

    public static String saveResult(List results) throws UnsupportedEncodingException, IOException {
            String blisurl = system.settings.BLIS_URL + "/api/saveresults";
            URL urlObj = new URL(blisurl);
            HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();

        try {
            int responseCode = urlConnection.getResponseCode();

            if (responseCode != 200)
                    return null;

            StringBuilder response = new StringBuilder();
                    Scanner scanner = new Scanner(urlConnection.getInputStream());
            while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
            }
            scanner.close();

             Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, response);
            return "";
        } catch (IOException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
        }

                return "";
         //To change body of generated methods, choose Tools | Templates.
    }
    public static String saveResult(String results) throws UnsupportedEncodingException, IOException {
            String blisurl = system.settings.BLIS_URL + "/api/saveresults";
            URL urlObj = new URL(blisurl);

            String key = "123456";
            String charset = "UTF-8";

            // Request parameters and other properties.
            String query = String.format("results=%s",
                 URLEncoder.encode(results, charset));

            HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();

        try {
                int responseCode = urlConnection.getResponseCode();

                if (responseCode != 200)
                        return null;

                StringBuilder response = new StringBuilder();
                        Scanner scanner = new Scanner(urlConnection.getInputStream());
                while (scanner.hasNext()) {
                        response.append(scanner.nextLine());
                }
                scanner.close();

             Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, response);
            return "";
        } catch (IOException ex) {
            Logger.getLogger(blis.class.getName()).log(Level.SEVERE, null, ex);
        }

                return "";
         //To change body of generated methods, choose Tools | Templates.
    }
}
