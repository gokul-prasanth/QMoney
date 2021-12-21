package com.stock;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;
import com.stock.dto.AnnualizedReturn;
import com.stock.dto.Candle;
import com.stock.dto.PortfolioTrade;
import com.stock.dto.TiingoCandle;
import com.stock.log.UncaughtExceptionHandler;
import com.stock.portfolio.PortfolioManager;
import com.stock.portfolio.PortfolioManagerFactory;

public class PortfolioManagerApplication {

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
		ThreadContext.put("runId", UUID.randomUUID().toString());

		printJsonObject(mainCalculateReturnsAfterRefactor(args));
	}

	/**
	 * From the resolved file path, read the contents of the file into a collection. Process the collection and extract the symbols.
	 * 
	 * @param args
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
		File file = resolveFileFromResources(args[0]);
		ObjectMapper om = getObjectMapper();
		PortfolioTrade[] values = om.readValue(file, PortfolioTrade[].class);
		List<String> result = new ArrayList<>();
		for (PortfolioTrade value : values) {
			result.add(value.getSymbol());
		}
		return result;
	}

	/**
	 * Resolve given filename to actual filename and read file.
	 * @param filename
	 * @return
	 * @throws URISyntaxException
	 */
	private static File resolveFileFromResources(String filename) throws URISyntaxException {
		return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
	}

	/**
	 * Static method to initialize ObjectMapper
	 * 
	 * @return ObjectMapper
	 */
	private static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper;
	}

	/**
	 * @param args[0] fileName
	 * @param args[1] startDate of the stock
	 * 
	 * @return list of the Stock names sorted by 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
		List<PortfolioTrade> trades = readTradesFromJson(args[0]);
		String startDate = args[1];
		LocalDate endDate = LocalDate.parse(startDate);

		HashMap<String, String> map = new HashMap<>();
		for (PortfolioTrade trade : trades) {
			map.put(trade.getSymbol(), makeUrlCall(prepareUrl(trade, endDate, getToken())));
		}

		map = sortByValue(map);
		List<String> result = new ArrayList<>();
		result.addAll(map.keySet());
		return result;
	}

	/**
	 * Method to read values from the file
	 * 
	 * @param filename
	 * @return List<PortfolioTrade>
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
		File file =  resolveFileFromResources(filename);
		ObjectMapper om = getObjectMapper();
		PortfolioTrade[] values = om.readValue(file, PortfolioTrade[].class);
		List<PortfolioTrade> result = new ArrayList<>();
		for(PortfolioTrade trade: values) {
			result.add(trade);
		}
		return result;
	}

	/**
	 * Method to sort hashmap by values
	 * 
	 * @param map
	 * @return HashMap<String, String>
	 */
	private static HashMap<String, String> sortByValue(HashMap<String, String> map)
	{
		HashMap<String, String> temp
		= map.entrySet()
		.stream()
		.sorted((i1, i2)
				-> (Double.valueOf(i1.getValue()).compareTo(
						Double.valueOf(i2.getValue()))))
		.collect(Collectors.toMap(
				Map.Entry::getKey,
				Map.Entry::getValue,
				(e1, e2) -> e1, LinkedHashMap::new));

		return temp;
	}

	/**
	 * Method to prepare the url as needed by the Tiingo API.
	 * 
	 * @param trade
	 * @param endDate
	 * @param token
	 * @return url string
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token)
			throws JsonParseException, JsonMappingException, IOException {
		String symbol = trade.getSymbol();

		String urlString = "https://api.tiingo.com/tiingo/daily/" + symbol +"/prices?startDate="
				+ trade.getPurchaseDate() + "&endDate=" + endDate + "&token=" + token;

		return urlString;
	}

	/**
	 * Method to make the url call
	 * @param urlString
	 * @return String
	 */
	public static String makeUrlCall(String urlString) {
		try {
			URL url = new URL(urlString);
			ObjectMapper om = getObjectMapper();
			TiingoCandle[] quotes = om.readValue(url, TiingoCandle[].class);
			for(TiingoCandle quote : quotes) {
				return String.valueOf(quote.getClose());
			} 
		} catch (Exception exc) {
			throw new RuntimeException();
		} 
		return null; 
	}

	/**
	 * Method to get the starting price
	 * @param candles
	 */
	public static Double getOpeningPriceOnStartDate(List<Candle> candles) {
		Candle candle = candles.get(0);
		return candle.getOpen();
	}

	/**
	 * Method to get the closing price
	 * @param candles
	 */
	public static Double getClosingPriceOnEndDate(List<Candle> candles) {
		Candle candle = candles.get(candles.size() - 1);
		return candle.getClose();
	}


	/**
	 * Method to get candles within given range
	 * 
	 * @param trade
	 * @param endDate
	 * @param token
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 */
	public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token)
			throws JsonParseException, JsonMappingException {
		try {
			List<Candle> result = new ArrayList<>();
			result =  makeUrlCallForCandle(prepareUrl(trade, endDate, token));
			return result;
		} catch (Exception exc) {
			throw new RuntimeException();
		} 
	}

	private static List<Candle> makeUrlCallForCandle(String urlString) {
		try {
			List<Candle> result = new ArrayList<>();
			URL url = new URL(urlString);
			ObjectMapper om = getObjectMapper();
			TiingoCandle[] quotes = om.readValue(url, TiingoCandle[].class);

			for(TiingoCandle candle : quotes) {
				result.add(candle);
			}

			return result;
		} catch (Exception exc) {
			throw new RuntimeException();
		} 
	}

	/**
	 * Method to calculate Annualized Return as per the provided logic
	 * 
	 * @param endDate
	 * @param trade
	 * @param buyPrice
	 * @param sellPrice
	 * @return
	 */
	public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
			PortfolioTrade trade, Double buyPrice, Double sellPrice) {
		Double totalReturns = (sellPrice - buyPrice) / buyPrice;

		LocalDate currentDate = trade.getPurchaseDate(); 
		double year = currentDate.until(endDate, ChronoUnit.DAYS)/365.24;

		Double annualizedReturn = Math.pow(1+totalReturns, 1.0/year) - 1;
		return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturns);
	}

	public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
			throws Exception {
		String file = args[0];
		LocalDate endDate = LocalDate.parse(args[1]);
		RestTemplate restTemplate = new RestTemplate();
		PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
		List<PortfolioTrade> portfolioTrades = readTradesFromJson(file);
		return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
	}

	/**
	 * Registered at https://api.tiingo.com/ and got a token to access the stock api.
	 * 
	 * @return token
	 */
	public static String getToken() {
		String token = "8a57082685909f364648bdc69d4f0ddaf4f2ef00";
		return token;
	}

	/**
	 * Prints the JSON Object of file.
	 * 
	 * @param object
	 * @throws IOException
	 */
	private static void printJsonObject(Object object) throws IOException {
		Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
		ObjectMapper mapper = getObjectMapper();
		logger.info(mapper.writeValueAsString(object));
	}
	
	public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
			throws IOException, URISyntaxException {

		String file = args[0];
		final LocalDate endDate = LocalDate.parse(args[1]);
		String contents = readFileAsString(file);
		ObjectMapper objectMapper = getObjectMapper();
		PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);

		String uri = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

		return Arrays.stream(portfolioTrades).map(trade -> {
			String url = uri.replace("$APIKEY", getToken()).replace("$SYMBOL", trade
					.getSymbol())
					.replace("$STARTDATE", trade.getPurchaseDate().toString())
					.replace("$ENDDATE", endDate.toString());
			TiingoCandle[] tiingoCandles = new RestTemplate().getForObject(url, TiingoCandle[].class);
			Double buyPrice = 0.0;
			Double sellPrice = 0.0;
			for (TiingoCandle candle : tiingoCandles) {
				if (candle.getDate().equals(trade.getPurchaseDate())) {
					buyPrice = candle.getOpen();
				}
				if (candle.getDate().equals(endDate)) {
					sellPrice = candle.getClose();
				}
			}
			return calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
		})
				.sorted(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed())
				.collect(Collectors.toList());

	}

	/**
	 * Method to read file as string
	 * 
	 * @param filename
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	private static String readFileAsString(String filename) throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()), "UTF-8");
	}

}


