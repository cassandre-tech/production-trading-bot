package production.tech.cassandre.trading.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import tech.cassandre.trading.bot.dto.position.PositionDTO;
import tech.cassandre.trading.bot.dto.position.PositionRulesDTO;
import tech.cassandre.trading.bot.dto.user.AccountDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyAmountDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyPairDTO;
import tech.cassandre.trading.bot.dto.util.GainDTO;
import tech.cassandre.trading.bot.strategy.BasicTa4jCassandreStrategy;
import tech.cassandre.trading.bot.strategy.CassandreStrategy;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static tech.cassandre.trading.bot.dto.position.PositionStatusDTO.CLOSED;
import static tech.cassandre.trading.bot.dto.position.PositionStatusDTO.OPENED;
import static tech.cassandre.trading.bot.dto.util.CurrencyDTO.BTC;
import static tech.cassandre.trading.bot.dto.util.CurrencyDTO.USDT;

/**
 * SMA strategy.
 */
@CassandreStrategy(
        strategyId = "001",
        strategyName = "SMA strategy")
public final class SMAStrategy extends BasicTa4jCassandreStrategy {

    /** Number of bars. */
    private static final int NUMBER_OF_BARS = 24;

    /** Stop gain percentage. */
    private static final float STOP_GAIN_PERCENTAGE = 4;

    /** Stop loss percentage. */
    private static final float STOP_LOSS_PERCENTAGE = 15;

    /** Currency pair. */
    private static final CurrencyPairDTO POSITION_CURRENCY_PAIR = new CurrencyPairDTO(BTC, USDT);

    /** Position amount. */
    private static final BigDecimal POSITION_AMOUNT = new BigDecimal("0.001");

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /** Reporting. */
    private final Reporting reporting;

    /**
     * Constructor.
     *
     * @param newReporting reporting
     */
    public SMAStrategy(final Reporting newReporting) {
        this.reporting = newReporting;
    }

    @Override
    public CurrencyPairDTO getRequestedCurrencyPair() {
        return POSITION_CURRENCY_PAIR;
    }

    @Override
    public Optional<AccountDTO> getTradeAccount(final Set<AccountDTO> accounts) {
        return accounts.stream()
                .filter(a -> "trade".equalsIgnoreCase(a.getName()))
                .findFirst();
    }

    @Override
    public int getMaximumBarCount() {
        return NUMBER_OF_BARS;
    }

    @Override
    public Duration getDelayBetweenTwoBars() {
        return Duration.ofHours(1);
    }

    @Override
    public Strategy getStrategy() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(getSeries());
        SMAIndicator sma = new SMAIndicator(closePrice, getMaximumBarCount());
        return new BaseStrategy(new OverIndicatorRule(sma, closePrice), new UnderIndicatorRule(sma, closePrice));
    }

    @Override
    public void onPositionStatusUpdate(final PositionDTO position) {
        if (position.getStatus().equals(OPENED) || position.getStatus().equals(CLOSED)) {
            sendReport("Position " + position.getId() + " is now " + position.getStatus().toString().toLowerCase());
        }
    }

    @Override
    public void shouldEnter() {
        // Creating the position.
        if (canBuy(POSITION_AMOUNT)) {
            logger.info("Buying now");
            // Create rules.
            PositionRulesDTO rules = PositionRulesDTO.builder()
                    .stopGainPercentage(STOP_GAIN_PERCENTAGE)
                    .stopLossPercentage(STOP_LOSS_PERCENTAGE)
                    .build();
            // Create position.
            createLongPosition(
                    POSITION_CURRENCY_PAIR,
                    POSITION_AMOUNT,
                    rules);
        } else {
            logger.info("Should by buying but not enough assets");
        }
    }

    @Override
    public void shouldExit() {
    }

    /**
     * Daily report.
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Paris")
    public void dailyReport() {
        sendReport("Your daily report");
    }

    /**
     * Send a report by email.
     *
     * @param messageSubject message subject
     */
    public void sendReport(final String messageSubject) {
        StringBuilder emailBody = new StringBuilder();

        // Gains & fees.
        final HashMap<CurrencyDTO, GainDTO> gains = getGains();
        emailBody.append("Global gains:").append(System.lineSeparator());
        gains.forEach((currency, gain) -> {
            emailBody.append(currency).append(" : ");
            emailBody.append(gain.getPercentage()).append(" %").append(" / ");
            emailBody.append(getFormattedValue(gain.getAmount())).append(" / ");
            emailBody.append(getFormattedValue(gain.getFees()));
            emailBody.append(System.lineSeparator());
        });
        emailBody.append(System.lineSeparator());

        // Opened positions.
        emailBody.append("Opened positions:").append(System.lineSeparator());
        getPositions().values()
                .stream()
                .filter(p -> p.getStatus().equals(OPENED))
                .forEach(p -> emailBody.append(p.getDescription()).append(System.lineSeparator()));
        emailBody.append(System.lineSeparator());

        // Closed positions.
        emailBody.append("Closed positions:").append(System.lineSeparator());
        getPositions().values()
                .stream()
                .filter(p -> p.getStatus().equals(CLOSED))
                .forEach(p -> emailBody.append(p.getDescription()).append(System.lineSeparator()));

        reporting.sendReport(messageSubject, emailBody.toString());
    }

    /**
     * Returns formatted value.
     *
     * @param value value
     * @return formatted value
     */
    private String getFormattedValue(final CurrencyAmountDTO value) {
        return new DecimalFormat("#0.00").format(value.getValue()) + " " + value.getCurrency();
    }

}
