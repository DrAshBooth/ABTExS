library(optparse)

option_list <- list( 
	make_option("--dir", action="store", type="character", help="The directory of data"),
	make_option("--nRuns", action="store", type="integer", help="Number of market runs"),
	make_option("--quotesF", action="store", type="character", default="quotes", help="File prefix for quotes data"),
	make_option("--midsF", action="store", type="character", default="mids", help="File prefix for mid price data")
)
args<-parse_args(OptionParser(option_list = option_list))

setwd(args$dir)
nRuns <- args$nRuns
quotesFileName <- args$quotesF
midsFileName <- args$midsF

library(pracma)

quoteSigns.ACs = NULL
quoteSigns.Hs = NULL
for (i in 0:(nRuns-1)) {
	quotes <- read.csv(paste(quotesFileName, i, ".csv", sep=""), header=T, strip.white=T)
	
	# AC quote signs
	quotes.ac <- acf(quotes$side, plot=F)
	firstOrderLagTerm <- quotes.ac$acf[,,1][2]
	quoteSigns.ACs[i+1] <- firstOrderLagTerm
	
	# Hurst Exponent quote signs
	quoteSigns.Hs[i+1] <- hurst(quotes$side)
	
	# free memory (i think?!?!)
	rm(quotes.ac)
	rm(quotes) 
}
summary(quoteSigns.ACs)
summary(quoteSigns.Hs)