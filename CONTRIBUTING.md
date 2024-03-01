Frontend of the Marriage Allowance application
====================================================================

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.


How to contribute
-----------

If you want to contribute any changes to Marriage Allowance Frontend application, then
* Go to the [tamc-front](https://github.com/hmrc/tamc-frontend) repository on github.
* Click the “Fork” button at the top right.
* You’ll now have your own copy of that repository in your github account.
* Open a terminal/shell and clone directory using below command

```shell
git clone git@github.com:username/tamc-frontend.git
```

where 'username' is your github user name

* You’ll now have a local copy of your version of that repository.
* Change to project directory tamc-frontend and start with changes.

Post code changes check
-----------

Once you are done with the changes make sure that:
* all test cases run successfully. Use below command to run the testcases

```shell
sbt test
```

* all your changes are covered by unit test cases. If not, please write more testcases.
* code coverage does not go below alread existing code coverage. Use below commad to run coverage report

```shell
sbt clean coverage test coverageReport
```

* you have taken latest code from master (rebase the code) before you raise a Pull Request
* you have to check that there are no merege conflicts in your Pull Request.
* you have provided relevant comments while committing changes and while raising Rull Request.

What happens next
------------

Once you have raised pull request for the changes, tamc-frontend owner team will receive an email. The team will review these changes and will advice you further. They will:
* check for unit test code coverage for the changes.
* check the overall test coverage for the whole project.
* review the changes and may ask you for further enhancements.
* merge your changes and you will receive a mail for the same.
