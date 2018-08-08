package important; 

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.json.*;
import org.springframework.core.env.Environment;

import important.ApplicationContextProvider;

import javax.ws.rs.core.Response;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

/**
 * Created by primpap on 7/8/15.
 */
public class Helper {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String INFINITE_TIMESTAMP = "2038-12-12 00:00:01";
    private static Environment env;
    private static String DB_URL = null;
    private static String DB_USER = null;
    private static String DB_PASSWORD = null;
    public static Gson gson = Converters.registerDateTime(new GsonBuilder()).setPrettyPrinting().create();

    static {
        env = ApplicationContextProvider.getInstance().getApplicationContext().getEnvironment();
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(
                    Helper.class.getClassLoader().getResource(
                            "META-INF/" + env.getActiveProfiles()[0] + ".properties").getFile());
            prop.load(input);
            DB_URL = prop.getProperty("jdbc.url");
            DB_USER = prop.getProperty("jdbc.username");
            DB_PASSWORD = prop.getProperty("jdbc.password");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Calendar timestampStrToCal(String timestamp) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT);
        try {
            cal.setTime(sdf.parse(timestamp));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return cal;
    }

    public static String calToTimeStampStr(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT);
        return sdf.format(calendar.getTime());
    }

    public static void addRestrictionIfNotNull(Criteria criteria, String propertyName, Object value) {
        if (value != null) {
            criteria.add(Restrictions.eq(propertyName, value));
        }
    }

    public static java.sql.Connection getDBConnection() {

        // Load the Connector/J driver
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            return DriverManager.getConnection(Helper.DB_URL, Helper.DB_USER,
                    Helper.DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void closeConnection(Connection connection) {

        // Load the Connector/J driver
        try {
            connection.close();
        }  catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static Response createResponse(int errorCode, String errorMessage) {
        
       /* JSONObject status = new JSONObject();
        try {
            status.put("status", errorMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
		
        return Response.status(errorCode)
				.entity(status.toString())
				.build(); */
    	return Response.status(errorCode)
                .entity(errorMessage).build();
    }

    public static boolean isDateValid(String date) {

        if (date == null || date.isEmpty())
            return true;

        try {
            DateFormat df = new SimpleDateFormat(TIMESTAMP_FORMAT);
            df.setLenient(false);
            df.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
