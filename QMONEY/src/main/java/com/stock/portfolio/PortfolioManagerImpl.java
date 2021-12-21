package com.stock.portfolio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.stock.PortfolioManagerApplication;
import com.stock.dto.AnnualizedReturn;
import com.stock.dto.Candle;
import com.stock.dto.PortfolioTrade;
import com.stock.dto.TiingoCandle;

public class PortfolioManagerImpl implements PortfolioManager {

  RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
        TiingoCandle[] candle = restTemplate.getForObject(buildUri(symbol,from,to), TiingoCandle[].class);
        return Arrays.stream(candle).collect(Collectors.toList());
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

       String urlString = uriTemplate.replace("$APIKEY", getToken()).replace("$SYMBOL", symbol)
            .replace("$STARTDATE", startDate.toString())
            .replace("$ENDDATE", endDate.toString());

      return urlString;
  }

  private static String getToken() {
    String token = "8a57082685909f364648bdc69d4f0ddaf4f2ef00";
    return token;
  }

  @Override
  public List<AnnualizedReturn> 
      calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) {

    List<AnnualizedReturn> list = new ArrayList<>();
    //PortfolioTrade[] portfolioTrades = getObjectMapper().readValue(file, PortfolioTrade[].class);
    try{
    for (PortfolioTrade tradesObj : portfolioTrades) { 
      Double buyPrice = PortfolioManagerApplication.getOpeningPriceOnStartDate(getStockQuote(tradesObj.getSymbol(),tradesObj.getPurchaseDate(), endDate));
      Double sellPrice = PortfolioManagerApplication.getClosingPriceOnEndDate(getStockQuote(tradesObj.getSymbol(),tradesObj.getPurchaseDate(), endDate));
      list.add(PortfolioManagerApplication.calculateAnnualizedReturns(endDate, tradesObj, buyPrice, sellPrice));
    }
  }
  catch(Exception e){
   System.out.println(e);
  }
    list.sort(getComparator());
    return list;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
      
      Double totalReturns = (sellPrice - buyPrice) / buyPrice;

      LocalDate currentDate = trade.getPurchaseDate(); 
      double year = currentDate.until(endDate, ChronoUnit.DAYS)/365.24;

      Double annualizedReturn = Math.pow(1+totalReturns, 1.0/year) - 1;
      return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturns);
  }



}
