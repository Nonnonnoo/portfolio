package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.util.TextUtil;

@SuppressWarnings("nls")
public class BigbankPDFExtractor extends AbstractPDFExtractor
{
    public BigbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BIGBANK AS");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bigbank AS";
    }


    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug",
                        documentContext -> documentContext
                        // @formatter:off
                        // Datum Gegenkonto Buchung Name  Betrag in EUR
                        // @formatter:on
                                        .section("currency") //
                                        .match("^.*Betrag in (?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 21.03.2024 AT123456789101112131 Einzahlung oDkoRVZEb  TxDUxE +1 500,00
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*Einzahlung.*\\+[\\d\\s]+\\,[\\d]{2}$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount")
                        .documentContext("currency")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*Einzahlung.*\\+(?<amount>[\\d\\s]+\\,[\\d]{2})$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(TextUtil.stripBlanks(v.get("amount"))));
                            t.setCurrencyCode(v.get("currency"));
                        })
                        .wrap(TransactionItem::new));

        // @formatter:off
        // 25.03.2024 AT123456789101112131 Auszahlung sUBHAKqzf  vNNKxT -10,00
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*Auszahlung.*\\-[\\d\\s]+,[\\d]{2}.*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*Auszahlung.*\\-(?<amount>[\\d\\s]+,[\\d]{2}).*$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(TextUtil.stripBlanks(v.get("amount"))));
                            t.setCurrencyCode(v.get("currency"));
                        })
                        .wrap(TransactionItem::new));
    }
}
