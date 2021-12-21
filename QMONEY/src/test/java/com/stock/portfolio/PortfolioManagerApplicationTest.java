package com.stock.portfolio;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.stock.PortfolioManagerApplication;
import com.stock.dto.AnnualizedReturn;
import com.stock.dto.PortfolioTrade;

class PortfolioManagerApplicationTest {

  @Test
  void mainReadFile() throws Exception {
    //given
    String filename = "trades_new.json";
    List<String> expected = Arrays.asList(new String[]{"AAPL", "MSFT", "GOOGL"});

    //when
    List<String> results = PortfolioManagerApplication
        .mainReadFile(new String[]{filename});

    //then
    Assertions.assertEquals(expected, results);
  }


  @Test
  void mainReadQuotes() throws Exception {
    //given
    String filename = "trades_new.json";
    List<String> expected = Arrays.asList(new String[]{"MSFT", "AAPL", "GOOGL"});

    //when
    List<String> actual = PortfolioManagerApplication
        .mainReadQuotes(new String[]{filename, "2019-12-12"});

    //then
    Assertions.assertEquals(expected, actual);
  }


  @Test
  void mainCalculateAnnualReturn() throws Exception {
    //given
    String filename = "trades_new.json";
    //when
    List<AnnualizedReturn> result = PortfolioManagerApplication
        .mainCalculateSingleReturn(new String[]{filename, "2019-12-12"});

    //then
    List<String> symbols = result.stream().map(AnnualizedReturn::getSymbol)
        .collect(Collectors.toList());
    Assertions.assertEquals(0.814, result.get(0).getAnnualizedReturn(), 0.01);
    Assertions.assertEquals(0.584, result.get(1).getAnnualizedReturn(), 0.01);
    Assertions.assertEquals(0.33, result.get(2).getAnnualizedReturn(),0.01);
    Assertions.assertEquals(Arrays.asList(new String[]{"AAPL", "MSFT", "GOOGL"}), symbols);

  }

  @Test
  public void testCalculateAnnualizedReturn() {
    PortfolioTrade trade = new PortfolioTrade("AAPL", 50, LocalDate.parse("2015-01-01"));
    AnnualizedReturn returns = PortfolioManagerApplication
        .calculateAnnualizedReturns(LocalDate.parse("2018-01-01"),
        trade, 10000.00, 11000.00);
    Assertions.assertEquals(returns.getAnnualizedReturn(), 0.0322, 0.0001);
  }

  @Test
  public void testCalculateAnnualizedReturnGoogl() {
    PortfolioTrade trade = new PortfolioTrade("GOOGL", 50, LocalDate.parse("2019-01-02"));
    AnnualizedReturn returns = PortfolioManagerApplication
        .calculateAnnualizedReturns(LocalDate.parse("2019-12-12"),
        trade, 1054.00, 1348.00);
    Assertions.assertEquals(returns.getAnnualizedReturn(), 0.298, 0.001);
  }

}

