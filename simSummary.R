# library(optparse)
# 
# option_list <- list( 
# 	make_option("--dir", action="store", type="character", help="The directory of data"),
# 	make_option("--nRuns", action="store", type="integer", help="Number of market runs"),
# 	make_option("--quotesF", action="store", type="character", default="quotes", help="File prefix for quotes data"),
# 	make_option("--midsF", action="store", type="character", default="mids", help="File prefix for mid price data"),
#   make_option("--tradesF", action="store", type="character", default="trades", help="File prefix for trades data")
# )
# args<-parse_args(OptionParser(option_list = option_list))
# 
# setwd(args$dir)
# nRuns <- args$nRuns
# quotesFileName <- args$quotesF
# midsFileName <- args$midsF
# tradesFileName <- args$tradesF

# for manually doing stats:
setwd('/Users/user/Dropbox/PhD_ICSS/Research/ABM/output/')
nRuns <- 5
quotesFileName <- 'quotes'
midsFileName <- 'mids'
tradesFileName <- 'trades'

library(pracma)
library(quantmod)

quoteSigns.ACs = NULL
quoteSigns.Hs = NULL
tradePrice.ACs = NULL
midPrice.ACs = NULL
for (i in 0:(nRuns-1)) {
  #######################
  ##### Quote stuff #####
  #######################
	quotes <- read.csv(paste(quotesFileName, i, ".csv", sep=""), header=T, strip.white=T)
	
	# AC
	quotes.ac <- acf(quotes$side, plot=F)
	firstOrderLagTerm <- quotes.ac$acf[,,1][2]
	quoteSigns.ACs[i+1] <- firstOrderLagTerm
	
	# Hurst Exponent
	quoteSigns.Hs[i+1] <- hurst(quotes$side)
	
  ###########################
  ##### Mid Price stuff #####
  ###########################
  mids <- read.csv(paste(midsFileName, i, ".csv", sep=""), header=T, strip.white=T)
  midReturns <- Delt(mids$price)[-1]
  
  # AC
  midReturns.ac <- acf(midReturns, plot=F)
  firstOrderLagTerm <- midReturns.ac$acf[,,1][2]
  midPrice.ACs[i+1] <- firstOrderLagTerm
  
  #######################
  ##### Trade stuff #####
  #######################
  trades <- read.csv(paste(tradesFileName, i, ".csv", sep=""), header=T, strip.white=T, 
                     colClasses=c("integer","numeric","integer","integer","integer","integer","integer"))
  returns <- Delt(trades$price)[-1]
  
  # AC
  returns.ac <- acf(returns, plot=F)
  firstOrderLagTerm <- returns.ac$acf[,,1][2]
  tradePrice.ACs[i+1] <- firstOrderLagTerm
  
	# free memory
	rm(quotes.ac)
	rm(quotes)
  rm(trades.ac)
  rm(trades)
  rm(midReturns.ac)
  rm(midReturns)
}
summary(quoteSigns.ACs)
summary(quoteSigns.Hs)
summary(midPrice.Hs)
summary(tradePrice.Hs)
