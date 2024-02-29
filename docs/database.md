# Using H2

Xeres uses the H2 database. The following is a list of guidelines:

## Data types

### Instant

`TIMESTAMP(9)`

By default, the precision is 6, which is only up to milliseconds instead of nanoseconds. Truncation will occur and make comparison with a fresh instant fail.
So make sure to always specify 9 as precision.

