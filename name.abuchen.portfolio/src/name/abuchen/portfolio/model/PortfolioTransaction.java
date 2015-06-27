package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

public class PortfolioTransaction extends Transaction
{
    public enum Type
    {
        BUY, SELL, TRANSFER_IN, TRANSFER_OUT, DELIVERY_INBOUND, DELIVERY_OUTBOUND;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("portfolio." + name()); //$NON-NLS-1$
        }
    }

    private Type type;

    @Deprecated
    /* package */transient long fees;

    @Deprecated
    /* package */transient long taxes;

    public PortfolioTransaction()
    {}

    public PortfolioTransaction(LocalDate date, String currencyCode, long amount, Security security, long shares,
                    Type type,
                    long fees, long taxes)
    {
        super(date, currencyCode, amount, security, shares, null);
        this.type = type;

        if (fees != 0)
            addUnit(new Unit(Unit.Type.FEE, Money.of(currencyCode, fees)));
        if (taxes != 0)
            addUnit(new Unit(Unit.Type.TAX, Money.of(currencyCode, taxes)));
    }

    public PortfolioTransaction(String date, String currencyCode, long amount, Security security, long shares,
                    Type type, long fees, long taxes)
    {
        this(LocalDate.parse(date), currencyCode, amount, security, shares, type, fees, taxes);
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public long getFees()
    {
        return getMonetaryFees().getAmount();
    }

    public Money getMonetaryFees()
    {
        return getUnits().filter(u -> u.getType() == Unit.Type.FEE)
                        .collect(MoneyCollectors.sum(getCurrencyCode(), u -> u.getAmount()));
    }

    @Deprecated
    public void setFees(long fees)
    {
        Optional<Unit> unit = getUnits().filter(u -> u.getType() == Unit.Type.FEE).findAny();
        if (unit.isPresent())
            removeUnit(unit.get());
        addUnit(new Unit(Unit.Type.FEE, Money.of(getCurrencyCode(), fees)));
    }

    public long getTaxes()
    {
        return getMonetaryTaxes().getAmount();
    }

    public Money getMonetaryTaxes()
    {
        return getUnits().filter(u -> u.getType() == Unit.Type.TAX)
                        .collect(MoneyCollectors.sum(getCurrencyCode(), u -> u.getAmount()));
    }

    @Deprecated
    public void setTaxes(long taxes)
    {
        Optional<Unit> unit = getUnits().filter(u -> u.getType() == Unit.Type.TAX).findAny();
        if (unit.isPresent())
            removeUnit(unit.get());
        addUnit(new Unit(Unit.Type.TAX, Money.of(getCurrencyCode(), taxes)));
    }

    public long getLumpSumPrice()
    {
        switch (this.type)
        {
            case BUY:
            case TRANSFER_IN:
            case DELIVERY_INBOUND:
                return getAmount() - getFees() - getTaxes();
            case SELL:
            case TRANSFER_OUT:
            case DELIVERY_OUTBOUND:
                return getAmount() + getFees() + getTaxes();
            default:
                throw new UnsupportedOperationException("Unsupport transaction type: "); //$NON-NLS-1$
        }
    }

    public Money getLumpSum()
    {
        return Money.of(getCurrencyCode(), getLumpSumPrice());
    }

    /**
     * Returns the purchase price before fees
     */
    public long getActualPurchasePrice()
    {
        if (getShares() == 0)
            return 0;

        return getLumpSumPrice() * Values.Share.factor() / getShares();
    }

    public Money getPricePerShare()
    {
        return Money.of(getCurrencyCode(), getActualPurchasePrice());
    }

    @Override
    public boolean isPotentialDuplicate(Transaction other)
    {
        if (!(other instanceof PortfolioTransaction))
            return false;

        if (!super.isPotentialDuplicate(other))
            return false;

        PortfolioTransaction pother = (PortfolioTransaction) other;

        if (type != pother.getType())
            return false;

        return true;
    }
}
