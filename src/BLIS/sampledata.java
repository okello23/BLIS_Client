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

/**
 *
 * @author Stephen Adjei-Kyei <stephen.adjei.kyei@gmail.com>
 * 
 * Data elements that will be retrieved from BLIS
 */
public class sampledata {
    
    public String specimen_id;
    public String time_collected;
    public String time_accepted;
    public String doctor;
    public String patient_name;
    public String gender;
    public String dob;
    public String admission_status;
    public String result;
    public String test_type_id;   
    public String test_type_name;
    public String specimen_type_id;
    public String specimen;
    public String specimen_type_name;
    public String testType;
    public String patient_id;
    public String measure_id;

    @Override
    public String toString()
    {
        return specimen_id;
    }
    
}
