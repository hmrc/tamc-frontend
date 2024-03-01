Frontend of the Marriage Allowance application
==============================================

Summary
-------
This service allow a customer to apply for [Marriage Allowance], and to modify an existing Marriage Allowance application.

This service is also known as TAMC - Transfer of Allowance for Married Couples, and should not be confused with [Married Couple's Allowance]

This service provides the frontend endpoint for the [TAMC API](https://github.com/hmrc/tamc).

Tax rates
---------
The service should now dynamically pick the tax year from the current system date. 
The tax year rates must be updated every tax year before 6 April in [tax rates configuration](conf/data/tax-rates.conf) 
file to pick the new tax rates. If the rates are not updated it will take consider zero (0) for all the fields from tax-rates files.


Requirements
------------
This service is written in [Scala] and [Play], and needs at least a [JRE] to run.


Running the application
-----------------------
```shell
sbt -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes run
```


Testing the application
-----------------------
```shell
sbt test
```


Acronyms
--------
In the context of this application we use the following acronyms and define their
meanings. Provided you will also find a web link to discover more about the systems
and technology.

* [API]: Application Programming Interface
* [JRE]: Java Runtime Environment
* [JSON]: JavaScript Object Notation
* [NINO]: [National Insurance] Number
* [URL]: Uniform Resource Locator

License
-------
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

[API]: https://en.wikipedia.org/wiki/Application_programming_interface
[JRE]: https://www.oracle.com/technetwork/java/javase/overview/index.html
[JSON]: https://json.org/
[National Insurance]: https://www.gov.uk/national-insurance/overview
[NINO]: https://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm
[Play]: https://https://www.playframework.com/
[Scala]: https://www.scala-lang.org/
[URL]: https://en.wikipedia.org/wiki/Uniform_Resource_Locator
[Marriage Allowance]: https://www.gov.uk/marriage-allowance
[Married Couple's Allowance]: https://www.gov.uk/married-couples-allowance
