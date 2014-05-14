
setwd("/Users/user/Dropbox/PhD_ICSS/Research/ABM")
options(scipen=999)

library(quantmod)

# dev.new(width=10,height=5)
# par(mfrow=c(1,2))

layout(matrix(c(1,2,3,3), 2, 2, byrow = TRUE))

trades <- read.csv("trades.csv",header=T,strip.white=T)
plot(trades$price~trades$time, type="l", main="Trades", xlab="Time", ylab="Price")

# Returns
returns <- Delt(trades$price)
plot(returns[-1]~trades$time[-1], type="l", main="Returns Series", xlab="Time", ylab="Return")

mids <- read.csv("mids.csv",header=T,strip.white=T)
plot(mids$price~mids$time, type="l", main="Mid-Prices", xlab="Time", ylab="Price")

# quotes <- read.csv("quotes.csv",header=T,strip.white=T)
# bids <- quotes[quotes$side=="bid",]
# offers <- quotes[quotes$side=="offer",]
