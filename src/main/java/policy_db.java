// Imports
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import webserver.ConsentYesAPI;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.client.ClientResponseFilter;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class policy_db {
	// Main Function
	public static void main(String[] args) {
		// Variable Initialization and Declaration
		String sqlurl = "jdbc:mysql://tippersweb.ics.uci.edu:3306/policy_db_test?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
		
		// Connection to policy database in sql
		Connection polyconn = null;
		PreparedStatement st = null;
		String wifid = ""; // To obtain wifi device id from sql database
		ResultSet rs = null; // To save all query results into
		DateTime startstamp = new DateTime(2018, 07, 25, 10, 00, 00); // Start time stamp for policy
		DateTime endstamp = new DateTime(2018, 07, 25, 15, 00, 00); // End time stamp for policy
		String author = "admin";
		LocalDateTime now; // current localdatetime
		int threshhold = 50; // Threshhold to check against wifi connections for single wifi device
		int threshhold2 = 200; // Threshhold to check against wifi connections for multiple wifi devices
		String[] wifidevices = {"3142-clwa-2059", "3142-clwa-2051", "3142-clwa-2099", "3142-clwa-2065"}; // List of wifi device ids associated with this policy
		int policyid = 0; // policy id to input in sql database
		int jj = 0; // Counter to obtain wifidevices from query result into a String array
		String[] wifidevs = new String[4]; // List of related wifidevice ids
		HashMap<String, String[]> wificam = new HashMap<String, String[]>(); // HashMap of region overlap between wifi and cams
		wificam.put("3142-clwa-2099", new String[] {"128.200.218.17", "128.200.218.18"}); // wifidevice in lab room 2099
		wificam.put("3142-clwa-2065", new String[] {"128.200.218.16", "128.195.53.226", "128.195.53.176", "128.195.53.232"}); // wifidevice in meeting room 2065
		wificam.put("3142-clwa-2059", new String[] {"128.200.218.16", "128.200.218.14"}); // wifidevice in lab room 2059
		wificam.put("3142-clwa-2051", new String[] {"128.200.218.16", "128.200.218.14"}); // wifidevice in room 2051
		String operator = ">"; // For the condition table in mysql database

		// Main Body of Code 

		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			// get connection to database
			polyconn = DriverManager.getConnection(sqlurl, "test", "test");
			// First query to input into policy table
			String query = "INSERT INTO policy (author, startdatetime, enddatetime) VALUES (\"" + author + "\", \"" + startstamp.toString("yyyy-MM-dd HH:mm:ss") + "\", \"" + endstamp.toString("yyyy-MM-dd HH:mm:ss") +"\");";
			//String query1 = "UPDATE policy SET startdatetime = \"" + startstamp.toString("yyyy-MM-dd HH:mm:ss") + "\", enddatetime = \"" + endstamp.toString("yyyy-MM-dd HH:mm:ss") + "\", threshhold = " + threshhold2 + " WHERE policyid = " + Integer.toString(policyid) + ";";
			String query12, query13;

			st = polyconn.prepareStatement(query);
			st.executeUpdate();
			
			String query6 = "SELECT policyid FROM policy WHERE author = \"" + author + "\" AND startdatetime = \"" + startstamp.toString("yyyy-MM-dd HH:mm:ss") + "\" AND enddatetime = \"" + endstamp.toString("yyyy-MM-dd HH:mm:ss") + "\";";
			PreparedStatement ps7 = polyconn.prepareStatement(query6);
			rs = ps7.executeQuery();
			
			while (rs.next()) {
				policyid = rs.getInt(1);
				break;
			}
			
			// Queries to input into devices and devices_policy tables for database
			for (String wifi: wifidevices) {
				query12 = "INSERT IGNORE INTO devices VALUES (\"" + wifi + "\");";
				st = polyconn.prepareStatement(query12);
				st.executeUpdate(); // Must be done to see fruits of query made
				query13 = "INSERT IGNORE INTO devices_policy (policy_id, device_id) VALUES (" + Integer.toString(policyid) + ", \"" + wifi + "\");";	    
			    st = polyconn.prepareStatement(query13); 
			    st.executeUpdate();
			} 
			
			// Query to insert the condition values in the conditionpol table
			query = "INSERT IGNORE INTO conditionpol (policyid, operator, threshhold) VALUES (" + policyid + ", \"" + operator + "\", " + threshhold +");";
			st = polyconn.prepareStatement(query);
			st.executeUpdate();
			
			// Query to obtain all related devices for particular policy with id policyid
			String query2 = "SELECT DISTINCT (device_id) device_id FROM devices_policy WHERE policy_id = " + Integer.toString(policyid) + ";";
			PreparedStatement ps2 = polyconn.prepareStatement(query2);
			rs = ps2.executeQuery();

			// Query to obtain start timestamp
			String query3 = "SELECT startdatetime FROM policy WHERE policyid = " + Integer.toString(policyid) + ";";
			PreparedStatement ps3 = polyconn.prepareStatement(query3);
			ResultSet rs2 = ps3.executeQuery();

			// Query to obtain start timestamp
			String query4 = "SELECT enddatetime FROM policy WHERE policyid = " + Integer.toString(policyid) + ";";
			PreparedStatement ps4 = polyconn.prepareStatement(query4);
			ResultSet rs3 = ps4.executeQuery();

			// Store all wifidevices into wifidevs String array
			while (rs.next()) {
				wifidevs[jj] = rs.getString(1);
				jj++;
			}

			// Store start timestamp and end timestamp
			while (rs2.next()) {
				startstamp = DateTime.parse(rs2.getString(1), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
			}
			while (rs3.next()) {
				endstamp = DateTime.parse(rs3.getString(1), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
			}
			
			wifid = wifidevs[0];
			
			// Query to obtain the operator and threshhold values from the condition table
			query = "SELECT operator, threshhold FROM conditionpol WHERE policyid = " + policyid + ";";
			st = polyconn.prepareStatement(query);
			rs = st.executeQuery();
			
			while (rs.next()) {
				operator = rs.getString(1);
				threshhold = rs.getInt(2);
			}
			
			initiateServer("akrishnan@scu.edu", author, startstamp, endstamp);
			
			// Check connections to wifid against threshhold and print wifid checked
		//	System.out.println(fetchObsData(wifid, startstamp, endstamp, threshhold, author, operator));
		//	System.out.println(wifid);

		//	DateTime startnew = singleconn(wifid, startstamp, endstamp, threshhold, wificam, author, operator);
		//	HashMap<Integer, Integer[]> rellogs = findlogs(startnew, endstamp, wificam.get(wifid));
		// 	seallogs(rellogs);
		 
			// Check connections to list of wifi devices against threshholdand print array after 
		//	System.out.println(fetchObsDatas(wifidevs, startstamp, endstamp, threshhold2, author, operator));
		//	System.out.println(Arrays.toString(wifidevs));
			
		//  startnew = multiconn(wifidevs, startstamp, endstamp, threshhold, wificam, author, operator);
		//	for (String wifid : wifidevs) {
	    //      HashMap<Integer, Integer[]> rellogs = findlogs(startnew, endstamp, wificam.get(wifid));
		//		seallogs(rellogs);
		//	}
			
			//sendEmail("dyrenkova.emiliia@gmail.com", "Policy Update", "SURVEILLANCE POLICY ACTIVE: \n \t We would like to inform you of a policy update in Donald Bren Hall at UC Irvine where cameras " + "" + "are on. \n \t Don't worry and Stay Happy!!!!!");

		//	sendSMS("+19495379867", "+15853120188", "Hello. This is TIPPERS!!");
	 
		} catch (SQLException e) {
			System.out.println("Error! ERROR! Error!\n");
			e.printStackTrace();
		} catch(Exception e2){
			e2.printStackTrace();
		}
	}
	
	// Check for single wificonnection
	public static DateTime singleconn(String wifid, DateTime startstamp, DateTime endstamp, int threshhold, HashMap<String, String[]> wificam, String author, String operator) {
		// If connections to particular wifid is related to the threshhold based on an operator start camera feed
		LocalDateTime now = new LocalDateTime(); // system time for knowing how long to run camera feed
		LocalDateTime startnew = new LocalDateTime(); // time to determine which logs are relevant later
		
		// Do not check connections until system time reaches start timestamp for policy
		while (now.toDateTime().isBefore(startstamp)) {
			now = new LocalDateTime();
		}
		
		// Check if connections follow an operator condition to threshhold. If so, start camera feed
		if (fetchObsData(wifid, startstamp, endstamp, threshhold, author, operator)) {
			for (String s: wificam.get(wifid)) {
				startrec(s, "brenhall314", "admin", "0"); // Needs to record at what time condition is met so for duration of time left within interval recording proceeds
			}
			// Get current time to check against end timestamp
			now = new LocalDateTime();
			startnew = now;
			while ((now.toDateTime()).isBefore(endstamp)) {
				now = new LocalDateTime();
				sendwifi(wifid, now.toDateTime(), endstamp, startstamp, author); // check to send emails to individuals who enter after camera has begun recording
			}
			// Stop camera recording once time has reached end boundary
			for (String s: wificam.get(wifid)) {
				stoprec(s, "brenhall314", "admin", "0");
			}
		}
		return startnew.toDateTime();
	}
	
	// Find all the relevant logs
	public static HashMap<Integer, Integer[]> findlogs(DateTime startstamp, DateTime endstamp, String[] cams) {
		HashMap<Integer, Integer[]> arr = new HashMap<Integer, Integer[]>();
		ArrayList<Integer> contracts = new ArrayList<Integer>();
		String sqlurl = "jdbc:mysql://tippersweb.ics.uci.edu:3306/policy_db_test?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
		Connection logcon = null;
		PreparedStatement ps,ps2 = null;
		ResultSet rs,rs2 = null;
		
		try {
			logcon = DriverManager.getConnection(sqlurl, "test", "test");
			
			LocalDateTime current = new LocalDateTime();
			while (current.toDateTime().isBefore(endstamp) || current.toDateTime().isEqual(endstamp)) {
				for (String cam : cams) {
					String query = "SELECT * FROM logging WHERE startstamp >= \"" + startstamp + "\" AND endstamp <= \"" + endstamp + "\" AND deviceid == \"" + cam + "\";";
					ps = logcon.prepareStatement(query);
					rs = ps.executeQuery();
					
					while (rs.next()) {
						String query2 = "SELECT policy_id FROM devices_policy WHERE device_id = \"" + rs.getString(4) + "\" ORDER BY policy_id ASC;";
						ps2 = logcon.prepareStatement(query2);
						rs2 = ps2.executeQuery();
						
						while (rs2.next()) {
							contracts.add(rs2.getInt(1));
						}
						
						arr.put(rs.getInt(1), contracts.toArray(new Integer[contracts.size()]));
					}
				}
				current = new LocalDateTime();
			}
			
			return arr;
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return arr;
	}

	// Check for multiple connections
	public static DateTime multiconn(String[] wifidevs, DateTime startstamp, DateTime endstamp, int threshhold, HashMap<String, String[]> wificam, String author, String operator) {
		// If connections to all wifi devices in wifidevs array is greather than threshhold start camera feed
		LocalDateTime now = new LocalDateTime();
		LocalDateTime startnew = new LocalDateTime();
		
		// Only start connection checking when current system time reaches policy's start timestamp
		while (now.toDateTime().isBefore(startstamp)) {
			now = new LocalDateTime();
		}
		
		// Check if connections to wifi devices are relevant to connections based on user-defined operator and threshhold
		if (fetchObsDatas(wifidevs, startstamp, endstamp, threshhold, author, operator)) {
			for (String wifidev : wifidevs) { // Turn on all cameras associated with specified wifidevs
				for (String s: wificam.get(wifidev)) {
					startrec(s, "brenhall314", "admin", "0"); // Needs to record at what time condition is met so for duration of time left within interval recording proceeds
				}
			}
			now = new LocalDateTime();
			startnew = now;
			
			while ((now.toDateTime()).isBefore(endstamp)) {
				now = new LocalDateTime();
				sendwifis(wifidevs, now.toDateTime(), endstamp, startstamp, author); // Check if new users appear in areas
			}
			for (String wifidev : wifidevs) {
				for (String s: wificam.get(wifidev)) {
					stoprec(s, "brenhall314", "admin", "0"); // Needs to stop recording for cameras
				}
			}
		}
		return startnew.toDateTime();
	}
	
	private static void sendwifis(String[] wifidevs, DateTime dateTime,
			DateTime end, DateTime start, String author) {
		// TODO Auto-generated method stub
		try {
			Client client = Client.create();
			String serverOutput = "";
			Set<Integer> ownarr = new HashSet<Integer>();
			
			while (start.isBefore(end) || start.isEqual(end)) {
				DateTime startstep = start.plusMinutes(5);
				for (String sensor_id : wifidevs) {
					String url = "http://sensoria.ics.uci.edu:8059" + "/observation/get"+ "?start_timestamp=" + (start.plusSeconds(2)).toString("yyyy-MM-dd HH:mm:ss") + "&end_timestamp=" + startstep.toString("yyyy-MM-dd HH:mm:ss") + "&sensor_id="+ sensor_id;
					WebResource webResource = client.resource(url.replaceAll(" ", "%20"));
					ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
					JSONArray obsList = null;
					serverOutput = response.getEntity(String.class);
					obsList = new JSONArray(serverOutput);

					if (obsList.length() > 0) {
						for(int i = 0; i < obsList.length(); i++){
							JSONObject obsObject = (JSONObject) obsList.get(i);
							JSONObject payloadObj = obsObject.getJSONObject("payload");
							String clientid = payloadObj.getString("client_id");
							int owners = fetchSensorData(clientid, start, end);
							ownarr.add(owners);
						}
					}
				}
				start = startstep;
			}
			
			/*for (int own : ownarr) {
				String email = fetchUserData(own);
				if (!email.isEmpty()) {
					initiateServer(email, author, starty, end);
				}
			}*/
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	// Starts recording camera feed by passing instruction into video log server
	public static void startrec(String cameraip, String camerapass, String user, String reqid) {
		String url = "http://tippersweb.ics.uci.edu:6969/camerawrapper/start";
		System.out.println(url);
		String[] ips = cameraip.split(":"); // Split camera ip to obtain last two digits for sensor id specs
		String sensorid = "";
		if (ips[3].length() < 3) {
			sensorid = "Camera" + ips[3]; // Sensors for corridor cameras will be like Camera10 
		} else {
			if (ips[3].equals("226")) {
				sensorid = "MeetingRoomVideoCamera1"; // Special for meeting room 2065 video cameras 
			} else if (ips[3].equals("176")) {
				sensorid = "MeetingRoomVideoCamera2"; // Special for meeting room 2065 video cameras
			} else {
				sensorid = "MeetingRoomVideoCamera3"; // Special for meeting room 2065 video cameras
			}
		}

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Content-Type", "application/json");

			String postJsonData1 = "[{\"CameraIP\": \"http://" + cameraip + "\", \"CameraPass\" : \"" + camerapass + "\","+
					"\"CameraUser\" : \"" + user + "\" ,  \"sensor_id\" : \"" + sensorid + "\", \"request_id\": \""+ reqid + "\"}]";

			/*String postJsonData1 = "[{\"CameraIP\": \"" + sensor.getSensorIP() + "\", \"CameraPass\" : " +
			                "\"brenhall314\", \"CameraUser\" : \"admin\" ,  \"sensor_id\" : \"" + sensor.getId() +
			                "\", \"request_id\": \"" + req_id + "\"}]";*/

			System.out.println("postJsonData=" + postJsonData1);

			con.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(postJsonData1);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			System.out.println("Sending POST request to URL : " + url);
			System.out.println("Post Data : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

			String output;
			StringBuffer response = new StringBuffer();

			while ((output = in.readLine()) != null) {
				response.append(output);
			}

			in.close();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Stops recording camera feed by passing instruction into video log server
	public static void stoprec(String cameraip, String campass, String user, String reqid) {
		String[] ips = cameraip.split(":");
		String sensorid = "";
		if (ips[3].length() < 3) {
			sensorid = "Camera" + ips[3]; // Sensors for corridor cameras will be like Camera10 
		} else {
			if (ips[3].equals("226")) {
				sensorid = "MeetingRoomVideoCamera1"; // Special for meeting room 2065 video cameras 
			} else if (ips[3].equals("176")) {
				sensorid = "MeetingRoomVideoCamera2"; // Special for meeting room 2065 video cameras
			} else {
				sensorid = "MeetingRoomVideoCamera3"; // Special for meeting room 2065 video cameras
			}
		}

		try {
			String url = "http://tippersweb.ics.uci.edu:6969/camerawrapper/stop";
			System.out.println(url);

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Content-Type", "application/json");
			String postJsonData1 = "[{\"CameraIP\": \"http://" + cameraip + "\", \"CameraPass\" : \"" + campass + "\","+
					"\"CameraUser\" : \"" + user + "\" ,  \"sensor_id\" : \"" + sensorid + "\", \"request_id\": \""+ reqid + "\"}]";

			System.out.println("postJsonData=" + postJsonData1);

			con.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(postJsonData1);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			System.out.println("Sending POST request to URL : " + url);
			System.out.println("Post Data : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

			String output;
			StringBuffer response = new StringBuffer();

			while ((output = in.readLine()) != null) {
				response.append(output);
			}

			in.close();
			System.out.println("Supposed to call stop recording for " + sensorid);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Obtain wifi connections against threshhold for particular wifi device
	// find the number of connections and test against threshhold value for particular device
	public static boolean fetchObsData(String sensor_id, DateTime start, DateTime end, int threshhold, String author, String operator) {
		try {
			Client client = Client.create();
			int connect = 0;
			String serverOutput = "";
			Set<Integer> ownarr = new HashSet<Integer>();
			DateTime starty = start;
			
			while (start.isBefore(end) || start.isEqual(end)) {
				DateTime startstep = start.plusMinutes(5);
				String url = "http://sensoria.ics.uci.edu:8059" + "/observation/get"+ "?start_timestamp=" + (start.plusSeconds(2)).toString("yyyy-MM-dd HH:mm:ss") + "&end_timestamp=" + startstep.toString("yyyy-MM-dd HH:mm:ss") + "&sensor_id="+ sensor_id;
				WebResource webResource = client.resource(url.replaceAll(" ", "%20"));
				ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
				JSONArray obsList = null;
				serverOutput = response.getEntity(String.class);
				obsList = new JSONArray(serverOutput);

				if (obsList.length() > 0) {
					for(int i = 0; i < obsList.length(); i++){
						JSONObject obsObject = (JSONObject) obsList.get(i);
						JSONObject payloadObj = obsObject.getJSONObject("payload");
						String clientid = payloadObj.getString("client_id");
						int owners = fetchSensorData(clientid, start, end);
						ownarr.add(owners);
						connect += ownarr.size() - connect;
						switch (operator) {
						case ">": if (connect > threshhold) {
							System.out.println(connect);
						/*  for (int own : ownarr) {
								String email = fetchUserData(own);
								if (!email.isEmpty()) {
									initiateServer(email, author, starty, end);
								}
							}    */
							return true;
						}
						break;
						case "<": if (connect < threshhold) {
							System.out.println(connect);
						/*	for (int own : ownarr) {
								String email = fetchUserData(own);
								if (!email.isEmpty()) {
									initiateServer(email, author, starty, end);
								}
							}    */
							return true;
						}
						break;
						case ">=": if (connect >= threshhold) {
							System.out.println(connect);
						/*	for (int own : ownarr) {
								String email = fetchUserData(own);
								if (!email.isEmpty()) {
									initiateServer(email, author, starty, end);
								}
							}    */
							return true;
						}
						break;
						case "<=": if (connect <= threshhold) {
							System.out.println(connect);
						/*	for (int own : ownarr) {
								String email = fetchUserData(own);
								if (!email.isEmpty()) {
									initiateServer(email, author, starty, end);
								}
							}    */
							return true;
						}
						break;
						case "==": if (connect == threshhold) {
							System.out.println(connect);
						/*	for (int own : ownarr) {
								String email = fetchUserData(own);
								if (!email.isEmpty()) {
									initiateServer(email, author, starty, end);
								}
							}    */
							return true;
						}
						break;
						}
						
					}
				}
				start = startstep;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		return false;
	}

	// Fetch sensor data to get hashed mac addresses 
	// Obtain wifi observation data such as connections
	// Get the user's owner number from their hashed MAC address
	public static int fetchSensorData(String sensor_id, DateTime start, DateTime end) {
		try {
			Client client = Client.create();
			String url = "http://sensoria.ics.uci.edu:8059" + "/sensor/get" + "?sensor_id=" + sensor_id + "&type_id=3";
			WebResource webResource = client.resource(url.replaceAll(" ", "%20"));
			ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
			JSONArray obsList = null;
			String serverOutput = response.getEntity(String.class);
			obsList = new JSONArray(serverOutput);

			JSONObject ownobj = (JSONObject) (obsList.get(0));

			return ownobj.getInt("owner");

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		return 0;
	}

	// Obtain email_ids to send policy info
	// Fetch user data for sending emails
	// Get the user's email address from their owner number
	public static String fetchUserData(int ownerid) {
		try {
			Client client = Client.create();
			String url = "http://sensoria.ics.uci.edu:8059" + "/user/get" + "?semantic_entity_id=" + ownerid;
			WebResource webResource = client.resource(url.replaceAll(" ", "%20"));
			ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
			JSONArray userList = null;
			String serverOutput = response.getEntity(String.class);
			userList = new JSONArray(serverOutput);

			JSONObject userobj = (JSONObject) (userList.get(0));
			if(userobj.has("email")) {
				System.out.println(userobj.getString("email"));
				return userobj.getString("email");
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		return "";
	}
	
	// fetch obs for multiple sensors
	public static boolean fetchObsDatas(String[] sensors, DateTime start, DateTime end, int threshhold, String author, String operator) {
		try {
			Client client = Client.create();
			int connect = 0;
			Set<Integer> ownarr = new HashSet<Integer>();
			DateTime starty = start;

			while (start.isBefore(end) || start.isEqual(end)) {
				DateTime startstep = start.plusMinutes(5);
				for (String sensor_id : sensors) {
					String url = "http://sensoria.ics.uci.edu:8059" + "/observation/get"+ "?start_timestamp=" + start.toString("yyyy-MM-dd HH:mm:ss") + "&end_timestamp=" + startstep.toString("yyyy-MM-dd HH:mm:ss") + "&sensor_id="+ sensor_id;
					WebResource webResource = client.resource(url.replaceAll(" ", "%20"));
					ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
					JSONArray obsList = null;
					String serverOutput = response.getEntity(String.class);
					obsList = new JSONArray(serverOutput);

					if (obsList.length() > 0) {
						for(int i = 0; i < obsList.length(); i++){
							JSONObject obsObject = (JSONObject) obsList.get(i);
							JSONObject payloadObj = obsObject.getJSONObject("payload");
							String clientid = payloadObj.getString("client_id");
							int owners = fetchSensorData(clientid, start, end);
							ownarr.add(owners);
							connect += ownarr.size() - connect;
							switch (operator) {
							case ">": if (connect > threshhold) {
								System.out.println(connect);
							/*  for (int own : ownarr) {
									String email = fetchUserData(own);
									if (!email.isEmpty()) {
										initiateServer(email, author, starty, end);
									}
								}    */
								return true;
							}
							break;
							case "<": if (connect < threshhold) {
								System.out.println(connect);
							/*	for (int own : ownarr) {
									String email = fetchUserData(own);
									if (!email.isEmpty()) {
										initiateServer(email, author, starty, end);
									}
								}    */
								return true;
							}
							break;
							case ">=": if (connect >= threshhold) {
								System.out.println(connect);
							/*	for (int own : ownarr) {
									String email = fetchUserData(own);
									if (!email.isEmpty()) {
										initiateServer(email, author, starty, end);
									}
								}    */
								return true;
							}
							break;
							case "<=": if (connect <= threshhold) {
								System.out.println(connect);
							/*	for (int own : ownarr) {
									String email = fetchUserData(own);
									if (!email.isEmpty()) {
										initiateServer(email, author, starty, end);
									}
								}    */
								return true;
							}
							break;
							case "==": if (connect == threshhold) {
								System.out.println(connect);
							/*	for (int own : ownarr) {
									String email = fetchUserData(own);
									if (!email.isEmpty()) {
										initiateServer(email, author, starty, end);
									}
								}    */
								return true;
							}
							break;
							}
						}
					}
				}

				start = startstep;
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		return false;
	}

	// Send email about policy and its conditions
	public static void sendEmail(String emailID, String emailSubject, String emailContent) {
		try {
			Client client = Client.create();
			
			JSONObject emailObj = new JSONObject();
			emailObj.put("subject", emailSubject);
			emailObj.put("message", emailContent);
			emailObj.put("recipient", emailID);
			
			String url = "http://sensoria.ics.uci.edu:8059/message/send";
			WebResource webResource = client.resource(url.replaceAll(" ", "%20"));
			ClientResponse response = webResource.type("application/json").post(ClientResponse.class, emailObj.toString());
			JSONArray userList = null;
			String serverOutput = response.getEntity(String.class);
			
			System.out.println(serverOutput);
			System.out.println("Message Sent Successfully....");

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}
	
	// Send email to users while policy is active for particular wifi device 
	// Send real-time emails while policy is active and condition has been met
	public static void sendwifi(String sensor_id, DateTime start, DateTime end, DateTime starty, String author) {
		try {
			Client client = Client.create();
			String serverOutput = "";
			Set<Integer> ownarr = new HashSet<Integer>();
			
			while (start.isBefore(end) || start.isEqual(end)) {
				DateTime startstep = start.plusMinutes(5);
				String url = "http://sensoria.ics.uci.edu:8059" + "/observation/get"+ "?start_timestamp=" + (start.plusSeconds(2)).toString("yyyy-MM-dd HH:mm:ss") + "&end_timestamp=" + startstep.toString("yyyy-MM-dd HH:mm:ss") + "&sensor_id="+ sensor_id;
				WebResource webResource = client.resource(url.replaceAll(" ", "%20"));
				ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
				JSONArray obsList = null;
				serverOutput = response.getEntity(String.class);
				obsList = new JSONArray(serverOutput);

				if (obsList.length() > 0) {
					for(int i = 0; i < obsList.length(); i++){
						JSONObject obsObject = (JSONObject) obsList.get(i);
						JSONObject payloadObj = obsObject.getJSONObject("payload");
						String clientid = payloadObj.getString("client_id");
						int owners = fetchSensorData(clientid, start, end);
						ownarr.add(owners);
					}
				}
				start = startstep;
			}
			
			/*for (int own : ownarr) {
				String email = fetchUserData(own);
				if (!email.isEmpty()) {
					initiateServer(email, author, starty, end);
				}
			}*/
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	// Send SMS message to user
	public static void sendSMS(String toNum, String fromNum, String messCon) {
		final String ACCOUNT_SID = "AC6b2daf58af4a3f344b14063ff6bed147";
		final String AUTH_TOKEN = "16ee82c8e8cadab32b0ce5f3e9d03cc0";

		Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

		Message message = Message.creator(new PhoneNumber(toNum),
				new PhoneNumber(fromNum), 
				messCon).create();

		System.out.println(message.getSid());
	}
	
	// Check if user is in database and add to database accordingly
	public static boolean checkUser(String emailID, String author, DateTime start, DateTime end) {
		Connection polyconn = null;
		String sqlurl = "jdbc:mysql://tippersweb.ics.uci.edu:3306/policy_db_test?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
		try {
			polyconn = DriverManager.getConnection(sqlurl, "test", "test");
			int policyid = 0;
			String consent = "";
			String querypol = "SELECT policyid FROM policy WHERE author = \"" + author + "\" AND startdatetime = \"" + start.toString("yyyy-MM-dd HH:mm:ss") + "\" AND enddatetime = \"" + end.toString("yyyy-MM-dd HH:mm:ss") + "\";";
			PreparedStatement st = polyconn.prepareStatement(querypol);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				policyid = rs.getInt(1);
				break;
			}
			
			String query = "SELECT * FROM userpolicy WHERE user = \"" + emailID + "\" AND policyid = " + Integer.toString(policyid) + ";";
			st = polyconn.prepareStatement(query);
			rs = st.executeQuery();
			if (!rs.next()) {
				query = "INSERT INTO userpolicy (policyid, user, consent) VALUES (" + Integer.toString(policyid) + ", \"" + emailID + "\", \"" + consent + "\");";
				st = polyconn.prepareStatement(query);
				st.executeUpdate();
				return true;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	// Http Server
	public static final String BASE_URI = "http://0.0.0.0:8002/";
	
	// Start HTTP Server to use with REST APIs
   	public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in edu.uci.ics.tippers.requestscheduler package
        final ResourceConfig rc = new ResourceConfig(/*ConsentYesAPI.class*/).packages("webserver");
        rc.register(CrossDomainFilter.class);
        rc.register(CORSResponseFilter.class);
       	return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }
   	
   	// Stop server stuff
    public static void stopAllComponents() {
        System.out.println("All components stopped...");

    }
    
    // Initiate HTTP server
    public static void initiateServer(String email, String author, DateTime startstamp, DateTime endstamp) {
    	// Initializing Server
    	Server.init();
    	
    	String sqlurl = "jdbc:mysql://tippersweb.ics.uci.edu:3306/policy_db_test?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    	Connection con = null;
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	
    	int policyid = 1;
    	
    	// Starting Server
    	final HttpServer server = startServer();
    	
    	System.out.println(server.getServerConfiguration());
    	System.out.println(server.getListeners());
    	//System.out.println(System.getProperty("java.class.path").toString());
    	System.out.println("Is server started?" + server.isStarted());
    	System.out.println("Server name?" + server.getServerConfiguration().getHttpServerName());

    	System.out.println(String.format("Jersey app started with WADL available at "
    			+ "%s\nHit enter to stop it...", BASE_URI));
    	
		/*String subject = "POLICY UPDATE - TIPPERS";
		String body = "SURVEILLANCE POLICY ACTIVE: \n \t \t We would like to inform you of a policy update "
				+ "in Donald Bren Hall at UC Irvine where cameras near rooms 2065, 2099, and 2061 are on. "
				+ "\n \t If you agree with this policy please click on the following link: \n \t \t "
				+ BASE_URI + "consent/post?response=yes&userid=" + email + "&policyid=" + policyid 
				+ " \n \t If you do not agree please click on the following link: \n \t \t \t "
				+ BASE_URI + "consent/post?response=no&userid=" + email + "&policyid=" + policyid 
				+ "\n \n Please have a nice day!";
		*/
    	
    	try {
    		con = DriverManager.getConnection(sqlurl, "test", "test");
    		
    		String query = "SELECT policyid FROM policy WHERE author = \"" + author + "\" AND startdatetime = \"" + startstamp + "\" AND enddatetime = \"" + endstamp + "\";";
    		ps = con.prepareStatement(query);
    		rs = ps.executeQuery();
    		
    		if (rs.next()) {
    			policyid = rs.getInt(1);
    		}
    		
    		checkUser(email, author, startstamp, endstamp);
    		//if (checkUser(email, "admin", startstamp, endstamp)) {
    		//	sendEmail(email, subject, body);
  
    			do {
    				query = "SELECT consent FROM userpolicy WHERE policyid = " + policyid + " AND user = \"" + email + "\";";
    				ps = con.prepareStatement(query);
    				rs = ps.executeQuery();
    			} while (rs.next() && rs.getString(1).equals(""));
    		//}
    		//server.stop();
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
    // Log Sealing through hash chain
    public static void seallogs(HashMap<Integer, String[]> logdevs) {
    	HashMap<Integer, Integer[]> logcontracts = new HashMap<Integer, Integer[]>();
    	ArrayList<Integer> logs = new ArrayList<Integer>();
    	Set<Integer> contracts = new HashSet<Integer>();
    	Iterator it = logdevs.entrySet().iterator();
    	String sqlurl = "jdbc:mysql://tippersweb.ics.uci.edu:3306/policy_db_test?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    	Connection logon = null;
    	String logentry = "";
    	PreparedStatement pst = null;
    	ResultSet rs = null;
    	
    	// Obtain all distinct contracts as a set
    	while (it.hasNext()) {
    		Map.Entry pair = (Map.Entry) it.next();
    		Integer[] contract = (Integer[]) pair.getValue();
    		for (int cont : contract) {
    			contracts.add(cont);
    		}
    	}
    	
    	// Find all logs that have contract
    	for (int contr : contracts) {
    		it = logdevs.entrySet().iterator();
    		while (it.hasNext()) {
    			Map.Entry pair = (Map.Entry) it.next();
    			for (int cont : (Integer[]) pair.getValue()) {
    				if (cont == contr) {
    					logs.add((Integer) pair.getKey()); 
    				}
    				break;
    			}
    		}
    		logcontracts.put(contr, logs.toArray(new Integer[logs.size()]));
    	}
    	
    	try {
    		logon = DriverManager.getConnection(sqlurl, "test", "test");
    		int Hend = 0;
    		int seof = (ThreadLocalRandom.current().nextInt(1, 842747646 + 1));
    		int sk = (ThreadLocalRandom.current().nextInt(1, 84 + 1)); // secret key for secret key cryptography
    		
    		// Log sealing and database updating
        	it = logcontracts.entrySet().iterator();
        	while (it.hasNext()) {
        		Map.Entry pair = (Map.Entry) it.next();
        		int count = 0;
        		int lastlog = 0;
        		for (int log : (Integer[]) pair.getValue()) {
        			count++;
        			String query = "SELECT * FROM logging WHERE logid = \"" + log + "\";";
        			pst = logon.prepareStatement(query);
        			rs = pst.executeQuery();
        			
        			// Obtain combination of log metadata
        			while(rs.next()) {
        				logentry += rs.getInt(1) + "00";
        				logentry += rs.getString(2) + "00";
        				if (rs.getString(3) != "NULL") {
        					logentry += rs.getString(3) + "00";
        				}
        				logentry  += rs.getString(4) + "00" + rs.getString(5) + "00" + rs.getString(6) + "00";
        				if (rs.getString(7) != "NULL") {
        					logentry += rs.getInt(7) + "00";
        				}
        				if (rs.getString(8) != "NULL") {
        					logentry += rs.getInt(8) + "00";
        				}
        			}
        			
        			// Implement Log Sealing
        			if (count == 1) {
        				Hend = logentry.hashCode();
        				
        				String conHend = "INSERT IGNORE INTO contractHend VALUES (" + pair.getValue() + ", " + Hend + ");";
        				pst = logon.prepareStatement(conHend);
        				pst.executeUpdate();
        				
        				String logHend = "INSERT INTO contractlog (logid, contractid, Hend) VALUES (" + log + ", " + pair.getValue() + ", " + Hend + ");";
        				pst = logon.prepareStatement(logHend);
        				pst.executeUpdate();
        				
        			} else {
        				lastlog = log;
        				Hend = (logentry + Integer.toString(Hend)).hashCode();
        				
        				if (count < ((Integer[]) pair.getValue()).length) {
        					String conHend = "INSERT IGNORE INTO contractHend VALUES (" + pair.getValue() + ", " + Hend + ");";
            				pst = logon.prepareStatement(conHend);
            				pst.executeUpdate();
            				
            				String logHend = "INSERT INTO contractlog (logid, contractid, Hend) VALUES (" + log + ", " + pair.getValue() + ", " + Hend + ");";
            				pst = logon.prepareStatement(logHend);
            				pst.executeUpdate();
        				}
        			}
        		}
        		Hend = sign((Hend ^ seof), sk);
        		
        		String conHend = "INSERT IGNORE INTO contractHend VALUES (" + pair.getValue() + ", " + Hend + ");";
				pst = logon.prepareStatement(conHend);
				pst.executeUpdate();
				
				String logHend = "INSERT INTO contractlog (logid, contractid, Hend) VALUES (" + lastlog + ", " + pair.getValue() + ", " + Hend + ");";
				pst = logon.prepareStatement(logHend);
				pst.executeUpdate();
        	}
        	
    	} catch (SQLException e) {
    		e.printStackTrace();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
    // Signature on Seof
    public static int sign(int a, int sk) {
    	int b = a * (a - sk); // Algorithm for calculating the signature
    	return b;
    }
 	
}



