
package com.stock.portfolio;

import java.time.LocalDate;
import java.util.List;

import com.stock.dto.AnnualizedReturn;
import com.stock.dto.PortfolioTrade;

public interface PortfolioManager {

	List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate);

}
