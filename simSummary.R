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
nRuns <- 50
quotesFileName <- 'quotes'
midsFileName <- 'mids'
tradesFileName <- 'trades'

library(pracma)
library(quantmod)
library(ggplot2)
library(e1071)
gcinfo(FALSE)

quoteSigns.ACs = NULL
quoteSigns.Hs = NULL
tradePrice.ACs = NULL
midPrice.ACs = NULL

midVariances = vector('list', 2000) # midVariances[[i]][j] tells us the variance on run j at time interval i
midKurtosis = vector('list', 2000)
for (i in 0:(nRuns-1)) {
  print(paste("run ", i))
  #######################
  ##### Quote stuff #####
  #######################
	quotes <- read.csv(paste(quotesFileName, i, ".csv", sep=""), header=T, strip.white=T,
                     colClasses=c("integer", "factor", "integer", "integer", "numeric", "integer"))
	
	# AC
	quotes.ac <- acf(quotes$side, plot=F)
	firstOrderLagTerm <- quotes.ac$acf[,,1][2]
	quoteSigns.ACs[i+1] <- firstOrderLagTerm
	
	# Hurst Exponent
	quoteSigns.Hs[i+1] <- hurst(quotes$side)
	
  # free memory
  rm(quotes.ac)
  rm(quotes)
  ###########################
  ##### Mid Price stuff #####
  ###########################
  mids <- read.csv(paste(midsFileName, i, ".csv", sep=""), header=T, strip.white=T,
                   colClasses=c("integer","numeric"))
  midReturns <- Delt(mids$price)[-1]
  
  # AC
  midReturns.ac <- acf(midReturns, plot=F)
  firstOrderLagTerm <- midReturns.ac$acf[,,1][2]
  midPrice.ACs[i+1] <- firstOrderLagTerm
  
  # Mid stats against time interval
  for (j in 1:2000){
    # select every j^th mid price
    midSample <- mids$price[seq(1, length(mids$price), j)]
    # calc variance of this sample and add to list
    midVariances[[j]] <-c( midVariances[[j]], var(midSample))
    # calc kurtosis of this sample and add to list
    midKurtosis[[j]] <-c (midKurtosis[[j]], kurtosis(midSample))
  }
  
  # free memory
  rm(mids)
  rm(midReturns.ac)
  rm(midReturns)
  rm(midSample)
  
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
  rm(returns.ac)
  rm(returns)
  rm(trades)
  gc()
}

# Average the midPrice data across runs
midVarByTime <- vector(length=2000)
midKurByTime <- vector(length=2000)
for (k in 1:2000){
  # calc average for time interval i
  midVarByTime[k] <- mean(midVariances[[k]])
  midKurByTime[k] <- mean(midKurtosis[[k]])
}

df <- data.frame(c(1:2000),midVarByTime)
colnames(df) <- c("TimeScale", "Variance")
pdf('varByTime.pdf')
print(ggplot(data=df, aes(x=TimeScale, y=Variance, group=1)) + geom_line() +geom_smooth(method=lm,size=2))
dev.off()

df2 <- data.frame(c(1:2000),midKurByTime)
colnames(df2) <- c("TimeScale", "Kurtosis")
pdf('kurByTime.pdf')
print(ggplot(data=df2, aes(x=TimeScale, y=Kurtosis, group=1)) + geom_line() +geom_smooth(method=lm,size=2))
dev.off()

#sink(file="statsOutput.txt")
summary(quoteSigns.ACs)
summary(quoteSigns.Hs)
summary(midPrice.ACs)
summary(tradePrice.ACs)
#sink()
