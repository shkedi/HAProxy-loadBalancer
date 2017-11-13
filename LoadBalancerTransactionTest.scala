package creditGuard

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class LoadBalancerTransactionTest extends Simulation {

  val httpConf = http
    .baseURL("http://localhost:8080")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val headers_10 = Map("Content-Type" -> "application/x-www-form-urlencoded") // Note the headers specific to a given request

  val randomNumber = scala.util.Random
  val mpiId = "11264"
  val terminalNumber = "0962832"

  var succProcess = scenario("success processing")
    .exec(http("payment request (succ)")
      .post("https://Isracardtruma.co.il:9443/xpo/Relay")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .formParam("user", "mpilbtest")
        .formParam("password", "Admin123.")
        .formParam("int_in", _ => getIntInValue())
	.check(xpath("/ashrait/response/doDeal/token/text()").saveAs("token"))
	.check(xpath("/ashrait/response/doDeal/mpiHostedPageUrl/text()").saveAs("paymentPageUrl")))
        .pause(1)
    .exec(http("payment page (ok)")
      .get("${paymentPageUrl}"))
	.pause(10)
    .exec(http("submit (ok) request")
      .post("https://mpi-lb.creditguard.co.il/CGMPI_Server/ProcessCreditCard")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Accept-Language", "en-US,en;q=0.8,he;q=0.6")
        .header("Cache-Control", "max-age=0")
        .header("Connection", "keep-alive")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Host", "mpi-lb.creditguard.co.il")
        .header("Origin", "https://mpi-lb.creditguard.co.il")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36")
        .formParam("txId", "${token}")
        .formParam("lang", "HE")
        .formParam("cardNumber", "4580458045804580")
        .formParam("track2", "")
        .formParam("last4d", "")
        .formParam("transactionCode", "Phone")
        .formParam("listPaymentsInterestValues", "")
        .formParam("listNumberOfPaymentsValues", "0")
        .formParam("Track2CardNo", "4580458045804580")
        .formParam("expYear", "19")
        .formParam("expMonth", "12")
        .formParam("cvv", "123")
        .formParam("personalId", "000000000")
	.check(substring("https://mpi-lb.creditguard.co.il/CGMPI_Server/merchantPages/mpilbtest/OK.jsp?")))

  var failProcess = scenario("fail processing")
    .exec(http("payment request (succ)")
      .post("https://Isracardtruma.co.il:9443/xpo/Relay")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .formParam("user", "mpilbtest")
        .formParam("password", "Admin123.")
        .formParam("int_in", _ => getIntInValue())
        .check(xpath("/ashrait/response/doDeal/token/text()").saveAs("token"))
	.check(xpath("/ashrait/response/doDeal/mpiHostedPageUrl/text()").saveAs("paymentPageUrl")))
        .pause(1)
    .exec(http("payment page (not ok)")
      .get("${paymentPageUrl}"))
	.pause(10)
    .exec(http("submit (notok) request")
      .post("https://mpi-lb.creditguard.co.il/CGMPI_Server/ProcessCreditCard")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Accept-Language", "en-US,en;q=0.8,he;q=0.6")
        .header("Cache-Control", "max-age=0")
        .header("Connection", "keep-alive")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Host", "mpi-lb.creditguard.co.il")
        .header("Origin", "https://mpi-lb.creditguard.co.il")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36")
        .formParam("txId", "${token}")
        .formParam("lang", "HE")
        .formParam("cardNumber", "4580458045804123")
        .formParam("track2", "")
        .formParam("last4d", "")
        .formParam("transactionCode", "Phone")
        .formParam("listPaymentsInterestValues", "")
        .formParam("listNumberOfPaymentsValues", "0")
        .formParam("Track2CardNo", "4580458045804123")
        .formParam("expYear", "19")
        .formParam("expMonth", "12")
        .formParam("cvv", "123")
        .formParam("personalId", "000000000")
	check(substring("https://mpi-lb.creditguard.co.il/CGMPI_Server/merchantPages/mpilbtest/NOTOK.jsp?")))

  setUp(succProcess.inject(rampUsers(1000) over (60 seconds)),
	failProcess.inject(rampUsers(500) over (60 seconds)).
	protocols(httpConf))

 

   def getIntInValue() : String = {
    return "<ashrait><request><version>1000</version><language>HEB</language><dateTime/><requestId>444123</requestId>" +
      s"<mayBeDuplicate></mayBeDuplicate><command>doDeal</command><doDeal><terminalNumber>${terminalNumber}</terminalNumber>" +
      "<mainTerminalNumber></mainTerminalNumber><cardNo>CGMPI</cardNo><total>5000</total><transactionType>Debit</transactionType>" +
      "<creditType>regularCredit</creditType><currency>ILS</currency><transactionCode>Phone</transactionCode><authNumber/>" + 
      "<numberOfPayments></numberOfPayments><firstPayment></firstPayment><periodicalPayment></periodicalPayment>" +
      s"<validation>TxnSetup</validation><dealerNumber></dealerNumber><user>test</user><successUrl></successUrl><mid>${mpiId}</mid>" + 
      s"<uniqueid>yairG${randomNumber.nextInt(91000000)}</uniqueid><mpiValidation>autoComm</mpiValidation><cardId></cardId><cardExpiration></cardExpiration>" +
      "<email></email><clientIP/><customerData><userData1>adi</userData1><userData2>mamb</userData2><userData3/><userData4>" +
      "</userData4><userData5/><userData6/><userData7/><userData8/><userData9/><userData10/></customerData></doDeal></request></ashrait>"
   }
   

}
