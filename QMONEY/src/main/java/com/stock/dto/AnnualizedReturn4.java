package com.stock.dto;

public class AnnualizedReturn4 {

	  private final String symbol;
	  private final Double annualizedReturn;
	  private final Double totalReturns;

	  public AnnualizedReturn4(String symbol, Double annualizedReturn, Double totalReturns) {
	    this.symbol = symbol;
	    this.annualizedReturn = annualizedReturn;
	    this.totalReturns = totalReturns;
	  }

	  public String getSymbol() {
	    return symbol;
	  }

	  public Double getAnnualizedReturn() {
	    return annualizedReturn;
	  }

	  public Double getTotalReturns() {
	    return totalReturns;
	  }
	}
