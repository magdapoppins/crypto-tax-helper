# Proceeds Helper for Revolut Crypto Reports

Taxes for cryptocurrency sales are reported for the proceeds of the transaction in a FIFO-basis. From a transaction history, buy and sell events need to be matched so that each sell has a matching buy. If the amounts are do not match, a single transaction will be split into multiple buy-sell pairs.

DISCLAIMER! This is a hobby project. The author(s) are not liable for any issues or faults in results.

## Getting started 
```
mvn test
mvn compile
java -cp target/classes com.magda.taxhelper.TaxHelperApp <data/your-file.csv> <symbol (e.g. "ETH)>
```