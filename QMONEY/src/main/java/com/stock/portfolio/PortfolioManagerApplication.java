package com.stock.portfolio;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stock.dto.AnnualizedReturn;
import com.stock.dto.Candle;
import com.stock.dto.PortfolioTrade;
import com.stock.dto.TiingoCandle;
import com.stock.log.UncaughtExceptionHandler;

public class PortfolioManagerApplication {

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


	private static void printJsonObject(Object object) throws IOException {
		Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
		ObjectMapper mapper = getObjectMapper();
		logger.info(mapper.writeValueAsString(object));
	}

	private static File resolveFileFromResources(String filename) throws URISyntaxException {
		return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();

	}

	private static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper;
	}


	public static List<String> debugOutputs() {

		String valueOfArgument0 = "trades.json";
		String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/prasanthgokulgp-ME_QMONEY_V2/qmoney/bin/main/trades.json";
		String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@2f9f7dcf";
		String functionNameFromTestFileInStackTrace = "mainReadFile";
		String lineNumberFromTestFileInStackTrace = "29";

		return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
				functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
	}

	// Note:
	// Remember to confirm that you are getting same results for annualized returns
	// as in Module 3.
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

	// function to sort hashmap by values
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


	public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token)
			throws JsonParseException, JsonMappingException, IOException {
		String symbol = trade.getSymbol();

		String urlString = "https://api.tiingo.com/tiingo/daily/" + symbol +"/prices?startDate="
				+ trade.getPurchaseDate() + "&endDate=" + endDate + "&token=" + token;

		return urlString;
	}

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

	public static Double getOpeningPriceOnStartDate(List<Candle> candles) {
		Candle candle = candles.get(0);
		return candle.getOpen();
	}


	public static Double getClosingPriceOnEndDate(List<Candle> candles) {
		Candle candle = candles.get(candles.size() - 1);
		return candle.getClose();
	}


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


			// Candle[] candles = om.readValue(url, Candle[].class);

			for(TiingoCandle candle : quotes) {
				result.add(candle);
			}

			return result;
		} catch (Exception exc) {
			throw new RuntimeException();
		} 

	}


	public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
			throws IOException, URISyntaxException {
		// List<PortfolioTrade> trades = readTradesFromJson(args[0]);
		// LocalDate endDate = LocalDate.parse(args[1]);
		String file = args[0];
		final LocalDate endDate = LocalDate.parse(args[1]);
		String contents = readFileAsString(file);
		ObjectMapper objectMapper = getObjectMapper();
		PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);

		// List<AnnualizedReturn> result = new ArrayList<>();
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

	private static String readFileAsString(String filename) throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()),
				"UTF-8");
	}


	public static TiingoCandle[] makeUrlCallForTiingoCandle(String urlString) {
		try {
			URL url = new URL(urlString);
			ObjectMapper om = getObjectMapper();
			TiingoCandle[] quotes = om.readValue(url, TiingoCandle[].class);
			return quotes;
		} catch (Exception exc) {
			throw new RuntimeException();
		}
	}

	public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
			PortfolioTrade trade, Double buyPrice, Double sellPrice) {

		Double totalReturns = (sellPrice - buyPrice) / buyPrice;

		LocalDate currentDate = trade.getPurchaseDate(); 
		double year = currentDate.until(endDate, ChronoUnit.DAYS)/365.24;

		Double annualizedReturn = Math.pow(1+totalReturns, 1.0/year) - 1;
		return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturns);
	}

	// TODO: CRIO_TASK_MODULE_REFACTOR
	//  Once you are done with the implementation inside PortfolioManagerImpl and
	//  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
	//  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
	//  call the newly implemented method in PortfolioManager to calculate the annualized returns.

	// Note:
	// Remember to confirm that you are getting same results for annualized returns as in Module 3.
	public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
			throws Exception {
		String file = args[0];
		LocalDate endDate = LocalDate.parse(args[1]);
		String contents = readFileAsString(file);
		ObjectMapper objectMapper = getObjectMapper();
		RestTemplate restTemplate = new RestTemplate();
		PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
		List<PortfolioTrade> portfolioTrades = readTradesFromJson(file);
		return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
	}


	public static String getToken() {
		String token = "8a57082685909f364648bdc69d4f0ddaf4f2ef00";
		return token;
	}

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
		ThreadContext.put("runId", UUID.randomUUID().toString());

		printJsonObject(mainCalculateReturnsAfterRefactor(args));

	}

}


