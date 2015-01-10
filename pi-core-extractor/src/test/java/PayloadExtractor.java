
import com.sap.mw.jco.IFunctionTemplate;
import com.sap.mw.jco.IRepository;
import com.sap.mw.jco.JCO;


/**
 * Created with IntelliJ IDEA.
 * User: asanchez
 * Date: 7/07/14
 * Time: 06:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class PayloadExtractor {

    // The MySAP.com system we gonna be using
    static final String SID = "XIP";
    static final String PID = "PID";
    // The repository we will be using

    IRepository repository;
    JCO.Field msgkeyField;


    public PayloadExtractor() {

        try {

            System.out.println("*** Creating the Pool... ***");


            JCO.addClientPool(SID, 10, "001", "user", "pwd", "EN", "host", "00");
            repository = JCO.createRepository("RecoverRepository", SID);
        } catch (JCO.Exception ex) {
            System.out.println("RecoverXI Caught an exception: \n" + ex);
        }

    }


    // Retrieves and prints information about the remote system

    public void getPayload() {

        // A messageID from your XI/PI

        String key = "54B023A054362230E10080000A0A32A6";

        final String pipelineID = "CENTRAL";


        byte[] msgkey = key.getBytes();

        try {

            IFunctionTemplate ftemplate = repository.getFunctionTemplate("SXMB_READ_MESSAGE_VERSION_RAW");

            if (ftemplate != null) {
                System.out.println("*** Creating client and function... ***");

                JCO.Function function = ftemplate.getFunction();
                JCO.Client client = JCO.getClient(SID);

                JCO.Structure struct = function.getImportParameterList().getStructure("MESSAGEKEY");

                struct.setValue(key, "MSGID");
                struct.setValue(pipelineID, "PID");
                // SELECTION must be like this!
                function.getImportParameterList().getField("SELECTION").setValue("2");
                // This is the msg version number, where 000 is the first (Inbound); the last can be caught from the function output (see below).
                // Setting this strongly depends on what you want to get: basically before or after the mapping...
                function.getImportParameterList().getField("VERSION_REQUEST").setValue("000");
                System.out.println("*** Calling... ***");
                client.execute(function);
                JCO.Table tb = function.getExportParameterList().getTable("MESSAGEPAYLOAD");

                if (tb.getNumRows() > 0) {
                    // There could be multiple payloads (even if usually it's only one)
                    do {

                        String plstr = new String(tb.getField("PAYLOAD").getByteArray());
                        System.out.println("*** Payload found *** " + tb.getField("NAME").getString() + " *** BEGIN ***");
                        System.out.println("Message Last Version: "+ function.getExportParameterList().getField("MAXVERSION").getString());

                        System.out.println(plstr);
                        System.out.println("*** Payload found *** " + tb.getField("NAME").getString() + " ***  END  ***");

                    } while (tb.nextRow());

                } else {

                    System.out.println("*** No payload found! ***");

                }

                // Release the client into the pool

                JCO.releaseClient(client);

            } else {

                System.out.println("Function SXMB_READ_MESSAGE_VERSION_RAW not found in backend system.");

            }

        } catch (Exception ex) {

            System.out.println("Caught an exception: \n" + ex);

        }

    }


    protected void cleanUp() {

        System.out.println("*** Cleaning... ***");

        JCO.removeClientPool(SID);

    }


    public static void main(String[] argv) {

        PayloadExtractor e = new PayloadExtractor();

        e.getPayload();

        e.cleanUp();

    }

}