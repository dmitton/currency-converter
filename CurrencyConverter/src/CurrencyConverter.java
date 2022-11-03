import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class CurrencyConverter {
    private Connection conn;
    private int CurrencyID;
    private String CurrencyCode;
    private double ExchangeRate;

    public CurrencyConverter() {}

    public CurrencyConverter(int CurrencyID,String CurrencyCode,double ExchangeRate){
        this.CurrencyID = CurrencyID;
        this.CurrencyCode = CurrencyCode;
        this.ExchangeRate = ExchangeRate;
    }

    //function to open the connection to the database
    public Connection openConnection(){
        try{
            //open the connection to the database
            String dbName = "CurrencyDatabase.db";
            final String CONNECTION_URL = "jdbc:sqlite:" + dbName;
            conn = DriverManager.getConnection(CONNECTION_URL);
            conn.setAutoCommit(false);
        } catch(SQLException e){
            e.printStackTrace();
        }
        return conn;
    }

    //function to close the connection to the database
    public void closeConnection(boolean commit){
        try {
            if(commit){
                conn.commit();
            }
            else{
                conn.rollback();
            }
            conn.close();
            conn = null;
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    //accesses a web api and puts all of the currencies in a list that is returned
    public List<CurrencyConverter> extractInfoFromAPI() throws IOException, SQLException {
        //initialize a new empty list
        List<CurrencyConverter>currencies = new ArrayList<>();
        //get your url that is needed for your web api
        String url_str = "https://v6.exchangerate-api.com/v6/9cca69627201e528ed2f2e4b/latest/USD";
        URL url = new URL(url_str);
        //open a connection using that url and connect to the web api
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.connect();

        //get your information from the web api in a root element and pass it into an object
        JsonParser jp = new JsonParser();
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
        JsonObject jsonobj = root.getAsJsonObject();

        //get another json object that is all of the conversion rates
        JsonObject req_result = jsonobj.get("conversion_rates").getAsJsonObject();
        //create an iterator that iterates through all of the keys of that object
        Iterator<String>keys = req_result.keySet().iterator();
        int currencyID = 1;
        //while it has something to transition too get all of the information and create an object out of it to add to your array
        while(keys.hasNext()){
            String key = keys.next();
            double exchangeRate = req_result.get(key).getAsDouble();
            CurrencyConverter currency = new CurrencyConverter(currencyID,key,exchangeRate);
            currencies.add(currency);
            currencyID += 1;
        }
        //return this array
        return currencies;
    }

    // get the currency information you need through the API
    public CurrencyConverter getCurrencyAPI(String currencyHeld) throws SQLException, IOException {
        //initialize a list of currency objects that is all of the currencies from a web api
        List<CurrencyConverter> currencies = extractInfoFromAPI();
        CurrencyConverter currency1 = null;

        //search the currencies until you find the currency that you need and return it
        for(int i = 0; i < currencies.size();++i){
            if(currencies.get(i).getCurrencyCode().equals(currencyHeld)){
                currency1 = currencies.get(i);
            }
        }
        return currency1;
    }

    //get a currency that is listed in the database table
    public CurrencyConverter getCurrencyDatabase(String currencyNeeded) {
        CurrencyConverter currency = new CurrencyConverter();
        try{
            //open the connection to the database using the connection URL provided as a parameter
            conn = openConnection();

            //set up a query to go through all the rows in the database table
            ResultSet rs;
            String sql = "SELECT * FROM Currency WHERE CurrencyCode = ?;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, currencyNeeded);

            //set the table name with the one given in the parameter of the function
            rs = stmt.executeQuery();

            //while the result set has rows to go through create them into a Currency converter object and put them in a list
            if(rs.next()){
                currency = new CurrencyConverter(rs.getInt("CurrencyID"),rs.getString("CurrencyCode"),rs.getDouble("ExchangeRate"));

            }

            //close the connection to the database
            closeConnection(false);

        } catch (SQLException e){
            e.printStackTrace();
        }
        return currency;
    }

    //function that returns the converted currency
    public double ConvertCurrency(String currencyFrom, String currencyTo, double amount) throws SQLException, IOException {
        //set a variable that will be the result
        double result = 0.0;

        //initialize the two objects needed to calculate the exchange
        CurrencyConverter currency1 = null;
        CurrencyConverter currency2 = null;

        //get a random number between zero and one that will decide whether to access the database or web api
        Random random = new Random();
        int randomizedNumber = random.nextInt(2);

        //if the number is 0 access the database and if it is 1 access the web api
        if(randomizedNumber == 0) {
            //set your variables for your objects that fit the currency you have and the currency you want from a database
            currency1 = getCurrencyDatabase(currencyFrom);
            currency2 = getCurrencyDatabase(currencyTo);
        }
        else{
            //set your variables for your objects that fit the currency you have and the currency you want from a web api
            currency1 = getCurrencyAPI(currencyFrom);
            currency2 = getCurrencyAPI(currencyTo);
        }


        if(currency1 != null && currency2 != null) {
            //get the exchange rates for the current currency and the needed
            double initialExchangeRate = currency1.getExchangeRate();
            double secondExchangeRate = currency2.getExchangeRate();

            //take the amount you have and divide by the rate to get the amount you had in US dollars
            double dollarsFromAmount = amount / initialExchangeRate;
            //take that amount of dollars and multiply by the second rate to get the amount you in have in the second currency
            result = dollarsFromAmount * secondExchangeRate;
            //return the result to have the converted currency
            return result;
        }
        else{
            //return 0.0 if the currencies you are looking for are not found in the database
            return 0.0;
        }
    }

    public String getCurrencyCode() {
        return CurrencyCode;
    }

    public double getExchangeRate() {
        return ExchangeRate;
    }

    public static void main(String args[]) throws IOException, SQLException {


        CurrencyConverter converter = new CurrencyConverter();
        double result = converter.ConvertCurrency("PHP","USD",56.23);

        System.out.print(result);

    }


}
