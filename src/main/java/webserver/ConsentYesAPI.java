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
		int policyid = 1;
		status = new JSONObject();
		
		try {
			response = new JSONObject(respJson);
			user = response.getString("email");
			
			status.put("status", "Response recorded correctly!");
			
			checkUser(Integer.toString(user.hashCode()), policyid);
			updateConsent(Integer.toString(resp.hashCode()), Integer.toString(user.hashCode()), policyid);
			return Response.status(200)
	                .entity(status.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
			
			try {
				status.put("status", "Invalid JSON Object");
			} catch (Exception s) {
				s.printStackTrace();
			}
			return Response.status(500)
	                .entity(status.toString()).build();
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
			respcon = DriverManager.getConnection(sqlurl, "test", "test");
			
			String query = "UPDATE userpolicy SET consent = \"" + consent + "\" WHERE user = \"" + userid + "\" AND policyid = " + policyid + ";";
			ps = respcon.prepareStatement(query);
			ps.executeUpdate();
			
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
			
			String query = "SELECT * FROM userpolicy WHERE user = \"" + emailID + "\" AND policyid = " + Integer.toString(policyid) + ";";
			st = polyconn.prepareStatement(query);
			rs = st.executeQuery();
			if (!rs.next()) {
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
		JSONObject status = new JSONObject();
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
			
			String query = "SELECT consent FROM userpolicy WHERE user = \"" + Integer.toString(user.hashCode()) + "\" AND policyid = " + policyid + ";";
			ps = respcon.prepareStatement(query);
			rs = ps.executeQuery();
			
			while (rs.next()) {
				responseString = rs.getString(1);
				break;
			}
			
			if (responseString.equals(Integer.toString(("optin").hashCode()))) {
				status.put("response", "You have opted in");
			} else if (responseString.equals(Integer.toString(("optout").hashCode()))) {
				status.put("response", "You have opted out");
			}
			return Response.status(200)
	                .entity(status.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
			
			try {
				status.put("status", "ERROR: NO RESPONSE RECORDED!");
			} catch (Exception s) {
				s.printStackTrace();
			}
			return Response.status(500)
	                .entity(status.toString()).build();
		}
		
	}

}
