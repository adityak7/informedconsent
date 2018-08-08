

//import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;

/**
 * Created by peeyush on 22/2/17.
 */


public class Server {

    private static System system;
    private static Environment environment;
    private static String SERVER_PROPERTIES_FILE = "server.properties";
    private static String uploadLocationFolder;

    public static String getUploadLocationFolder() {
        return uploadLocationFolder;
    }

    public static void setUploadLocationFolder(String uploadLocationFolder) {
        Server.uploadLocationFolder = uploadLocationFolder;
    }

    public static System getSystem() {
        return system;
    }

    public static void setSystem(System system) {
        Server.system = system;
    }

    public static Environment getEnvironment() {
        return environment;
    }

    public static void setEnvironment(Environment environment) {
        Server.environment = environment;
    }

    public static void init() {
        try {
            Properties prop = new Properties();
            InputStream inputStream = Server.class.getClassLoader().getResourceAsStream(SERVER_PROPERTIES_FILE);
            prop.load(inputStream);

            setSystem(Server.System.fromString(prop.getProperty("system")));
            setEnvironment(Server.Environment.fromString(prop.getProperty("environment")));
            setUploadLocationFolder(prop.getProperty("uploadFolder"));

        } catch (Exception e) {
           // LOGGER.error("Error Initializing Server " + e);
        }

    }

    public enum System {
        UNIX("UNIX"),
        WINDOWS("WINDOWS");

        private String name;

        System(String name) {
            this.name = name;
        }

        public static System fromString(String name) {
            if ("UNIX".equals(name))
                return UNIX;
            else if ("WINDOWS".equals(name))
                return WINDOWS;
            else
                return null;
        }
    }

    public enum Environment {
        DEVELOPMENT("DEVELOPMENT"),
        TESTING("TESTING");

        private String name;

        Environment(String name) {
            this.name = name;
        }

        public static Environment fromString(String name) {
            if ("DEVELOPMENT".equals(name))
                return DEVELOPMENT;
            else if ("TESTING".equals(name))
                return TESTING;
            else
                return null;
        }
    }

}
