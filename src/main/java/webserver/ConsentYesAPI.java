package webserver;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.sql.*;

import org.joda.time.DateTime;
import org.json.*;

@Path("consent")
public class ConsentYesAPI {
	
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateResponse(@QueryParam("response") String resp, String respJson) {
		JSONObject response, status;
		String user = "";
		int policyid = 1; // Policyid is only 1 for now; however, theoretically polcies as entered into database will auto-increment
		status = new JSONObject();
		
		try {
			response = new JSONObject(respJson);
			user = response.getString("email"); // Obtain email from JSONObject
			
			status.put("status", "Response recorded correctly!");
			
			checkUser(Integer.toString(user.hashCode()), policyid); // Add user if not already in database
			updateConsent(Integer.toString(resp.hashCode()), Integer.toString(user.hashCode()), policyid); // Update consent in table to include hashed consent from application (done to encrypt consent logs)
			return Response.status(200)
	                .entity(status.toString()).build(); // Return the success response
		} catch (Exception e) {
			e.printStackTrace();
			
			try {
				status.put("status", "Invalid JSON Object");
			} catch (Exception s) {
				s.printStackTrace();
			}
			return Response.status(500)
	                .entity(status.toString()).build(); // Return the failure response
		}
		
    }
	
	private static String sqlurl = "jdbc:mysql://tippersweb.ics.uci.edu:3306/policy_db_test?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
	
	// Update user consent to policy in userpolicy table
	public static void updateConsent(String consent, String userid, int policyid) {
		Connection respcon = null;
		PreparedStatement ps = null;
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			// get connection to database
			respcon = DriverManager.getConnection(sqlurl, "test", "test"); // Make SQL connection to database
			
			String query = "UPDATE userpolicy SET consent = \"" + consent + "\" WHERE user = \"" + userid + "\" AND policyid = " + policyid + ";";
			ps = respcon.prepareStatement(query);
			ps.executeUpdate(); // Update to include the consent as obtained from the application
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Check if user is in database and add to database accordingly
	public static void checkUser(String emailID, int policyid) {
		Connection polyconn = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		String consent = "";
		try {
			polyconn = DriverManager.getConnection(sqlurl, "test", "test");
			
			String query = "SELECT * FROM userpolicy WHERE user = \"" + emailID + "\" AND policyid = " + Integer.toString(policyid) + ";"; // Select all users with corresponding email and policyid
			st = polyconn.prepareStatement(query);
			rs = st.executeQuery();
			if (!rs.next()) { // If user is not in database already, add them with an empty consent string
				query = "INSERT INTO userpolicy (policyid, user, consent) VALUES (" + Integer.toString(policyid) + ", \"" + emailID + "\", \"" + consent + "\");";
				st = polyconn.prepareStatement(query);
				st.executeUpdate();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Path("status")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getOpt(String respJson) {
		JSONObject status = new JSONObject(); // Perform status check to obtain consent of particular user
		JSONObject response = null;
		Connection respcon = null;
		PreparedStatement ps = null;
		int policyid = 1;
		String responseString = "";
		ResultSet rs = null;
		
		try {
			response = new JSONObject(respJson);
			
			String user = response.getString("email");
			
			respcon = DriverManager.getConnection(sqlurl, "test", "test");
			
			String query = "SELECT consent FROM userpolicy WHERE user = \"" + Integer.toString(user.hashCode()) + "\" AND policyid = " + policyid + ";"; // Find consent from the userpolicy table in SQL database that matches email and policyid
			ps = respcon.prepareStatement(query);
			rs = ps.executeQuery(); // Obtain results from query
			
			while (rs.next()) {
				responseString = rs.getString(1);
				break;
			}
			
			if (responseString.equals(Integer.toString(("optin").hashCode()))) { // Check if consent matches hash of optin.
				status.put("response", "You have opted in"); // Set status accordingly
			} else if (responseString.equals(Integer.toString(("optout").hashCode()))) { // else Check if consent matches hash of optout
				status.put("response", "You have opted out"); // Set status accordingly
			}
			return Response.status(200)
	                .entity(status.toString()).build(); // Return status associated with success
		} catch (Exception e) {
			e.printStackTrace();
			
			try {
				status.put("status", "ERROR: NO RESPONSE RECORDED!");
			} catch (Exception s) {
				s.printStackTrace();
			}
			return Response.status(500)
	                .entity(status.toString()).build(); // Return failure status
		}
		
	}

}
