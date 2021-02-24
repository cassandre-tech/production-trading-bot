package production.tech.cassandre.trading.bot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import production.tech.cassandre.trading.bot.Reporting;
import production.tech.cassandre.trading.bot.SMAStrategy;
import tech.cassandre.trading.bot.dto.util.CurrencyDTO;
import tech.cassandre.trading.bot.dto.util.GainDTO;
import tech.cassandre.trading.bot.service.PositionService;
import tech.cassandre.trading.bot.test.mock.TickerFluxMock;

import java.util.HashMap;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.cassandre.trading.bot.dto.position.PositionStatusDTO.OPENED;

/**
 * Basic Ta4j strategy test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TickerFluxMock.class)
@DisplayName("Simple ta4j strategy test")
public class SMAStrategyTest {

    @Autowired
    private SMAStrategy strategy;

    @Autowired
    private Reporting reporting;

    @Autowired
    private TickerFluxMock tickerFluxMock;

    @Autowired
    private PositionService positionService;

    @Test
    @DisplayName("Check gains")
    public void gainTest() {
		await().forever().until(() -> tickerFluxMock.isFluxDone());

		final HashMap<CurrencyDTO, GainDTO> gains = strategy.getGains();

		System.out.println("Cumulated gains:");
		gains.forEach((currency, gain) -> System.out.println(currency + " : " + gain.getAmount()));

		System.out.println("Position still opened :");
		strategy.getPositions()
				.values()
				.stream()
				.filter(p -> p.getStatus().equals(OPENED))
				.forEach(p -> System.out.println(" - " + p));

		assertTrue(gains.get(strategy.getRequestedCurrencyPair().getQuoteCurrency()).getPercentage() > 0);
    }

}
